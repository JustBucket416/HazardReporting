package com.example.hazardreporting.presentation.incidentlist

/**
 * One-off events of the incident list screen
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface IncidentListEvent {

    /**
     * A refresh failed while stored incidents are shown; the screen keeps them and shows a snackbar
     */
    data object RefreshFailed : IncidentListEvent

    /**
     * The technician signed out and the device was wiped; the app goes to Login with the back stack cleared
     */
    data object LoggedOut : IncidentListEvent

    /**
     * Something nobody expected failed; the screen shows a snackbar
     */
    data object UnexpectedError : IncidentListEvent
}
