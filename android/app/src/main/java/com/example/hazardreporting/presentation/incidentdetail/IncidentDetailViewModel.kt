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
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.example.hazardreporting.utils.Logger
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
 * they were never downloaded; pull-to-refresh downloads them again (ADR-0006)
 *
 * @param savedStateHandle holds the route with the incident's local id
 * @property incidentInteractor used for observing the incident and downloading its details
 * @param catalogInteractor used for naming the incident's category, severity and site
 * @property dispatchers used for launching off the main thread
 * @param logger used for logging unexpected exceptions
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentInteractor: IncidentInteractor,
    catalogInteractor: CatalogInteractor,
    private val dispatchers: CoroutineDispatchers,
    logger: Logger,
) : BaseViewModel(logger) {

    private val localId = savedStateHandle.toRoute<IncidentDetail>().localId
    private val loadStatus = MutableStateFlow(LoadStatus())
    private val _events = Channel<IncidentDetailEvent>(Channel.BUFFERED)

    /**
     * State of the incident detail screen
     */
    val uiState: StateFlow<IncidentDetailUiState> = combine(
        incidentInteractor.observeIncident(localId),
        catalogInteractor.observeCategories(),
        catalogInteractor.observeSeverities(),
        catalogInteractor.observeSites(),
        loadStatus,
    ) { incident, categories, severities, sites, load ->
        toUiState(incident, categories, severities, sites, load)
    }.stateIn(
        scope = viewModelScope + exceptionHandler + dispatchers.default,
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

    override fun onUnexpected(throwable: Throwable) {
        loadStatus.update { if (it.inFlight != null) LoadStatus(hasFailed = true) else it }
        _events.trySend(IncidentDetailEvent.UnexpectedError)
    }

    private fun load(kind: LoadKind) {
        val previous = loadStatus.getAndUpdate { if (it.inFlight == null) it.copy(inFlight = kind) else it }
        if (previous.inFlight != null) return

        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            val succeeded = try {
                incidentInteractor.loadIncidentDetails(localId, force = kind == LoadKind.Refresh)
                true
            } catch (e: NoConnectionException) {
                false
            } catch (e: UnauthorizedException) {
                // The session-invalid banner replaces this with the synchroniser (issue 13)
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
        categories: List<Category>,
        severities: List<Severity>,
        sites: List<Site>,
        load: LoadStatus,
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
            severity = severities.find { it.id == summary.severityId },
            categoryName = categories.find { it.id == summary.categoryId }?.name,
            siteName = sites.find { it.id == summary.siteId }?.name,
            createdAt = summary.createdAt,
            isPending = summary.syncState != SyncState.Synced,
            details = details,
        )
        return IncidentDetailUiState(incident = content, isRefreshing = load.inFlight == LoadKind.Refresh)
    }

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
