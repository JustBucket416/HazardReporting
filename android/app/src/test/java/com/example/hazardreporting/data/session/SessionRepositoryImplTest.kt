package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.session.dto.LoginResponseDto
import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.google.common.truth.Truth
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryImplTest {

    private val remoteDataSource = mockk<SessionRemoteDataSource>()
    private val sessionHolder = mockk<SessionHolder>()
    private val converter = mockk<SessionConverter>()
    private val dispatchers = mockk<CoroutineDispatchers>()

    private lateinit var repository: SessionRepositoryImpl

    @Before
    fun setUp() {
        every { dispatchers.io } returns UnconfinedTestDispatcher()

        repository = SessionRepositoryImpl(remoteDataSource, sessionHolder, converter, dispatchers)
    }

    @Test
    fun testOpenSession_Success() = runTest {
        val response = mockk<LoginResponseDto>()

        coEvery { remoteDataSource.login(EMAIL, PASSWORD) } returns response
        every { converter.convert(response) } returns SESSION

        val session = repository.openSession(EMAIL, PASSWORD)

        coVerifySequence {
            remoteDataSource.login(EMAIL, PASSWORD)
            converter.convert(response)
        }
        // Opening a session stores nothing; the caller decides whether to keep it
        verify { sessionHolder wasNot Called }

        Truth.assertThat(session).isEqualTo(SESSION)
    }

    @Test
    fun testOpenSession_InvalidCredentials() {
        coEvery { remoteDataSource.login(EMAIL, PASSWORD) } throws InvalidCredentialsException()

        Assert.assertThrows(InvalidCredentialsException::class.java) {
            runTest { repository.openSession(EMAIL, PASSWORD) }
        }

        coVerifySequence { remoteDataSource.login(EMAIL, PASSWORD) }
    }

    @Test
    fun testSaveSession() = runTest {
        coJustRun { sessionHolder.save(SESSION) }

        repository.saveSession(SESSION)

        coVerifySequence { sessionHolder.save(SESSION) }
    }

    @Test
    fun testCloseSession_Success() = runTest {
        coJustRun { remoteDataSource.logout(TOKEN) }

        repository.closeSession(SESSION)

        coVerifySequence { remoteDataSource.logout(TOKEN) }
        verify { sessionHolder wasNot Called }
    }

    @Test
    fun testCloseSession_NoConnection() = runTest {
        coEvery { remoteDataSource.logout(TOKEN) } throws NoConnectionException()

        // Best effort: the failure is ignored
        repository.closeSession(SESSION)

        coVerifySequence { remoteDataSource.logout(TOKEN) }
    }

    @Test
    fun testCloseSession_Unauthorized() = runTest {
        coEvery { remoteDataSource.logout(TOKEN) } throws UnauthorizedException()

        repository.closeSession(SESSION)

        coVerifySequence { remoteDataSource.logout(TOKEN) }
    }

    @Test
    fun testCloseSession_ServerError() = runTest {
        coEvery { remoteDataSource.logout(TOKEN) } throws HttpException(Response.error<Any>(500, "".toResponseBody()))

        repository.closeSession(SESSION)

        coVerifySequence { remoteDataSource.logout(TOKEN) }
    }

    @Test
    fun testCloseSession_UnexpectedException() {
        val exception = IllegalStateException()

        coEvery { remoteDataSource.logout(TOKEN) } throws exception

        // Only transport failures are ignored; anything else is a bug and propagates
        val thrown = Assert.assertThrows(IllegalStateException::class.java) {
            runTest { repository.closeSession(SESSION) }
        }

        Truth.assertThat(thrown).isSameInstanceAs(exception)
    }

    @Test
    fun testCurrentUser_SignedIn() = runTest {
        coEvery { sessionHolder.session() } returns SESSION

        val user = repository.currentUser()

        coVerifySequence { sessionHolder.session() }

        Truth.assertThat(user).isEqualTo(SESSION.user)
    }

    @Test
    fun testCurrentUser_SignedOut() = runTest {
        coEvery { sessionHolder.session() } returns null

        val user = repository.currentUser()

        coVerifySequence { sessionHolder.session() }

        Truth.assertThat(user).isNull()
    }

    @Test
    fun testCurrentSession() = runTest {
        coEvery { sessionHolder.session() } returns SESSION

        Truth.assertThat(repository.currentSession()).isEqualTo(SESSION)
    }

    @Test
    fun testClearSession() = runTest {
        coJustRun { sessionHolder.clear() }

        repository.clearSession()

        coVerifySequence { sessionHolder.clear() }
    }

    @Test
    fun testObserveSessionInvalid() {
        val isRejected = MutableStateFlow(true)

        every { sessionHolder.isRejected } returns isRejected

        Truth.assertThat(repository.observeSessionInvalid()).isSameInstanceAs(isRejected)
    }

    @Test
    fun testMarkSessionInvalid() {
        justRun { sessionHolder.markRejected() }

        repository.markSessionInvalid()

        verify(exactly = 1) { sessionHolder.markRejected() }
    }

    private companion object {
        const val EMAIL = "tech1@example.com"
        const val PASSWORD = "password"
        const val TOKEN = "token"
        val SESSION = Session(TOKEN, User("u1", EMAIL, "Tech One", UserRole.Technician))
    }
}
