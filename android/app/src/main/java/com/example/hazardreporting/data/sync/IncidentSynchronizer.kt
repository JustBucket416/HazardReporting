package com.example.hazardreporting.data.sync

import com.example.hazardreporting.data.incident.IncidentConverter
import com.example.hazardreporting.data.incident.IncidentLocalDataSource
import com.example.hazardreporting.data.incident.IncidentRemoteDataSource
import com.example.hazardreporting.data.incident.PhotoLocalDataSource
import com.example.hazardreporting.data.incident.dto.CreateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.data.session.SessionHolder
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.example.hazardreporting.utils.Logger
import java.io.File
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Sends the device's pending changes to the server (spec § 3.4, ADR-0002/0003/0004). Every push holds the
 * [SyncLock]. For each incident with a pending change, oldest first:
 * 1. Every photo without a server URL is uploaded; each URL is stored as soon as its upload succeeds, and the
 *    local file is deleted, so a retry never uploads a photo twice.
 * 2. The request for the incident's state is sent: a report sets `createAttempted` and is POSTed; an edit is
 *    PATCHed; a withdrawal that was never POSTed is deleted from the device without a request, otherwise it is
 *    POSTed first if the device has no server id yet, and then PATCHed to `withdrawn`.
 * 3. On success, the row is marked synced only if its `localVersion` is still the one read in step 1; a change
 *    saved meanwhile stays pending and is sent in the same push.
 * 4. On 409, the change is dropped, the incident is downloaded again and overwrites the row, and a
 *    Status Change Notice is raised; a withdrawal refused because the incident is already withdrawn succeeded.
 *
 * A missing connection or a rejected session stops the push: the first throws for a retry, the second also
 * marks the session as rejected. Any other refusal, e.g. a 400 for a field or file, leaves only that incident
 * pending; it is logged, and the other incidents are still sent. Each push starts by deleting old photo files
 * no row refers to
 *
 * @property localDataSource used for reading and updating the stored incidents
 * @property remoteDataSource used for sending reports, edits, withdrawals and photos
 * @property photoLocalDataSource used for reading and deleting photo files
 * @property converter used for converting between entities and DTOs
 * @property syncLock keeps pushes, pulls and logout's wipe apart
 * @property sessionHolder told when the server rejects the session
 * @property clock used for telling old photo files from new ones
 * @property logger used for logging changes the server refuses
 * @property dispatchers used for running on the IO dispatcher
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentSynchronizer @Inject constructor(
    private val localDataSource: IncidentLocalDataSource,
    private val remoteDataSource: IncidentRemoteDataSource,
    private val photoLocalDataSource: PhotoLocalDataSource,
    private val converter: IncidentConverter,
    private val syncLock: SyncLock,
    private val sessionHolder: SessionHolder,
    private val clock: Clock,
    private val logger: Logger,
    private val dispatchers: CoroutineDispatchers,
) {

    /**
     * Sends every pending change, waiting first for a push, pull or wipe in progress. Returns once nothing
     * is left to send, except changes the server refused for a reason other than a 409
     *
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached;
     * the changes not sent yet stay pending for a retry
     * @throws com.example.hazardreporting.domain.exception.UnauthorizedException if the server rejects the session,
     * which is then marked as rejected
     */
    suspend fun push() {
        withContext(dispatchers.io) {
            syncLock.withLock {
                deleteOrphanedPhotos()
                pushPendingIncidents()
            }
        }
    }

    /**
     * Returns how many incidents have a pending change a push would still send; a withdrawal of a report that
     * was never sent needs no request, so it doesn't count
     */
    suspend fun countUnsentChanges(): Int = withContext(dispatchers.io) {
        localDataSource.getPendingIncidents().count { !it.isUnsentWithdrawal }
    }

    private suspend fun deleteOrphanedPhotos() {
        val referencedPaths = localDataSource.getPhotoLocalPaths().toSet()
        val modifiedBefore = clock.millis() - ORPHANED_PHOTO_MIN_AGE.toMillis()
        photoLocalDataSource.deleteUnreferencedPhotos(referencedPaths, modifiedBefore)
    }

    private suspend fun pushPendingIncidents() {
        // Each version of an incident is attempted once per push. A change saved during the push is still sent
        // (scheduling another push meanwhile has no effect), and a refused one isn't retried in a loop
        val attemptedVersions = mutableMapOf<Long, Int>()
        while (true) {
            val next = localDataSource.getPendingIncidents()
                // Withdrawals that need no request go first, so a missing connection can't hold them up
                .sortedBy { if (it.isUnsentWithdrawal) 0 else 1 }
                .firstOrNull { attemptedVersions[it.localId] != it.localVersion }
                ?: break
            attemptedVersions[next.localId] = next.localVersion
            pushIncident(next.localId)
        }
    }

    private suspend fun pushIncident(localId: Long) {
        // Read in one transaction, so the version matches the fields and photos that are sent
        val snapshot = localDataSource.getIncidentWithPhotos(localId) ?: return
        val incident = snapshot.incident
        try {
            when {
                incident.syncState == SyncState.PendingCreate -> create(snapshot)
                incident.syncState == SyncState.PendingUpdate -> update(snapshot)
                incident.isUnsentWithdrawal -> deleteUnsent(snapshot)
                incident.syncState == SyncState.PendingWithdraw -> withdraw(snapshot)
            }
        } catch (e: UnauthorizedException) {
            sessionHolder.markRejected()
            throw e
        } catch (e: HttpException) {
            // Sending it again won't help, so it stays pending without holding up the other incidents
            logger.error(TAG, "The server refused the pending change of incident $localId (HTTP ${e.code()})", e)
        }
    }

    private suspend fun create(snapshot: IncidentWithPhotos) {
        val incident = snapshot.incident
        val photoUrls = uploadPhotos(snapshot) ?: return
        // Set before the POST: from now on the server may know the report, so it can't simply be deleted
        localDataSource.markCreateAttempted(incident.localId)
        val request = converter.convertCreateRequest(incident, photoUrls)
        val dto = remoteDataSource.createIncident(request)
        if (dto.status != STATUS_OPEN && !request.isSameReportAs(dto)) {
            // The server already had the report from an earlier POST whose response was lost, and an operator moved
            // it out of Open since, so it answered with its own version instead of applying this one
            storeServerVersion(incident.localId, dto, hasStatusChangeNotice = true)
        } else {
            storeAccepted(incident, dto)
        }
    }

    private suspend fun update(snapshot: IncidentWithPhotos) {
        val incident = snapshot.incident
        val serverId = incident.serverId ?: run {
            logUnsendable(incident.localId, "an edit without a server id")
            return
        }
        val photoUrls = uploadPhotos(snapshot) ?: return
        val dto = try {
            remoteDataSource.updateIncident(serverId, converter.convertUpdateRequest(incident, photoUrls))
        } catch (e: HttpException) {
            if (e.code() != HTTP_CONFLICT) throw e
            refuse(incident, serverId, isWithdrawal = false)
            return
        }
        storeAccepted(incident, dto)
    }

    private suspend fun withdraw(snapshot: IncidentWithPhotos) {
        val incident = snapshot.incident
        val serverId = incident.serverId ?: run {
            // A POST was sent, but its response never arrived: the report is POSTed again first, so withdrawing it
            // leaves no Open incident behind on the server (ADR-0003)
            val photoUrls = uploadPhotos(snapshot) ?: return
            val created = remoteDataSource.createIncident(converter.convertCreateRequest(incident, photoUrls))
            if (!storeServerState(incident.localId, created)) return
            created.id
        }
        val dto = try {
            remoteDataSource.withdrawIncident(serverId)
        } catch (e: HttpException) {
            if (e.code() != HTTP_CONFLICT) throw e
            refuse(incident, serverId, isWithdrawal = true)
            return
        }
        storeAccepted(incident, dto)
    }

    private suspend fun deleteUnsent(snapshot: IncidentWithPhotos) {
        // No POST was ever sent, so the server can't know the report: it goes without a request (ADR-0003)
        localDataSource.deleteIncidents(listOf(snapshot.incident.localId))
        snapshot.photos.mapNotNull { it.localPath }.forEach { photoLocalDataSource.deletePhoto(it) }
    }

    /**
     * Uploads every photo of [snapshot] that has no server URL yet, in order
     *
     * @return the server URLs of all its photos in order, or null if one of them can never be uploaded
     */
    private suspend fun uploadPhotos(snapshot: IncidentWithPhotos): List<String>? {
        val localId = snapshot.incident.localId
        return snapshot.photos.sortedBy { it.position }.map { photo ->
            photo.remoteUrl ?: uploadPhoto(localId, photo) ?: return null
        }
    }

    private suspend fun uploadPhoto(localId: Long, photo: IncidentPhotoEntity): String? {
        val path = photo.localPath ?: run {
            logUnsendable(localId, "a photo with neither a file nor a URL")
            return null
        }
        val format = photoLocalDataSource.formatOf(path) ?: run {
            logUnsendable(localId, "a photo whose file is missing or not an accepted image")
            return null
        }
        val url = remoteDataSource.uploadPhoto(File(path), format.mimeType)
        localDataSource.setPhotoRemoteUrl(localId, path, url)
        photoLocalDataSource.deletePhoto(path)
        return url
    }

    /**
     * Handles a 409: an operator moved the incident with [serverId] out of Open, so its pending change is dropped
     * and the server's version wins (ADR-0004). A refused withdrawal of an incident that is already withdrawn
     * is the retry of a withdrawal whose response was lost, so it succeeded and raises no notice
     */
    private suspend fun refuse(incident: IncidentEntity, serverId: String, isWithdrawal: Boolean) {
        val dto = remoteDataSource.fetchIncident(serverId)
        val isAlreadyWithdrawn = isWithdrawal && dto.status == STATUS_WITHDRAWN
        storeServerVersion(incident.localId, dto, hasStatusChangeNotice = !isAlreadyWithdrawn)
    }

    /**
     * Stores the server's answer [dto] to the pending change read as [sent]: synced if the row is still at the
     * version that was sent, otherwise only the server's id, status and update time, keeping the newer change pending
     */
    private suspend fun storeAccepted(sent: IncidentEntity, dto: IncidentDto) {
        localDataSource.inTransaction {
            val current = localDataSource.getIncident(sent.localId) ?: return@inTransaction
            if (!claimServerId(current, dto.id)) return@inTransaction
            val updated = if (current.localVersion == sent.localVersion) {
                converter.withAccepted(current, dto)
            } else {
                converter.withServerState(current, dto)
            }
            localDataSource.updateIncident(updated)
        }
    }

    /**
     * Stores the server's id, status and update time from [dto] on the incident with [localId], keeping its
     * pending change
     *
     * @return whether they were stored
     */
    private suspend fun storeServerState(localId: Long, dto: IncidentDto): Boolean = localDataSource.inTransaction {
        val current = localDataSource.getIncident(localId) ?: return@inTransaction false
        if (!claimServerId(current, dto.id)) return@inTransaction false
        localDataSource.updateIncident(converter.withServerState(current, dto))
        true
    }

    /**
     * Overwrites the incident with [localId] and its photos with the server's version [dto], dropping its
     * pending change. Files of photos that were never uploaded stay until they are old enough to be deleted
     * as unreferenced, in case a form that is still open attaches them again
     */
    private suspend fun storeServerVersion(localId: Long, dto: IncidentDto, hasStatusChangeNotice: Boolean) {
        localDataSource.inTransaction {
            val current = localDataSource.getIncident(localId) ?: return@inTransaction
            if (!claimServerId(current, dto.id)) return@inTransaction
            localDataSource.updateIncident(converter.withServerVersion(current, dto, hasStatusChangeNotice))
            localDataSource.replacePhotos(localId, converter.convertPhotos(localId, dto))
        }
    }

    /**
     * Makes sure [incident] can take [serverId] without two rows sharing it. As createdAt is unique on the device
     * and pulls never run during a push, no other row should have it; if a synced copy does anyway, it is the same
     * server incident without a change of its own, so it is deleted. A copy with a pending change is left alone
     *
     * Must run inside a transaction
     *
     * @return whether [incident] may take [serverId]
     */
    private suspend fun claimServerId(incident: IncidentEntity, serverId: String): Boolean {
        val holder = localDataSource.getIncidentByServerId(serverId)
        return when {
            holder == null || holder.localId == incident.localId -> true
            holder.syncState == SyncState.Synced -> {
                localDataSource.deleteIncidents(listOf(holder.localId))
                true
            }
            else -> {
                logUnsendable(incident.localId, "server id $serverId held by incident ${holder.localId}")
                false
            }
        }
    }

    // For a change that can never be sent as it is; it stays pending, so the technician can still edit it
    private fun logUnsendable(localId: Long, reason: String) {
        logger.error(TAG, "Can't send the pending change of incident $localId: $reason")
    }

    private fun CreateIncidentRequestDto.isSameReportAs(dto: IncidentDto): Boolean =
        title == dto.title && description == dto.description && categoryId == dto.categoryId &&
            severityId == dto.severityId && siteId == dto.siteId && locationDescription == dto.locationDescription &&
            position == dto.position && photoUrls == dto.photoUrls

    private val IncidentEntity.isUnsentWithdrawal: Boolean
        get() = syncState == SyncState.PendingWithdraw && !createAttempted

    private companion object {
        const val TAG = "IncidentSynchronizer"
        const val STATUS_OPEN = "open"
        const val STATUS_WITHDRAWN = "withdrawn"
        const val HTTP_CONFLICT = 409

        /**
         * How old an unreferenced photo file must be before it is deleted. Far longer than a form stays open,
         * and than Android keeps a killed form's saved state in practice
         */
        val ORPHANED_PHOTO_MIN_AGE: Duration = Duration.ofDays(7)
    }
}
