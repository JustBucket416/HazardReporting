package com.example.hazardreporting.data.session

import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Keeps the [Session] in memory, so the synchronous [AuthInterceptor] can read the token (ADR-0010).
 * [load] runs at startup; until it finishes, [sessionBlocking] falls back to a blocking DataStore read.
 * [save] updates DataStore and memory together. It also remembers, for as long as the process lives, whether
 * the server rejected the session ([isRejected]); the app never signs out on its own because of that (ADR-0007)
 *
 * @property localDataSource stores the session
 * @property dispatchers used for reading and writing the stored session on the IO dispatcher
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Singleton
class SessionHolder @Inject constructor(
    private val localDataSource: SessionLocalDataSource,
    private val dispatchers: CoroutineDispatchers,
) {

    private val state = MutableStateFlow<SessionState>(SessionState.Loading)
    private val _isRejected = MutableStateFlow(false)

    /**
     * Whether the server answered a request with 401 since the session was saved, e.g. after it was reset.
     * Sync stops until the technician signs out and in again
     */
    val isRejected: StateFlow<Boolean> = _isRejected.asStateFlow()

    /**
     * Reads the stored session into memory, unless it's already there
     */
    suspend fun load() {
        if (state.value == SessionState.Loading) readStoredSession()
    }

    /**
     * Returns the session, or null if nobody is signed in. Doesn't block once the session is loaded;
     * before that, blocks the calling thread on a DataStore read. Only [AuthInterceptor] calls this,
     * on OkHttp's threads, never on Main
     */
    fun sessionBlocking(): Session? = when (val current = state.value) {
        is SessionState.Loaded -> current.session
        SessionState.Loading -> runBlocking { readStoredSession() }
    }

    /**
     * Returns the session, or null if nobody is signed in, reading it from DataStore if it isn't loaded yet
     */
    suspend fun session(): Session? = when (val current = state.value) {
        is SessionState.Loaded -> current.session
        SessionState.Loading -> readStoredSession()
    }

    /**
     * Stores [session] in DataStore and then in memory. A new session isn't rejected
     */
    suspend fun save(session: Session) {
        withContext(dispatchers.io) { localDataSource.write(session) }
        state.value = SessionState.Loaded(session)
        _isRejected.value = false
    }

    /**
     * Records that the server rejected the session
     */
    fun markRejected() {
        _isRejected.value = true
    }

    private suspend fun readStoredSession(): Session? {
        val stored = withContext(dispatchers.io) { localDataSource.read() }
        // A session saved while this read was in flight is newer, so it wins
        state.compareAndSet(SessionState.Loading, SessionState.Loaded(stored))
        return (state.value as SessionState.Loaded).session
    }
}
