package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.session.dto.LoginResponseDto
import com.example.hazardreporting.domain.exception.InvalidCredentialsException
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
import org.junit.Assert
import org.junit.Before
import org.junit.Test

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
    fun testLogin_Success() = runTest {
        val response = mockk<LoginResponseDto>()

        coEvery { remoteDataSource.login(EMAIL, PASSWORD) } returns response
        every { converter.convert(response) } returns SESSION
        coJustRun { sessionHolder.save(SESSION) }

        val user = repository.login(EMAIL, PASSWORD)

        coVerifySequence {
            remoteDataSource.login(EMAIL, PASSWORD)
            converter.convert(response)
            sessionHolder.save(SESSION)
        }

        Truth.assertThat(user).isEqualTo(SESSION.user)
    }

    @Test
    fun testLogin_InvalidCredentials() {
        coEvery { remoteDataSource.login(EMAIL, PASSWORD) } throws InvalidCredentialsException()

        Assert.assertThrows(InvalidCredentialsException::class.java) {
            runTest { repository.login(EMAIL, PASSWORD) }
        }

        coVerifySequence { remoteDataSource.login(EMAIL, PASSWORD) }
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

    private companion object {
        const val EMAIL = "tech1@example.com"
        const val PASSWORD = "password"
        val SESSION = Session("token", User("u1", EMAIL, "Tech One", UserRole.Technician))
    }
}
