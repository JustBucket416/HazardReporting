package com.example.hazardreporting.presentation.incidentdetail

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.interactor.CatalogInteractor
import com.example.hazardreporting.domain.interactor.IncidentInteractor
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDetails
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.presentation.navigation.IncidentDetail
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.example.hazardreporting.utils.Logger
import com.example.hazardreporting.utils.MainCoroutineRule
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
class IncidentDetailViewModelTest {

    @Rule
    @JvmField
    val rule = MainCoroutineRule()

    private val savedStateHandle = SavedStateHandle()
    private val incidentInteractor = mockk<IncidentInteractor>()
    private val catalogInteractor = mockk<CatalogInteractor>()
    private val dispatchers = mockk<CoroutineDispatchers>()
    private val logger = mockk<Logger>()
    private val incident = MutableStateFlow(Incident(SUMMARY, details = null))

    @Before
    fun setUp() {
        mockkStatic(SAVED_STATE_HANDLE_KT)
        every { savedStateHandle.toRoute(IncidentDetail::class, any()) } returns IncidentDetail(LOCAL_ID)
        every { dispatchers.default } returns rule.testDispatcher
        every { incidentInteractor.observeIncident(LOCAL_ID) } returns incident
        every { catalogInteractor.observeCategories() } returns flowOf(listOf(CATEGORY))
        every { catalogInteractor.observeSeverities() } returns flowOf(listOf(SEVERITY))
        every { catalogInteractor.observeSites() } returns flowOf(listOf(SITE))
    }

    @After
    fun tearDown() {
        unmockkStatic(SAVED_STATE_HANDLE_KT)
    }

    @Test
    fun testInit_LoadsDetailsOnce() = runTest {
        val load = CompletableDeferred<Unit>()

        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) } coAnswers { load.await() }

        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        // The summary shows right away; the details shimmer while they download
        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentDetailUiState(incident = CONTENT.copy(details = IncidentDetailsState.Loading))
        )
        Truth.assertThat(viewModel.uiState.value.isMenuVisible).isFalse()

        incident.value = Incident(SUMMARY, DETAILS)
        load.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { incidentInteractor.loadIncidentDetails(LOCAL_ID, any()) }
        coVerify(exactly = 1) { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentDetailUiState(incident = CONTENT.copy(details = IncidentDetailsState.Loaded(DETAILS)))
        )
    }

    @Test
    fun testInit_DetailsNotDownloaded() = runTest {
        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) } throws NoConnectionException()

        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.events.test {
            advanceUntilIdle()

            // No snackbar: "Details not downloaded" already says it
            expectNoEvents()
        }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentDetailUiState(incident = CONTENT.copy(details = IncidentDetailsState.NotDownloaded))
        )
        Truth.assertThat(viewModel.uiState.value.isMenuVisible).isFalse()
    }

    @Test
    fun testInit_Unauthorized() = runTest {
        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) } throws UnauthorizedException()

        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.incident?.details).isEqualTo(IncidentDetailsState.NotDownloaded)
    }

    @Test
    fun testOnRetry() = runTest {
        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) } throws NoConnectionException()

        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) } coAnswers {
            incident.value = Incident(SUMMARY, DETAILS)
        }

        viewModel.onRetry()
        advanceUntilIdle()

        coVerify(exactly = 2) { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) }

        Truth.assertThat(viewModel.uiState.value.incident?.details).isEqualTo(IncidentDetailsState.Loaded(DETAILS))
    }

    @Test
    fun testOnRefresh_Forced() = runTest {
        val refresh = CompletableDeferred<Unit>()

        incident.value = Incident(SUMMARY, DETAILS)
        coJustRun { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) }
        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = true) } coAnswers { refresh.await() }

        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        viewModel.onRefresh()
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.isRefreshing).isTrue()

        refresh.complete(Unit)
        advanceUntilIdle()

        // Pull-to-refresh downloads the details again even though they are stored
        coVerifyOrder {
            incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false)
            incidentInteractor.loadIncidentDetails(LOCAL_ID, force = true)
        }
        coVerify(exactly = 2) { incidentInteractor.loadIncidentDetails(LOCAL_ID, any()) }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentDetailUiState(incident = CONTENT.copy(details = IncidentDetailsState.Loaded(DETAILS)))
        )
    }

    @Test
    fun testOnRefresh_NoConnectionWithDetails() = runTest {
        incident.value = Incident(SUMMARY, DETAILS)
        coJustRun { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) }
        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = true) } throws NoConnectionException()

        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onRefresh()
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentDetailEvent.RefreshFailed)
        }

        // The stored details stay
        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentDetailUiState(incident = CONTENT.copy(details = IncidentDetailsState.Loaded(DETAILS)))
        )
    }

    @Test
    fun testOnRefresh_WhileLoading() = runTest {
        val load = CompletableDeferred<Unit>()

        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, any()) } coAnswers { load.await() }

        val viewModel = createViewModel()
        viewModel.onRefresh()
        viewModel.onRetry()
        load.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { incidentInteractor.loadIncidentDetails(LOCAL_ID, any()) }
    }

    @Test
    fun testUiState_PendingWithoutCatalog() = runTest {
        every { catalogInteractor.observeCategories() } returns flowOf(emptyList())
        every { catalogInteractor.observeSeverities() } returns flowOf(emptyList())
        every { catalogInteractor.observeSites() } returns flowOf(emptyList())
        incident.value = Incident(SUMMARY.copy(syncState = SyncState.PendingUpdate), DETAILS)
        coJustRun { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) }

        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.incident).isEqualTo(
            CONTENT.copy(
                severity = null,
                categoryName = null,
                siteName = null,
                isPending = true,
                details = IncidentDetailsState.Loaded(DETAILS),
            )
        )
    }

    @Test
    fun testIsMenuVisible() {
        val loaded = CONTENT.copy(details = IncidentDetailsState.Loaded(DETAILS))

        // Only an Open incident whose details are downloaded may be edited or withdrawn
        Truth.assertThat(IncidentDetailUiState(incident = loaded).isMenuVisible).isTrue()
        Truth.assertThat(
            IncidentDetailUiState(incident = loaded.copy(status = IncidentStatus.Acknowledged)).isMenuVisible
        ).isFalse()
        Truth.assertThat(
            IncidentDetailUiState(incident = CONTENT.copy(details = IncidentDetailsState.Loading)).isMenuVisible
        ).isFalse()
        Truth.assertThat(
            IncidentDetailUiState(incident = CONTENT.copy(details = IncidentDetailsState.NotDownloaded)).isMenuVisible
        ).isFalse()
        Truth.assertThat(IncidentDetailUiState(incident = null).isMenuVisible).isFalse()
    }

    @Test
    fun testOnRefresh_UnexpectedException() = runTest {
        val exception = IllegalStateException()

        coEvery { incidentInteractor.loadIncidentDetails(LOCAL_ID, force = false) } throws exception
        justRun { logger.error(any(), any(), exception) }

        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.events.test {
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentDetailEvent.UnexpectedError)
        }

        verifySequence { logger.error("IncidentDetailViewModel", any(), exception) }

        // Loading stops and Retry is offered
        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentDetailUiState(incident = CONTENT.copy(details = IncidentDetailsState.NotDownloaded))
        )
    }

    private fun createViewModel() =
        IncidentDetailViewModel(savedStateHandle, incidentInteractor, catalogInteractor, dispatchers, logger)

    /**
     * Subscribes to the state the way the screen does, so the ViewModel's flows run
     */
    private fun TestScope.collectUiState(viewModel: IncidentDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private companion object {
        const val SAVED_STATE_HANDLE_KT = "androidx.navigation.SavedStateHandleKt"
        const val LOCAL_ID = 7L
        const val CREATED_AT = 1_793_015_100_000L

        val CATEGORY = Category("cat-equipment", "Equipment")
        val SEVERITY = Severity("sev-high", "High", 3)
        val SITE = Site("site-north", "North Plant", 53.5, -2.2)

        val SUMMARY = IncidentSummary(
            localId = LOCAL_ID,
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

        val DETAILS = IncidentDetails(
            description = "Fraying near the transfer point",
            locationDescription = "Line 3",
            position = Position(53.5, -2.2, 10.0),
            photos = listOf(Photo.Remote("http://10.0.2.2:3001/uploads/a.jpg")),
            priority = null,
        )

        val CONTENT = IncidentDetailContent(
            title = "Conveyor belt fraying",
            status = IncidentStatus.Open,
            severity = SEVERITY,
            categoryName = "Equipment",
            siteName = "North Plant",
            createdAt = CREATED_AT,
            isPending = false,
            details = IncidentDetailsState.Loading,
        )
    }
}
