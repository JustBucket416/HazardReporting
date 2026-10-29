package com.example.hazardreporting.domain.model

/**
 * Whether an incident has a pending change: a report, edit or withdrawal the server hasn't accepted yet
 *
 * @author Roman Pliskin on 29.10.2026
 */
enum class SyncState {

    /**
     * The server has everything the device has
     */
    Synced,

    /**
     * Reported on the device and not yet accepted by the server
     */
    PendingCreate,

    /**
     * Edited on the device and not yet accepted by the server
     */
    PendingUpdate,

    /**
     * Withdrawn on the device and not yet accepted by the server
     */
    PendingWithdraw,
}
