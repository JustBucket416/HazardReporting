package com.example.hazardreporting.data.session

import com.example.hazardreporting.domain.model.Session

/**
 * What [SessionHolder] knows about the session. Telling "not loaded yet" apart from
 * "loaded, signed out" is what lets signed-out requests skip the blocking read (ADR-0010)
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface SessionState {

    /**
     * The stored session hasn't been read yet
     */
    data object Loading : SessionState

    /**
     * The stored session has been read
     *
     * @property session the session, or null if nobody is signed in
     */
    data class Loaded(val session: Session?) : SessionState
}
