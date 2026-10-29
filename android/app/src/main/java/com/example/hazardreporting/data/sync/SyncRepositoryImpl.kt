package com.example.hazardreporting.data.sync

import com.example.hazardreporting.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Implementation of [SyncRepository]
 *
 * @property synchronizer pushes the pending changes
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SyncRepositoryImpl @Inject constructor(
    private val synchronizer: IncidentSynchronizer,
) : SyncRepository {

    override suspend fun push() {
        synchronizer.push()
    }
}
