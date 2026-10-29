package com.example.hazardreporting.data.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The sync `Mutex` (spec § 3.4). Every push holds it, and so does every pull, so a pull can't interleave with
 * a push: it would otherwise merge summaries downloaded before a POST into rows written after it. Logout holds it
 * while it wipes the device, so no push or pull writes into the emptied database
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Singleton
class SyncLock @Inject constructor() {

    private val mutex = Mutex()

    /**
     * Runs [block] while holding the lock, waiting first for whoever holds it now. Not reentrant:
     * [block] must not take the lock again
     */
    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
