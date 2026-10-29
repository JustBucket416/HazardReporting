package com.example.hazardreporting.domain.model

/**
 * How an attempt to sign out ended
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface LogoutResult {

    /**
     * Signed out: everything on the device was wiped
     */
    data object LoggedOut : LogoutResult

    /**
     * Not signed out, because some pending changes couldn't be sent and signing out would lose them;
     * nothing was wiped
     *
     * @property count how many incidents still have a pending change
     */
    data class PendingChangesRemain(val count: Int) : LogoutResult
}
