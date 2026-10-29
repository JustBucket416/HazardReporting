package com.example.hazardreporting.presentation.incidentdetail

/**
 * One-off events of the incident detail screen
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface IncidentDetailEvent {

    /**
     * Refreshing failed while the stored details are shown; the screen keeps them and shows a snackbar
     */
    data object RefreshFailed : IncidentDetailEvent

    /**
     * The incident was withdrawn on the device; the screen goes back
     */
    data object Withdrawn : IncidentDetailEvent

    /**
     * Something nobody expected failed; the screen shows a snackbar
     */
    data object UnexpectedError : IncidentDetailEvent
}
