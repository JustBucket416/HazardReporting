package com.example.hazardreporting.data.local

import javax.inject.Inject

/**
 * Acts on the Room database as a whole
 *
 * @property database the app's database
 *
 * @author Roman Pliskin on 29.10.2026
 */
class DatabaseLocalDataSource @Inject constructor(
    private val database: HazardReportingDatabase,
) {

    /**
     * Deletes every row of every table. Blocks, so it must run off the main thread, and outside a transaction
     */
    fun clearAllTables() {
        database.clearAllTables()
    }
}
