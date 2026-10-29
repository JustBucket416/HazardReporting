package com.example.hazardreporting.domain.repository

/**
 * Schedules sending the device's pending changes to the server in the background (ADR-0002).
 * Saving a change never waits for the network; it only asks for a push
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface SyncScheduler {

    /**
     * Asks for the pending changes to be pushed as soon as the device is online.
     * Returns at once; asking again while a push is already scheduled has no further effect
     */
    fun schedulePush()

    /**
     * Makes sure the pending changes are pushed every 15 minutes while the device is online, in case a scheduled
     * push gave up. Returns at once; calling it again keeps the existing schedule
     */
    fun schedulePeriodicPush()
}
