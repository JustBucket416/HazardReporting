package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.domain.repository.IncidentRepository
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of [IncidentRepository]. Room is the source of truth; the server is only pulled from on request
 *
 * @property remoteDataSource used for calling the server's incident routes
 * @property localDataSource used for storing the incidents
 * @property converter used for converting between DTOs, entities and domain models
 * @property dispatchers used for running on the IO dispatcher
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentRepositoryImpl @Inject constructor(
    private val remoteDataSource: IncidentRemoteDataSource,
    private val localDataSource: IncidentLocalDataSource,
    private val converter: IncidentConverter,
    private val dispatchers: CoroutineDispatchers,
) : IncidentRepository {

    override fun observeSummaries(): Flow<List<IncidentSummary>> = localDataSource.observeIncidents()
        .map { entities -> entities.map { converter.convertSummary(it) } }
        .flowOn(dispatchers.io)

    override suspend fun pullSummaries() {
        withContext(dispatchers.io) {
            val summaries = remoteDataSource.fetchSummaries()
            localDataSource.inTransaction {
                val stored = localDataSource.getIncidents()
                val storedByServerId = stored.filter { it.serverId != null }.associateBy { it.serverId }
                // A report whose POST reached the server but whose response was lost has no server id yet.
                // The server knows it by reporter and createdAt (ADR-0003), and every stored row is the reporter's
                val unidentifiedByCreatedAt = stored.filter { it.serverId == null }.associateBy { it.createdAt }

                val inserted = mutableListOf<IncidentEntity>()
                val updated = mutableListOf<IncidentEntity>()
                for (summary in summaries) {
                    val row = storedByServerId[summary.id] ?: unidentifiedByCreatedAt[summary.createdAt]
                    when {
                        row == null -> inserted += converter.convert(summary)
                        row.syncState == SyncState.Synced -> updated += converter.withSummary(row, summary)
                        // A pending change keeps its report fields until the server accepts it
                        else -> updated += converter.withStatus(row, summary)
                    }
                }
                // The server no longer lists a synced incident it once sent, e.g. one withdrawn elsewhere
                val pulledIds = summaries.map { it.id }.toSet()
                val gone = stored.filter {
                    it.syncState == SyncState.Synced && it.serverId != null && it.serverId !in pulledIds
                }

                localDataSource.insertIncidents(inserted)
                localDataSource.updateIncidents(updated)
                localDataSource.deleteIncidents(gone.map { it.localId })
            }
        }
    }

    override fun observeIncident(localId: Long): Flow<Incident> = localDataSource.observeIncident(localId)
        .filterNotNull()
        .map { converter.convert(it) }
        .flowOn(dispatchers.io)

    override suspend fun loadDetails(localId: Long, force: Boolean) {
        withContext(dispatchers.io) {
            val stored = localDataSource.getIncident(localId) ?: return@withContext
            if (!force && stored.detailsLoaded) return@withContext
            // A report the server doesn't know yet has all its details on the device
            val serverId = stored.serverId ?: return@withContext

            val dto = remoteDataSource.fetchIncident(serverId)
            localDataSource.inTransaction {
                // Read again: the row may have changed while the download was in flight
                val current = localDataSource.getIncident(localId) ?: return@inTransaction
                if (current.syncState == SyncState.Synced) {
                    localDataSource.updateIncident(converter.withDetails(current, dto))
                    localDataSource.replacePhotos(localId, converter.convertPhotos(localId, dto))
                } else {
                    // A pending change keeps its report fields until the server accepts it
                    localDataSource.updateIncident(converter.withStatus(current, dto))
                }
            }
        }
    }
}
