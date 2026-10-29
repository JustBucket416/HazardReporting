package com.example.hazardreporting.data.sync

import com.example.hazardreporting.domain.repository.SyncScheduler
import javax.inject.Inject

/**
 * Implementation of [SyncScheduler]. The synchroniser and the unique `sync-once` WorkManager work it enqueues
 * arrive with issue 13; until then scheduling does nothing, and pending changes wait on the device
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SyncSchedulerImpl @Inject constructor() : SyncScheduler {

    override fun schedulePush() = Unit
}
