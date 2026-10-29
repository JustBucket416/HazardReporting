package com.example.hazardreporting.data.local

import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class DatabaseLocalDataSourceTest {

    private val database = mockk<HazardReportingDatabase>()

    private val dataSource = DatabaseLocalDataSource(database)

    @Test
    fun testClearAllTables() {
        justRun { database.clearAllTables() }

        dataSource.clearAllTables()

        verifySequence { database.clearAllTables() }
    }
}
