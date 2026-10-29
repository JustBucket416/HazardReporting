package com.example.hazardreporting.presentation.incidentlist

import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.Severity

/**
 * State of the incident list screen
 *
 * @property content what the list area shows
 * @property isRefreshing whether a pull-to-refresh is in flight
 * @property isSessionInvalid whether the server rejected the session, which a persistent banner explains
 * @property logoutDialog the step of signing out the logout dialog shows, or null while it is closed
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentListUiState(
    val content: IncidentListContent = IncidentListContent.Loading,
    val isRefreshing: Boolean = false,
    val isSessionInvalid: Boolean = false,
    val logoutDialog: LogoutDialog? = null,
)

/**
 * A step of signing out from the list's ⋮ menu (spec § 3.5)
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface LogoutDialog {

    /**
     * "Log out? Data on this device will be removed."
     */
    data object Confirm : LogoutDialog

    /**
     * Pending changes are being sent before the device is wiped: a spinner, with the buttons disabled
     *
     * @property pendingCount how many incidents have a pending change to send; 0 if there is nothing to send
     */
    data class Sending(val pendingCount: Int) : LogoutDialog

    /**
     * Some pending changes couldn't be sent: "N reports couldn't be sent and will be lost", with Log out anyway
     * and Cancel
     *
     * @property pendingCount how many incidents still have a pending change
     */
    data class Unsent(val pendingCount: Int) : LogoutDialog
}

/**
 * What the incident list area shows
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface IncidentListContent {

    /**
     * The first load is in flight and nothing is stored yet: skeleton rows
     */
    data object Loading : IncidentListContent

    /**
     * The technician hasn't reported anything yet
     */
    data object Empty : IncidentListContent

    /**
     * Loading failed and nothing is stored: an error card with Retry
     */
    data object Error : IncidentListContent

    /**
     * The technician's incidents, newest first
     *
     * @property items one row per incident
     */
    data class Incidents(val items: List<IncidentListItem>) : IncidentListContent
}

/**
 * One row of the incident list
 *
 * @property localId local id of the incident, which opens its details
 * @property title short description of the problem
 * @property categoryName name of the incident's category, or null if it isn't in the stored catalog
 * @property siteName name of the incident's site, or null if it isn't in the stored catalog
 * @property severity the incident's severity, or null if it isn't in the stored catalog
 * @property status where the incident stands in its lifecycle
 * @property createdAt when the incident was reported, in epoch milliseconds
 * @property isPending whether the incident has a change the server hasn't accepted yet
 * @property hasStatusChangeNotice whether a Status Change Notice waits to be read
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentListItem(
    val localId: Long,
    val title: String,
    val categoryName: String?,
    val siteName: String?,
    val severity: Severity?,
    val status: IncidentStatus,
    val createdAt: Long,
    val isPending: Boolean,
    val hasStatusChangeNotice: Boolean,
)
