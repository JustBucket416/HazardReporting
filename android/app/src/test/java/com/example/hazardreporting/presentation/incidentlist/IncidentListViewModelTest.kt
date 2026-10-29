package com.example.hazardreporting.presentation.incidentlist

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.interactor.CatalogInteractor
import com.example.hazardreporting.domain.interactor.IncidentInteractor
import com.example.hazardreporting.domain.interactor.LoginInteractor
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.presentation.navigation.IncidentList
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.example.hazardreporting.utils.Logger
import com.example.hazardreporting.utils.MainCoroutineRule
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verifySequence
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncidentListViewModelTest {

    @Rule
    @JvmField
    val rule = MainCoroutineRule()

    private val incidentInteractor = mockk<IncidentInteractor>()
    private val catalogInteractor = mockk<CatalogInteractor>()
    private val loginInteractor = mockk<LoginInteractor>()
    private val dispatchers = mockk<CoroutineDispatchers>()
    private val logger = mockk<Logger>()
    private val incidents = MutableStateFlow<List<IncidentSummary>>(emptyList())
    private val isSessionInvalid = MutableStateFlow(false)

    @Before
    fun setUp() {
        mockkStatic(SAVED_STATE_HANDLE_KT)
        every { dispatchers.default } returns rule.testDispatcher
        every { incidentInteractor.observeMyIncidents() } returns incidents
        every { catalogInteractor.observeCategories() } returns flowOf(listOf(CATEGORY))
        every { catalogInteractor.observeSeverities() } returns flowOf(listOf(SEVERITY))
        every { catalogInteractor.observeSites() } returns flowOf(listOf(SITE))
        every { loginInteractor.observeSessionInvalid() } returns isSessionInvalid
    }

    @After
    fun tearDown() {
        unmockkStatic(SAVED_STATE_HANDLE_KT)
    }

    @Test
    fun testUiState_Incidents() = runTest {
        incidents.value = listOf(SUMMARY, SUMMARY.copy(localId = 2, syncState = SyncState.PendingCreate))

        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentListUiState(
                content = IncidentListContent.Incidents(
                    listOf(ITEM, ITEM.copy(localId = 2, isPending = true))
                ),
                isRefreshing = false,
            )
        )
    }

    @Test
    fun testUiState_IncidentsWithoutCatalog() = runTest {
        every { catalogInteractor.observeCategories() } returns flowOf(emptyList())
        every { catalogInteractor.observeSeverities() } returns flowOf(emptyList())
        every { catalogInteractor.observeSites() } returns flowOf(emptyList())
        incidents.value = listOf(SUMMARY.copy(hasStatusChangeNotice = true))

        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.content).isEqualTo(
            IncidentListContent.Incidents(
                listOf(ITEM.copy(categoryName = null, siteName = null, severity = null, hasStatusChangeNotice = true))
            )
        )
    }

    @Test
    fun testUiState_SessionInvalid() = runTest {
        incidents.value = listOf(SUMMARY)

        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.isSessionInvalid).isFalse()

        isSessionInvalid.value = true
        advanceUntilIdle()

        // The banner shows over the stored incidents, which stay
        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentListUiState(IncidentListContent.Incidents(listOf(ITEM)), isSessionInvalid = true)
        )
    }

    @Test
    fun testInit_WithoutRefreshOnOpen() = runTest {
        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)
        advanceUntilIdle()

        // Only signing in pulls; reopening the app shows what is stored (ADR-0002)
        coVerify(exactly = 0) { incidentInteractor.refreshIncidents() }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(IncidentListUiState(IncidentListContent.Empty))
    }

    @Test
    fun testInit_RefreshOnOpen_FirstLoad() = runTest {
        val refresh = CompletableDeferred<Unit>()

        coEvery { incidentInteractor.refreshIncidents() } coAnswers { refresh.await() }

        val viewModel = createViewModel(refreshOnOpen = true)
        collectUiState(viewModel)
        advanceUntilIdle()

        // Skeleton rows while the first load runs and nothing is stored
        Truth.assertThat(viewModel.uiState.value).isEqualTo(IncidentListUiState(IncidentListContent.Loading))

        incidents.value = listOf(SUMMARY)
        refresh.complete(Unit)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentListUiState(IncidentListContent.Incidents(listOf(ITEM)))
        )
    }

    @Test
    fun testInit_RefreshOnOpen_NothingReported() = runTest {
        coJustRun { incidentInteractor.refreshIncidents() }

        val viewModel = createViewModel(refreshOnOpen = true)
        collectUiState(viewModel)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value).isEqualTo(IncidentListUiState(IncidentListContent.Empty))
    }

    @Test
    fun testInit_RefreshOnOpen_NoConnection() = runTest {
        coEvery { incidentInteractor.refreshIncidents() } throws NoConnectionException()

        val viewModel = createViewModel(refreshOnOpen = true)
        collectUiState(viewModel)

        viewModel.events.test {
            advanceUntilIdle()

            // The error card replaces the skeleton; no snackbar, because nothing is shown behind it
            expectNoEvents()
        }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(IncidentListUiState(IncidentListContent.Error))
    }

    @Test
    fun testInit_RefreshOnOpen_Unauthorized() = runTest {
        coEvery { incidentInteractor.refreshIncidents() } throws UnauthorizedException()

        val viewModel = createViewModel(refreshOnOpen = true)
        collectUiState(viewModel)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value).isEqualTo(IncidentListUiState(IncidentListContent.Error))
    }

    @Test
    fun testInit_RefreshOnOpen_AlreadyStarted() = runTest {
        // Restored after process death: the opening refresh already ran
        val savedStateHandle = SavedStateHandle(mapOf(KEY_OPENING_REFRESH_STARTED to true))

        val viewModel = createViewModel(refreshOnOpen = true, savedStateHandle)
        collectUiState(viewModel)
        advanceUntilIdle()

        coVerify(exactly = 0) { incidentInteractor.refreshIncidents() }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(IncidentListUiState(IncidentListContent.Empty))
    }

    @Test
    fun testOnRetry() = runTest {
        coEvery { incidentInteractor.refreshIncidents() } throws NoConnectionException()

        val viewModel = createViewModel(refreshOnOpen = true)
        collectUiState(viewModel)
        advanceUntilIdle()

        val refresh = CompletableDeferred<Unit>()
        coEvery { incidentInteractor.refreshIncidents() } coAnswers { refresh.await() }

        viewModel.onRetry()
        advanceUntilIdle()

        // Retry is a first load again: skeleton rows instead of the error card
        Truth.assertThat(viewModel.uiState.value).isEqualTo(IncidentListUiState(IncidentListContent.Loading))

        incidents.value = listOf(SUMMARY)
        refresh.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 2) { incidentInteractor.refreshIncidents() }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentListUiState(IncidentListContent.Incidents(listOf(ITEM)))
        )
    }

    @Test
    fun testOnRefresh_Success() = runTest {
        val refresh = CompletableDeferred<Unit>()

        coEvery { incidentInteractor.refreshIncidents() } coAnswers { refresh.await() }
        incidents.value = listOf(SUMMARY)

        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)

        viewModel.onRefresh()
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentListUiState(IncidentListContent.Incidents(listOf(ITEM)), isRefreshing = true)
        )

        refresh.complete(Unit)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentListUiState(IncidentListContent.Incidents(listOf(ITEM)), isRefreshing = false)
        )
    }

    @Test
    fun testOnRefresh_NoConnectionWithIncidents() = runTest {
        coEvery { incidentInteractor.refreshIncidents() } throws NoConnectionException()
        incidents.value = listOf(SUMMARY)

        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onRefresh()
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentListEvent.RefreshFailed)
        }

        // The stored incidents stay on screen
        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentListUiState(IncidentListContent.Incidents(listOf(ITEM)), isRefreshing = false)
        )
    }

    @Test
    fun testOnRefresh_NoConnectionWithoutIncidents() = runTest {
        coEvery { incidentInteractor.refreshIncidents() } throws NoConnectionException()

        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onRefresh()
            advanceUntilIdle()

            expectNoEvents()
        }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(IncidentListUiState(IncidentListContent.Error))
    }

    @Test
    fun testOnRefresh_WhileRefreshing() = runTest {
        val refresh = CompletableDeferred<Unit>()

        coEvery { incidentInteractor.refreshIncidents() } coAnswers { refresh.await() }

        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)

        viewModel.onRefresh()
        viewModel.onRefresh()
        viewModel.onRetry()
        refresh.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { incidentInteractor.refreshIncidents() }
    }

    @Test
    fun testOnRefresh_UnexpectedException() = runTest {
        val exception = IllegalStateException()

        coEvery { incidentInteractor.refreshIncidents() } throws exception
        justRun { logger.error(any(), any(), exception) }
        incidents.value = listOf(SUMMARY)

        val viewModel = createViewModel(refreshOnOpen = false)
        collectUiState(viewModel)

        viewModel.events.test {
            viewModel.onRefresh()
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentListEvent.UnexpectedError)
        }

        verifySequence { logger.error("IncidentListViewModel", any(), exception) }

        // The pull indicator stops
        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentListUiState(IncidentListContent.Incidents(listOf(ITEM)), isRefreshing = false)
        )
    }

    private fun createViewModel(
        refreshOnOpen: Boolean,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): IncidentListViewModel {
        every { savedStateHandle.toRoute(IncidentList::class, any()) } returns IncidentList(refreshOnOpen)
        return IncidentListViewModel(
            savedStateHandle,
            incidentInteractor,
            catalogInteractor,
            loginInteractor,
            dispatchers,
            logger,
        )
    }

    /**
     * Subscribes to the state the way the screen does, so the ViewModel's flows run
     */
    private fun TestScope.collectUiState(viewModel: IncidentListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private companion object {
        const val SAVED_STATE_HANDLE_KT = "androidx.navigation.SavedStateHandleKt"
        const val KEY_OPENING_REFRESH_STARTED = "openingRefreshStarted"
        const val CREATED_AT = 1_793_015_100_000L

        val CATEGORY = Category("cat-equipment", "Equipment")
        val SEVERITY = Severity("sev-high", "High", 3)
        val SITE = Site("site-north", "North Plant", 53.5, -2.2)

        val SUMMARY = IncidentSummary(
            localId = 1,
            serverId = "inc-1",
            title = "Conveyor belt fraying",
            categoryId = "cat-equipment",
            severityId = "sev-high",
            siteId = "site-north",
            status = IncidentStatus.Open,
            createdAt = CREATED_AT,
            syncState = SyncState.Synced,
            hasStatusChangeNotice = false,
        )

        val ITEM = IncidentListItem(
            localId = 1,
            title = "Conveyor belt fraying",
            categoryName = "Equipment",
            siteName = "North Plant",
            severity = SEVERITY,
            status = IncidentStatus.Open,
            createdAt = CREATED_AT,
            isPending = false,
            hasStatusChangeNotice = false,
        )
    }
}
