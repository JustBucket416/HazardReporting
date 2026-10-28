package com.example.hazardreporting.data.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.google.common.truth.Truth
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Runs against a real DataStore in a temporary folder
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SessionLocalDataSourceTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun testRead_NothingStored() = runTest {
        val dataSource = createDataSource()

        Truth.assertThat(dataSource.read()).isNull()
    }

    @Test
    fun testWriteAndRead() = runTest {
        val dataSource = createDataSource()

        dataSource.write(SESSION)

        Truth.assertThat(dataSource.read()).isEqualTo(SESSION)
    }

    @Test
    fun testWrite_ReplacesStoredSession() = runTest {
        val dataSource = createDataSource()
        val newSession = Session("new-token", SESSION.user.copy(id = "u2", role = UserRole.Operator))

        dataSource.write(SESSION)
        dataSource.write(newSession)

        Truth.assertThat(dataSource.read()).isEqualTo(newSession)
    }

    private fun TestScope.createDataSource(): SessionLocalDataSource {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporaryFolder.root, "session.preferences_pb") },
        )
        return SessionLocalDataSource(dataStore)
    }

    private companion object {
        val SESSION = Session("token", User("u1", "tech1@example.com", "Tech One", UserRole.Technician))
    }
}
