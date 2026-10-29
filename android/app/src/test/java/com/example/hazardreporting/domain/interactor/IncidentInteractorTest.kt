package com.example.hazardreporting.domain.interactor

import app.cash.turbine.test
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.ValidationException
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.domain.repository.CatalogRepository
import com.example.hazardreporting.domain.repository.IncidentRepository
import com.example.hazardreporting.domain.repository.LocationProvider
import com.example.hazardreporting.domain.repository.SyncScheduler
import com.google.common.truth.Truth
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentInteractorTest {

    private val incidentRepository = mockk<IncidentRepository>()
    private val catalogRepository = mockk<CatalogRepository>()
    private val locationProvider = mockk<LocationProvider>()
    private val syncScheduler = mockk<SyncScheduler>()

    private val interactor = IncidentInteractor(incidentRepository, catalogRepository, locationProvider, syncScheduler)

    @Test
    fun testObserveMyIncidents() = runTest {
        val older = summary(localId = 1, createdAt = 100)
        val newest = summary(localId = 2, createdAt = 300)
        val pendingCreate = summary(localId = 3, createdAt = 200, syncState = SyncState.PendingCreate)
        val withdrawn = summary(localId = 4, createdAt = 400, status = IncidentStatus.Withdrawn)
        val pendingWithdraw = summary(localId = 5, createdAt = 500, syncState = SyncState.PendingWithdraw)

        every { incidentRepository.observeSummaries() } returns flowOf(
            listOf(older, withdrawn, newest, pendingWithdraw, pendingCreate)
        )

        interactor.observeMyIncidents().test {
            // Withdrawn and withdrawing incidents are left out; the rest are newest first
            Truth.assertThat(awaitItem()).containsExactly(newest, pendingCreate, older).inOrder()
            awaitComplete()
        }

        verifySequence { incidentRepository.observeSummaries() }
    }

    @Test
    fun testRefreshIncidents() = runTest {
        coJustRun { catalogRepository.refresh() }
        coJustRun { incidentRepository.pullSummaries() }

        interactor.refreshIncidents()

        // The catalog comes first, so the pulled incidents can be named
        coVerifySequence {
            catalogRepository.refresh()
            incidentRepository.pullSummaries()
        }
    }

    @Test
    fun testRefreshIncidents_NoConnection() {
        coEvery { catalogRepository.refresh() } throws NoConnectionException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { interactor.refreshIncidents() }
        }

        coVerifySequence { catalogRepository.refresh() }
    }

    @Test
    fun testObserveIncident() = runTest {
        val incident = mockk<Incident>()

        every { incidentRepository.observeIncident(7L) } returns flowOf(incident)

        interactor.observeIncident(7L).test {
            Truth.assertThat(awaitItem()).isSameInstanceAs(incident)
            awaitComplete()
        }

        verifySequence { incidentRepository.observeIncident(7L) }
    }

    @Test
    fun testLoadIncidentDetails() = runTest {
        coJustRun { incidentRepository.loadDetails(7L, force = true) }

        interactor.loadIncidentDetails(7L, force = true)

        coVerifySequence { incidentRepository.loadDetails(7L, force = true) }
    }

    @Test
    fun testValidate_ValidDraft() {
        Truth.assertThat(interactor.validate(VALID_DRAFT)).isEmpty()
    }

    @Test
    fun testValidate_EmptyDraft() {
        val draft = IncidentDraft(
            title = "",
            description = "",
            categoryId = null,
            severityId = null,
            siteId = null,
            locationDescription = "",
            position = null,
            photos = emptyList(),
        )

        Truth.assertThat(interactor.validate(draft)).containsExactly(
            IncidentDraft.Field.Title,
            IncidentDraft.Field.Category,
            IncidentDraft.Field.Severity,
            IncidentDraft.Field.Site,
            IncidentDraft.Field.Photos,
        )
    }

    @Test
    fun testValidate_BlankTitle() {
        // Whitespace alone doesn't make a title
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(title = "   ")))
            .containsExactly(IncidentDraft.Field.Title)
    }

    @Test
    fun testValidate_TitleLength() {
        val longest = "a".repeat(IncidentDraft.MAX_TITLE_LENGTH)

        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(title = "a"))).isEmpty()
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(title = longest))).isEmpty()
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(title = "${longest}a")))
            .containsExactly(IncidentDraft.Field.Title)
    }

    @Test
    fun testValidate_TitleTrimmedBeforeCounting() {
        val longest = "a".repeat(IncidentDraft.MAX_TITLE_LENGTH)

        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(title = "  $longest  "))).isEmpty()
    }

    @Test
    fun testValidate_DescriptionLength() {
        val longest = "d".repeat(IncidentDraft.MAX_DESCRIPTION_LENGTH)

        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(description = ""))).isEmpty()
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(description = longest))).isEmpty()
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(description = " $longest\n"))).isEmpty()
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(description = "${longest}d")))
            .containsExactly(IncidentDraft.Field.Description)
    }

    @Test
    fun testValidate_MissingCategory() {
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(categoryId = null)))
            .containsExactly(IncidentDraft.Field.Category)
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(categoryId = " ")))
            .containsExactly(IncidentDraft.Field.Category)
    }

    @Test
    fun testValidate_MissingSeverity() {
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(severityId = null)))
            .containsExactly(IncidentDraft.Field.Severity)
    }

    @Test
    fun testValidate_MissingSite() {
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(siteId = null)))
            .containsExactly(IncidentDraft.Field.Site)
    }

    @Test
    fun testValidate_PhotoCount() {
        val photos = List(IncidentDraft.MAX_PHOTOS + 1) { Photo.Local("/files/photos/$it.jpg") }

        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(photos = emptyList())))
            .containsExactly(IncidentDraft.Field.Photos)
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(photos = photos.take(1)))).isEmpty()
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(photos = photos.take(IncidentDraft.MAX_PHOTOS))))
            .isEmpty()
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(photos = photos)))
            .containsExactly(IncidentDraft.Field.Photos)
    }

    @Test
    fun testValidate_PositionOptional() {
        Truth.assertThat(interactor.validate(VALID_DRAFT.copy(position = null))).isEmpty()
    }

    @Test
    fun testReport() = runTest {
        val draft = VALID_DRAFT.copy(
            title = "  Conveyor belt fraying ",
            description = " Near the transfer point\n",
            locationDescription = " Line 3 ",
        )
        val stored = VALID_DRAFT.copy(
            title = "Conveyor belt fraying",
            description = "Near the transfer point",
            locationDescription = "Line 3",
        )

        coEvery { incidentRepository.report(stored) } returns 9L
        justRun { syncScheduler.schedulePush() }

        val localId = interactor.report(draft)

        // Stored trimmed, then a push is scheduled; nothing waits for the network
        coVerifySequence {
            incidentRepository.report(stored)
            syncScheduler.schedulePush()
        }

        Truth.assertThat(localId).isEqualTo(9L)
    }

    @Test
    fun testReport_Invalid() {
        val draft = VALID_DRAFT.copy(siteId = null, photos = emptyList())

        val thrown = Assert.assertThrows(ValidationException::class.java) {
            runTest { interactor.report(draft) }
        }

        // Nothing is stored and no push is scheduled
        verify { incidentRepository wasNot Called }
        verify { syncScheduler wasNot Called }

        Truth.assertThat(thrown.fields).containsExactly(IncidentDraft.Field.Site, IncidentDraft.Field.Photos)
    }

    @Test
    fun testCapturePosition() = runTest {
        coEvery { locationProvider.currentPosition() } returns POSITION

        Truth.assertThat(interactor.capturePosition()).isEqualTo(POSITION)

        coVerifySequence { locationProvider.currentPosition() }
    }

    @Test
    fun testCapturePosition_Unavailable() = runTest {
        coEvery { locationProvider.currentPosition() } returns null

        Truth.assertThat(interactor.capturePosition()).isNull()
    }

    @Test
    fun testCreatePhotoFile() = runTest {
        coEvery { incidentRepository.createPhotoFile() } returns PHOTO

        Truth.assertThat(interactor.createPhotoFile()).isEqualTo(PHOTO)

        coVerifySequence { incidentRepository.createPhotoFile() }
    }

    @Test
    fun testImportPhoto() = runTest {
        coEvery { incidentRepository.importPhoto(SOURCE_URI) } returns PHOTO

        Truth.assertThat(interactor.importPhoto(SOURCE_URI)).isEqualTo(PHOTO)

        coVerifySequence { incidentRepository.importPhoto(SOURCE_URI) }
    }

    @Test
    fun testDiscardPhoto() = runTest {
        coJustRun { incidentRepository.deletePhoto(PHOTO) }

        interactor.discardPhoto(PHOTO)

        coVerifySequence { incidentRepository.deletePhoto(PHOTO) }
    }

    private fun summary(
        localId: Long,
        createdAt: Long,
        status: IncidentStatus = IncidentStatus.Open,
        syncState: SyncState = SyncState.Synced,
    ) = IncidentSummary(
        localId = localId,
        serverId = "inc-$localId",
        title = "Incident $localId",
        categoryId = "cat-equipment",
        severityId = "sev-high",
        siteId = "site-north",
        status = status,
        createdAt = createdAt,
        syncState = syncState,
        hasStatusChangeNotice = false,
    )

    private companion object {
        const val SOURCE_URI = "content://media/picker/0/42"
        val PHOTO = Photo.Local("/files/photos/a.jpg")
        val POSITION = Position(53.5, -2.2, 10.0)

        val VALID_DRAFT = IncidentDraft(
            title = "Conveyor belt fraying",
            description = "",
            categoryId = "cat-equipment",
            severityId = "sev-high",
            siteId = "site-north",
            locationDescription = "",
            position = POSITION,
            photos = listOf(PHOTO),
        )
    }
}
