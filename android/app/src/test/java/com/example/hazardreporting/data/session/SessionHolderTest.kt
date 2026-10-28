package com.example.hazardreporting.data.session

import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionHolderTest {

    private val localDataSource = mockk<SessionLocalDataSource>()
    private val dispatchers = mockk<CoroutineDispatchers>()

    private lateinit var holder: SessionHolder

    @Before
    fun setUp() {
        every { dispatchers.io } returns UnconfinedTestDispatcher()

        holder = SessionHolder(localDataSource, dispatchers)
    }

    @Test
    fun testSessionBlocking_LoadedWithSession() = runTest {
        coEvery { localDataSource.read() } returns SESSION

        holder.load()
        val session = holder.sessionBlocking()

        coVerifySequence { localDataSource.read() }

        Truth.assertThat(session).isEqualTo(SESSION)
    }

    @Test
    fun testSessionBlocking_LoadedSignedOut() = runTest {
        coEvery { localDataSource.read() } returns null

        holder.load()
        val firstSession = holder.sessionBlocking()
        val secondSession = holder.sessionBlocking()

        // Signed out is a loaded state too, so no further DataStore reads happen
        coVerifySequence { localDataSource.read() }

        Truth.assertThat(firstSession).isNull()
        Truth.assertThat(secondSession).isNull()
    }

    @Test
    fun testSessionBlocking_BeforeLoad() {
        coEvery { localDataSource.read() } returns SESSION

        val firstSession = holder.sessionBlocking()
        val secondSession = holder.sessionBlocking()

        // The first call falls back to the blocking read and keeps its result
        coVerifySequence { localDataSource.read() }

        Truth.assertThat(firstSession).isEqualTo(SESSION)
        Truth.assertThat(secondSession).isEqualTo(SESSION)
    }

    @Test
    fun testSession_BeforeLoad() = runTest {
        coEvery { localDataSource.read() } returns SESSION

        val session = holder.session()

        coVerifySequence { localDataSource.read() }

        Truth.assertThat(session).isEqualTo(SESSION)
    }

    @Test
    fun testLoad_AlreadyLoaded() = runTest {
        coEvery { localDataSource.read() } returns SESSION

        holder.load()
        holder.load()

        coVerifySequence { localDataSource.read() }
    }

    @Test
    fun testSave() = runTest {
        coJustRun { localDataSource.write(SESSION) }

        holder.save(SESSION)
        val session = holder.sessionBlocking()

        coVerifySequence { localDataSource.write(SESSION) }

        Truth.assertThat(session).isEqualTo(SESSION)
    }

    @Test
    fun testLoad_SessionSavedDuringRead() = runTest {
        val savedSession = SESSION.copy(token = "newer-token")

        coJustRun { localDataSource.write(savedSession) }
        coEvery { localDataSource.read() } coAnswers {
            holder.save(savedSession)
            null
        }

        holder.load()

        coVerifySequence {
            localDataSource.read()
            localDataSource.write(savedSession)
        }

        Truth.assertThat(holder.session()).isEqualTo(savedSession)
    }

    private companion object {
        val SESSION = Session("token", User("u1", "tech1@example.com", "Tech One", UserRole.Technician))
    }
}
