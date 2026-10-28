package com.example.hazardreporting.presentation.login

/**
 * One-off events of the sign-in screen
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface LoginEvent {

    /**
     * Signing in succeeded; the screen leaves for the incident list
     */
    data object LoggedIn : LoginEvent

    /**
     * Something nobody expected failed; the screen shows a snackbar
     */
    data object UnexpectedError : LoginEvent
}
