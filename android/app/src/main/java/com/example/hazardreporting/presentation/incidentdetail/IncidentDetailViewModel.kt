package com.example.hazardreporting.presentation.incidentdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.interactor.CatalogInteractor
import com.example.hazardreporting.domain.interactor.IncidentInteractor
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.presentation.common.BaseViewModel
import com.example.hazardreporting.presentation.navigation.IncidentDetail
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Manages the incident detail screen's state. Opening the screen downloads the incident's details only if
 * they were never downloaded; pull-to-refresh downloads them again (ADR-0006). Withdrawing asks for confirmation
 * first and then leaves the screen. A Status Change Notice shows as soon as a push stores it, until dismissed
 *
 * @param savedStateHandle holds the route with the incident's local id
 * @property incidentInteractor used for observing the incident and downloading its details
 * @param catalogInteractor used for naming the incident's category, severity and site
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentInteractor: IncidentInteractor,
    catalogInteractor: CatalogInteractor,
) : BaseViewModel() {

    private val localId = savedStateHandle.toRoute<IncidentDetail>().localId
    private val loadStatus = MutableStateFlow(LoadStatus())
    private val isWithdrawDialogVisible = MutableStateFlow(false)
    private val _events = Channel<IncidentDetailEvent>(Channel.BUFFERED)

    /**
     * State of the incident detail screen
     */
    val uiState: StateFlow<IncidentDetailUiState> = combine(
        incidentInteractor.observeIncident(localId),
        combine(
            catalogInteractor.observeCategories(),
            catalogInteractor.observeSeverities(),
            catalogInteractor.observeSites(),
            ::Catalog,
        ),
        loadStatus,
        isWithdrawDialogVisible,
    ) { incident, catalog, load, isWithdrawDialogVisible ->
        toUiState(incident, catalog, load, isWithdrawDialogVisible)
    }.stateIn(
        scope = viewModelScope + exceptionHandler,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = IncidentDetailUiState(),
    )

    /**
     * One-off events of the incident detail screen
     */
    val events: Flow<IncidentDetailEvent> = _events.receiveAsFlow()

    init {
        load(LoadKind.Open)
    }

    /**
     * Downloads the details again after they failed to download. Ignored while a download is in flight
     */
    fun onRetry() {
        load(LoadKind.Open)
    }

    /**
     * Downloads the details again on a pull-to-refresh, even if they are on the device.
     * Ignored while a download is in flight
     */
    fun onRefresh() {
        load(LoadKind.Refresh)
    }

    /**
     * Asks "Withdraw this report?" after the technician picked Withdraw… in the ⋮ menu
     */
    fun onWithdrawClick() {
        isWithdrawDialogVisible.update { true }
    }

    /**
     * Closes "Withdraw this report?" without withdrawing
     */
    fun onWithdrawDismiss() {
        isWithdrawDialogVisible.update { false }
    }

    /**
     * Withdraws the incident on the device, which never waits for the network, and leaves the screen
     */
    fun onWithdrawConfirm() {
        isWithdrawDialogVisible.update { false }
        viewModelScope.launch(exceptionHandler) {
            incidentInteractor.withdraw(localId)
            _events.send(IncidentDetailEvent.Withdrawn)
        }
    }

    /**
     * Clears the Status Change Notice once the technician has read it
     */
    fun onDismissStatusChangeNotice() {
        viewModelScope.launch(exceptionHandler) {
            incidentInteractor.dismissStatusChangeNotice(localId)
        }
    }

    override fun onUnexpected(throwable: Throwable) {
        loadStatus.update { if (it.inFlight != null) LoadStatus(hasFailed = true) else it }
        _events.trySend(IncidentDetailEvent.UnexpectedError)
    }

    private fun load(kind: LoadKind) {
        val previous = loadStatus.getAndUpdate { if (it.inFlight == null) it.copy(inFlight = kind) else it }
        if (previous.inFlight != null) return

        viewModelScope.launch(exceptionHandler) {
            val succeeded = try {
                incidentInteractor.loadIncidentDetails(localId, force = kind == LoadKind.Refresh)
                true
            } catch (e: NoConnectionException) {
                false
            } catch (e: UnauthorizedException) {
                // The interceptor marked the session as invalid, which the list explains
                false
            }

            if (succeeded) {
                loadStatus.update { LoadStatus() }
            } else {
                val showsDetails = uiState.value.incident?.details is IncidentDetailsState.Loaded
                loadStatus.update { LoadStatus(hasFailed = true) }
                if (showsDetails) _events.send(IncidentDetailEvent.RefreshFailed)
            }
        }
    }

    private fun toUiState(
        incident: Incident,
        catalog: Catalog,
        load: LoadStatus,
        isWithdrawDialogVisible: Boolean,
    ): IncidentDetailUiState {
        val summary = incident.summary
        val details = when {
            incident.details != null -> IncidentDetailsState.Loaded(incident.details)
            load.inFlight == null && load.hasFailed -> IncidentDetailsState.NotDownloaded
            else -> IncidentDetailsState.Loading
        }
        val content = IncidentDetailContent(
            title = summary.title,
            status = summary.status,
            severity = catalog.severities.find { it.id == summary.severityId },
            categoryName = catalog.categories.find { it.id == summary.categoryId }?.name,
            siteName = catalog.sites.find { it.id == summary.siteId }?.name,
            createdAt = summary.createdAt,
            isPending = summary.syncState != SyncState.Synced,
            hasStatusChangeNotice = summary.hasStatusChangeNotice,
            details = details,
        )
        return IncidentDetailUiState(
            incident = content,
            isRefreshing = load.inFlight == LoadKind.Refresh,
            isWithdrawDialogVisible = isWithdrawDialogVisible,
        )
    }

    /**
     * The stored catalog the incident is named from
     *
     * @property categories the stored categories
     * @property severities the stored severities
     * @property sites the stored sites
     */
    private data class Catalog(
        val categories: List<Category>,
        val severities: List<Severity>,
        val sites: List<Site>,
    )

    /**
     * What the screen knows about downloading the details
     *
     * @property inFlight the kind of download in flight, or null if none is
     * @property hasFailed whether the last download failed
     */
    private data class LoadStatus(
        val inFlight: LoadKind? = null,
        val hasFailed: Boolean = false,
    )

    /**
     * Why the details are downloaded, which decides whether stored details are downloaded again
     */
    private enum class LoadKind {

        /**
         * On opening the screen, or on Retry: only if the details were never downloaded
         */
        Open,

        /**
         * By pull-to-refresh: always
         */
        Refresh,
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
