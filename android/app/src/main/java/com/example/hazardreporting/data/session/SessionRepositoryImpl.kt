package com.example.hazardreporting.data.session

import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.repository.SessionRepository
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.withContext

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

    override suspend fun login(email: String, password: String): User = withContext(dispatchers.io) {
        val session = converter.convert(remoteDataSource.login(email, password))
        sessionHolder.save(session)
        session.user
    }

    override suspend fun currentUser(): User? = withContext(dispatchers.io) {
        sessionHolder.session()?.user
    }
}
