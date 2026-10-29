package com.example.hazardreporting.domain.repository

/**
 * Sends the device's pending changes to the server (ADR-0002). A change the server refuses because an operator
 * moved the incident out of Open is dropped and raises a Status Change Notice (ADR-0004)
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface SyncRepository {

    /**
     * Sends every pending change now, waiting first for a push already in progress. Returns once nothing is left
     * to send, except changes the server refused outright, which stay pending
     *
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached;
     * the changes not sent yet stay pending
     * @throws com.example.hazardreporting.domain.exception.UnauthorizedException if the server rejects the session,
     * which is then marked as invalid
     */
    suspend fun push()

    /**
     * Returns how many incidents have a pending change a push would still send. A withdrawal of a report that
     * was never sent doesn't count: it needs no request, so nothing is lost if it never happens
     */
    suspend fun countPendingChanges(): Int

    /**
     * Runs [block] while no push or pull can run, waiting first for one in progress to finish.
     * [block] must not push or pull itself
     */
    suspend fun <T> withSyncPaused(block: suspend () -> T): T
}
