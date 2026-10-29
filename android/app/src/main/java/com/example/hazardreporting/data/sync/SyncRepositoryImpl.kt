package com.example.hazardreporting.data.sync

import com.example.hazardreporting.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Implementation of [SyncRepository]
 *
 * @property synchronizer pushes the pending changes
 * @property syncLock held by every push and pull
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SyncRepositoryImpl @Inject constructor(
    private val synchronizer: IncidentSynchronizer,
    private val syncLock: SyncLock,
) : SyncRepository {

    override suspend fun push() {
        synchronizer.push()
    }

    override suspend fun countPendingChanges(): Int = synchronizer.countUnsentChanges()

    override suspend fun <T> withSyncPaused(block: suspend () -> T): T = syncLock.withLock(block)
}
