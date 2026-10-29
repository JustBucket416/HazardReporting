package com.example.hazardreporting.presentation.incidentlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.interactor.CatalogInteractor
import com.example.hazardreporting.domain.interactor.IncidentInteractor
import com.example.hazardreporting.domain.interactor.LoginInteractor
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.presentation.common.BaseViewModel
import com.example.hazardreporting.presentation.navigation.IncidentList
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
 * Manages the incident list screen's state. The stored incidents are shown as they are; a refresh pushes the
 * pending changes and pulls the server's incidents on top of them. When the list is opened right after signing in
 * ([IncidentList.refreshOnOpen]), it starts with a first load that shows skeleton rows, or an error card if it fails
 * with nothing stored. A banner shows while the server rejects the session
 *
 * @param savedStateHandle holds the route and remembers that the opening refresh was started
 * @property incidentInteractor used for observing and refreshing the incidents
 * @param catalogInteractor used for naming each incident's category, severity and site
 * @param loginInteractor used for observing whether the session is still valid
 * @property dispatchers used for launching off the main thread
 * @param logger used for logging unexpected exceptions
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentInteractor: IncidentInteractor,
    catalogInteractor: CatalogInteractor,
    loginInteractor: LoginInteractor,
    private val dispatchers: CoroutineDispatchers,
    logger: Logger,
) : BaseViewModel(logger) {

    private val refreshStatus = MutableStateFlow(RefreshStatus())
    private val _events = Channel<IncidentListEvent>(Channel.BUFFERED)

    /**
     * State of the incident list screen
     */
    val uiState: StateFlow<IncidentListUiState> = combine(
        incidentInteractor.observeMyIncidents(),
        combine(
            catalogInteractor.observeCategories(),
            catalogInteractor.observeSeverities(),
            catalogInteractor.observeSites(),
            ::Catalog,
        ),
        refreshStatus,
        loginInteractor.observeSessionInvalid(),
    ) { incidents, catalog, refresh, isSessionInvalid ->
        toUiState(incidents, catalog, refresh, isSessionInvalid)
    }.stateIn(
        scope = viewModelScope + exceptionHandler + dispatchers.default,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = IncidentListUiState(),
    )

    /**
     * One-off events of the incident list screen
     */
    val events: Flow<IncidentListEvent> = _events.receiveAsFlow()

    init {
        val route = savedStateHandle.toRoute<IncidentList>()
        // Saved, so a list restored after process death doesn't pull again
        if (route.refreshOnOpen && savedStateHandle.get<Boolean>(KEY_OPENING_REFRESH_STARTED) != true) {
            savedStateHandle[KEY_OPENING_REFRESH_STARTED] = true
            refresh(RefreshKind.FirstLoad)
        }
    }

    /**
     * Pulls the server's incidents on a pull-to-refresh. Ignored while a refresh is already in flight
     */
    fun onRefresh() {
        refresh(RefreshKind.Pull)
    }

    /**
     * Loads the incidents again after a failed first load. Ignored while a refresh is already in flight
     */
    fun onRetry() {
        refresh(RefreshKind.FirstLoad)
    }

    override fun onUnexpected(throwable: Throwable) {
        refreshStatus.update { if (it.inFlight != null) RefreshStatus(hasFailed = true) else it }
        _events.trySend(IncidentListEvent.UnexpectedError)
    }

    private fun refresh(kind: RefreshKind) {
        val previous = refreshStatus.getAndUpdate { if (it.inFlight == null) it.copy(inFlight = kind) else it }
        if (previous.inFlight != null) return

        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            val succeeded = try {
                incidentInteractor.refreshIncidents()
                true
            } catch (e: NoConnectionException) {
                false
            } catch (e: UnauthorizedException) {
                // The interactor marked the session as invalid, which shows the banner
                false
            }

            if (succeeded) {
                refreshStatus.update { RefreshStatus() }
            } else {
                val showsIncidents = uiState.value.content is IncidentListContent.Incidents
                refreshStatus.update { RefreshStatus(hasFailed = true) }
                if (showsIncidents) _events.send(IncidentListEvent.RefreshFailed)
            }
        }
    }

    private fun toUiState(
        incidents: List<IncidentSummary>,
        catalog: Catalog,
        refresh: RefreshStatus,
        isSessionInvalid: Boolean,
    ): IncidentListUiState {
        val categoriesById = catalog.categories.associateBy { it.id }
        val severitiesById = catalog.severities.associateBy { it.id }
        val sitesById = catalog.sites.associateBy { it.id }
        val content = when {
            incidents.isNotEmpty() -> IncidentListContent.Incidents(
                incidents.map { incident ->
                    IncidentListItem(
                        localId = incident.localId,
                        title = incident.title,
                        categoryName = categoriesById[incident.categoryId]?.name,
                        siteName = sitesById[incident.siteId]?.name,
                        severity = severitiesById[incident.severityId],
                        status = incident.status,
                        createdAt = incident.createdAt,
                        isPending = incident.syncState != SyncState.Synced,
                        hasStatusChangeNotice = incident.hasStatusChangeNotice,
                    )
                }
            )
            refresh.inFlight == RefreshKind.FirstLoad -> IncidentListContent.Loading
            refresh.hasFailed -> IncidentListContent.Error
            else -> IncidentListContent.Empty
        }
        return IncidentListUiState(
            content = content,
            isRefreshing = refresh.inFlight == RefreshKind.Pull,
            isSessionInvalid = isSessionInvalid,
        )
    }

    /**
     * The stored catalog the rows are named from
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
     * What the screen knows about refreshing
     *
     * @property inFlight the kind of refresh in flight, or null if none is
     * @property hasFailed whether the last refresh failed
     */
    private data class RefreshStatus(
        val inFlight: RefreshKind? = null,
        val hasFailed: Boolean = false,
    )

    /**
     * How a refresh was started, which decides how it shows
     */
    private enum class RefreshKind {

        /**
         * On opening the list after signing in, or on Retry: skeleton rows while nothing is stored
         */
        FirstLoad,

        /**
         * By pull-to-refresh: the pull indicator
         */
        Pull,
    }

    private companion object {
        const val KEY_OPENING_REFRESH_STARTED = "openingRefreshStarted"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
