package com.example.hazardreporting.data.session

import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.repository.SessionRepository
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Implementation of [SessionRepository]
 *
 * @property remoteDataSource used for calling the server's session routes
 * @property sessionHolder keeps the session in memory and in DataStore
 * @property converter used for converting the server's response to a [Session]
 * @property dispatchers used for running on the IO dispatcher
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SessionRepositoryImpl @Inject constructor(
    private val remoteDataSource: SessionRemoteDataSource,
    private val sessionHolder: SessionHolder,
    private val converter: SessionConverter,
    private val dispatchers: CoroutineDispatchers,
) : SessionRepository {

    override suspend fun openSession(email: String, password: String): Session = withContext(dispatchers.io) {
        converter.convert(remoteDataSource.login(email, password))
    }

    override suspend fun saveSession(session: Session) {
        withContext(dispatchers.io) { sessionHolder.save(session) }
    }

    override suspend fun closeSession(session: Session) {
        withContext(dispatchers.io) {
            try {
                remoteDataSource.logout(session.token)
            } catch (e: NoConnectionException) {
                // Best effort: an unreachable server keeps a session nobody will use again
            } catch (e: UnauthorizedException) {
                // The server already forgot the session
            } catch (e: HttpException) {
                // Any other refusal leaves nothing for the device to do
            }
        }
    }

    override suspend fun currentUser(): User? = withContext(dispatchers.io) {
        sessionHolder.session()?.user
    }

    override fun observeSessionInvalid(): Flow<Boolean> = sessionHolder.isRejected

    override fun markSessionInvalid() {
        sessionHolder.markRejected()
    }
}
