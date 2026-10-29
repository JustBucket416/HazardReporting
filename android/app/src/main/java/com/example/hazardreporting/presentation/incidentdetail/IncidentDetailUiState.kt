package com.example.hazardreporting.presentation.incidentdetail

import com.example.hazardreporting.domain.model.IncidentDetails
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.Severity

/**
 * State of the incident detail screen
 *
 * @property incident the incident, or null until it is read from the device
 * @property isRefreshing whether a pull-to-refresh is in flight
 * @property isWithdrawDialogVisible whether "Withdraw this report?" asks for confirmation
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentDetailUiState(
    val incident: IncidentDetailContent? = null,
    val isRefreshing: Boolean = false,
    val isWithdrawDialogVisible: Boolean = false,
) {

    /**
     * Whether the ⋮ menu with Edit and Withdraw shows: only once the details are downloaded and while the incident
     * is Open, the only status in which the reporter may change it (ADR-0006)
     */
    val isMenuVisible: Boolean
        get() {
            val content = incident ?: return false
            return content.details is IncidentDetailsState.Loaded && content.status == IncidentStatus.Open
        }
}

/**
 * The incident the detail screen shows
 *
 * @property title short description of the problem
 * @property status where the incident stands in its lifecycle
 * @property severity the incident's severity, or null if it isn't in the stored catalog
 * @property categoryName name of the incident's category, or null if it isn't in the stored catalog
 * @property siteName name of the incident's site, or null if it isn't in the stored catalog
 * @property createdAt when the incident was reported, in epoch milliseconds
 * @property isPending whether the incident has a change the server hasn't accepted yet
 * @property hasStatusChangeNotice whether a pending change was refused because an operator had moved the incident
 * out of Open, and the technician hasn't dismissed the notice yet
 * @property details the incident's details, or why they aren't shown
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentDetailContent(
    val title: String,
    val status: IncidentStatus,
    val severity: Severity?,
    val categoryName: String?,
    val siteName: String?,
    val createdAt: Long,
    val isPending: Boolean,
    val hasStatusChangeNotice: Boolean,
    val details: IncidentDetailsState,
)

/**
 * What the detail screen shows in place of an incident's details
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface IncidentDetailsState {

    /**
     * The details are being downloaded: they shimmer
     */
    data object Loading : IncidentDetailsState

    /**
     * Downloading the details failed and they were never downloaded: "Details not downloaded" with Retry
     */
    data object NotDownloaded : IncidentDetailsState

    /**
     * The details are on the device
     *
     * @property details the incident's details
     */
    data class Loaded(val details: IncidentDetails) : IncidentDetailsState
}
