package com.example.hazardreporting.presentation.incidentform

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.hazardreporting.domain.exception.PhotoTooLargeException
import com.example.hazardreporting.domain.exception.ValidationException
import com.example.hazardreporting.domain.interactor.CatalogInteractor
import com.example.hazardreporting.domain.interactor.IncidentInteractor
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.presentation.navigation.IncidentForm
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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verifySequence
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class IncidentFormViewModelTest {

    @Rule
    @JvmField
    val rule = MainCoroutineRule()

    private val savedStateHandle = SavedStateHandle()
    private val incidentInteractor = mockk<IncidentInteractor>()
    private val catalogInteractor = mockk<CatalogInteractor>()
    private val dispatchers = mockk<CoroutineDispatchers>()

    @Before
    fun setUp() {
        mockkObject(Logger)
        mockkStatic(SAVED_STATE_HANDLE_KT)
        every { dispatchers.default } returns rule.testDispatcher
        every { catalogInteractor.observeCategories() } returns flowOf(listOf(CATEGORY))
        every { catalogInteractor.observeSeverities() } returns flowOf(listOf(SEVERITY))
        every { catalogInteractor.observeSites() } returns flowOf(listOf(SITE))
        every { incidentInteractor.validate(any()) } returns emptySet()
        coJustRun { incidentInteractor.discardPhoto(any()) }
        coJustRun { incidentInteractor.checkPhoto(any()) }
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
        unmockkStatic(SAVED_STATE_HANDLE_KT)
    }

    @Test
    fun testInit_NewReport() = runTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.events.test {
            // The position is captured once when the form opens, after asking for permission
            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.RequestLocationPermission)
        }
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentFormUiState(
                categories = listOf(CATEGORY),
                severities = listOf(SEVERITY),
                sites = listOf(SITE),
                positionStatus = PositionStatus.Locating,
            )
        )
        Truth.assertThat(viewModel.uiState.value.canAddPhoto).isTrue()
    }

    @Test
    fun testInit_Editing() = runTest {
        coEvery { incidentInteractor.getDraft(7L) } returns STORED_DRAFT

        val viewModel = createViewModel(localId = 7L)
        collectUiState(viewModel)

        viewModel.events.test {
            advanceUntilIdle()

            // An edit keeps the incident's original position, so it never asks for location
            expectNoEvents()
        }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            IncidentFormUiState(
                isEditing = true,
                isLoading = false,
                title = STORED_DRAFT.title,
                description = STORED_DRAFT.description,
                categoryId = CATEGORY.id,
                severityId = SEVERITY.id,
                siteId = SITE.id,
                locationDescription = STORED_DRAFT.locationDescription,
                photos = STORED_DRAFT.photos,
                categories = listOf(CATEGORY),
                severities = listOf(SEVERITY),
                sites = listOf(SITE),
                position = POSITION,
                positionStatus = PositionStatus.Captured,
            )
        )
    }

    @Test
    fun testInit_EditingWithoutPosition() = runTest {
        coEvery { incidentInteractor.getDraft(7L) } returns STORED_DRAFT.copy(position = null)

        val viewModel = createViewModel(localId = 7L)
        collectUiState(viewModel)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.position).isNull()
        Truth.assertThat(viewModel.uiState.value.positionStatus).isEqualTo(PositionStatus.Unavailable)
    }

    @Test
    fun testInit_EditingWhileReading() = runTest {
        val draft = CompletableDeferred<IncidentDraft?>()

        coEvery { incidentInteractor.getDraft(7L) } coAnswers { draft.await() }

        val viewModel = createViewModel(localId = 7L)
        collectUiState(viewModel)
        advanceUntilIdle()

        // Nothing to show or save until the incident is read
        Truth.assertThat(viewModel.uiState.value.isLoading).isTrue()
        viewModel.onSave()
        advanceUntilIdle()
        coVerify(exactly = 0) { incidentInteractor.edit(any(), any()) }

        draft.complete(STORED_DRAFT)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.isLoading).isFalse()
        Truth.assertThat(viewModel.uiState.value.errors).isEmpty()
    }

    @Test
    fun testInit_EditingNotEditable() = runTest {
        // E.g. withdrawn elsewhere, or its details were never downloaded
        coEvery { incidentInteractor.getDraft(7L) } returns null

        val viewModel = createViewModel(localId = 7L)

        viewModel.events.test {
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.Closed)
        }
    }

    @Test
    fun testOnSave_Editing() = runTest {
        coEvery { incidentInteractor.getDraft(7L) } returns STORED_DRAFT
        coJustRun { incidentInteractor.edit(7L, any()) }

        val viewModel = createViewModel(localId = 7L)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onTitleChange("Leaking valve, worse")
            viewModel.onRemovePhoto(REMOTE_PHOTO)
            viewModel.onSave()
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.Saved)
        }

        // Saved as an edit of the same incident, with its original position and a server photo removed
        coVerify(exactly = 1) {
            incidentInteractor.edit(
                7L,
                STORED_DRAFT.copy(title = "Leaking valve, worse", photos = listOf(PHOTO)),
            )
        }
        coVerify(exactly = 0) { incidentInteractor.report(any()) }
        // A server photo has no file of this form to delete
        coVerify(exactly = 0) { incidentInteractor.discardPhoto(any()) }
    }

    @Test
    fun testOnCloseRequest_EditingUnchanged() = runTest {
        coEvery { incidentInteractor.getDraft(7L) } returns STORED_DRAFT

        val viewModel = createViewModel(localId = 7L)
        collectUiState(viewModel)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onCloseRequest()

            // The stored values aren't unsaved changes
            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.Closed)
        }
        Truth.assertThat(viewModel.uiState.value.isDiscardDialogVisible).isFalse()
    }

    @Test
    fun testEditing_SurvivesProcessDeath() = runTest {
        coEvery { incidentInteractor.getDraft(7L) } returns STORED_DRAFT

        val viewModel = createViewModel(localId = 7L)
        advanceUntilIdle()
        viewModel.onTitleChange("Changed before the process died")
        advanceUntilIdle()

        // A new ViewModel on the same saved state, as after the process was killed
        val restored = createViewModel(localId = 7L)
        collectUiState(restored)
        advanceUntilIdle()

        // Not read again, which would undo the change
        coVerify(exactly = 1) { incidentInteractor.getDraft(7L) }
        Truth.assertThat(restored.uiState.value.title).isEqualTo("Changed before the process died")
        Truth.assertThat(restored.uiState.value.isLoading).isFalse()
    }

    @Test
    fun testOnLocationPermissionResult_Granted() = runTest {
        coEvery { incidentInteractor.capturePosition() } returns POSITION

        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onLocationPermissionResult(isGranted = true)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.position).isEqualTo(POSITION)
        Truth.assertThat(viewModel.uiState.value.positionStatus).isEqualTo(PositionStatus.Captured)
    }

    @Test
    fun testOnLocationPermissionResult_GrantedWithoutFix() = runTest {
        coEvery { incidentInteractor.capturePosition() } returns null

        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onLocationPermissionResult(isGranted = true)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.position).isNull()
        Truth.assertThat(viewModel.uiState.value.positionStatus).isEqualTo(PositionStatus.Unavailable)
    }

    @Test
    fun testOnLocationPermissionResult_Denied() = runTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onLocationPermissionResult(isGranted = false)
        advanceUntilIdle()

        coVerify(exactly = 0) { incidentInteractor.capturePosition() }
        Truth.assertThat(viewModel.uiState.value.positionStatus).isEqualTo(PositionStatus.Unavailable)

        // The report still saves, without a position
        val draft = slot<IncidentDraft>()
        coEvery { incidentInteractor.report(capture(draft)) } returns 1L
        viewModel.onSave()
        advanceUntilIdle()

        Truth.assertThat(draft.captured.position).isNull()
    }

    @Test
    fun testOnLocationPermissionResult_WhileCapturing() = runTest {
        val capture = CompletableDeferred<Position?>()

        coEvery { incidentInteractor.capturePosition() } coAnswers { capture.await() }

        val viewModel = createViewModel()
        viewModel.onLocationPermissionResult(isGranted = true)
        advanceUntilIdle()
        viewModel.onLocationPermissionResult(isGranted = true)
        viewModel.onRecapturePosition()
        capture.complete(POSITION)
        advanceUntilIdle()

        coVerify(exactly = 1) { incidentInteractor.capturePosition() }
    }

    @Test
    fun testOnRecapturePosition() = runTest {
        coEvery { incidentInteractor.capturePosition() } returns POSITION

        val viewModel = createViewModel()
        collectUiState(viewModel)
        viewModel.onLocationPermissionResult(isGranted = true)
        advanceUntilIdle()

        viewModel.events.test {
            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.RequestLocationPermission)

            viewModel.onRecapturePosition()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.RequestLocationPermission)
        }
        advanceUntilIdle()
        Truth.assertThat(viewModel.uiState.value.positionStatus).isEqualTo(PositionStatus.Locating)

        coEvery { incidentInteractor.capturePosition() } returns OTHER_POSITION
        viewModel.onLocationPermissionResult(isGranted = true)
        advanceUntilIdle()

        // The new fix replaces the old one
        Truth.assertThat(viewModel.uiState.value.position).isEqualTo(OTHER_POSITION)
    }

    @Test
    fun testFieldChanges() = runTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onTitleChange("Leaking valve")
        viewModel.onDescriptionChange("Dripping")
        viewModel.onCategorySelect(CATEGORY.id)
        viewModel.onSeveritySelect(SEVERITY.id)
        viewModel.onSiteSelect(SITE.id)
        viewModel.onLocationDescriptionChange("Pump room")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        Truth.assertThat(uiState.title).isEqualTo("Leaking valve")
        Truth.assertThat(uiState.description).isEqualTo("Dripping")
        Truth.assertThat(uiState.categoryId).isEqualTo(CATEGORY.id)
        Truth.assertThat(uiState.severityId).isEqualTo(SEVERITY.id)
        Truth.assertThat(uiState.siteId).isEqualTo(SITE.id)
        Truth.assertThat(uiState.locationDescription).isEqualTo("Pump room")
    }

    @Test
    fun testErrors_HiddenUntilBlurOrSave() = runTest {
        every { incidentInteractor.validate(any()) } returns
            setOf(IncidentDraft.Field.Title, IncidentDraft.Field.Photos)
        coEvery { incidentInteractor.report(any()) } throws
            ValidationException(setOf(IncidentDraft.Field.Title, IncidentDraft.Field.Photos))

        val viewModel = createViewModel()
        collectUiState(viewModel)
        advanceUntilIdle()

        // Nothing shows on a fresh form
        Truth.assertThat(viewModel.uiState.value.errors).isEmpty()

        viewModel.onFieldBlur(IncidentDraft.Field.Title)
        advanceUntilIdle()

        // A field that lost focus shows its own error only
        Truth.assertThat(viewModel.uiState.value.errors).containsExactly(IncidentDraft.Field.Title)

        viewModel.onSave()
        advanceUntilIdle()

        // After the first Save every error shows
        Truth.assertThat(viewModel.uiState.value.errors)
            .containsExactly(IncidentDraft.Field.Title, IncidentDraft.Field.Photos)
    }

    @Test
    fun testErrors_FollowFixes() = runTest {
        every { incidentInteractor.validate(any()) } returns setOf(IncidentDraft.Field.Site)
        coEvery { incidentInteractor.report(any()) } throws ValidationException(setOf(IncidentDraft.Field.Site))

        val viewModel = createViewModel()
        collectUiState(viewModel)
        viewModel.onSave()
        advanceUntilIdle()

        every { incidentInteractor.validate(any()) } returns emptySet()
        viewModel.onSiteSelect(SITE.id)
        advanceUntilIdle()

        // The rules are re-applied as the technician fixes the form
        Truth.assertThat(viewModel.uiState.value.errors).isEmpty()
    }

    @Test
    fun testOnSave_Invalid() = runTest {
        every { incidentInteractor.validate(any()) } returns
            setOf(IncidentDraft.Field.Site, IncidentDraft.Field.Photos)
        coEvery { incidentInteractor.report(any()) } throws
            ValidationException(setOf(IncidentDraft.Field.Site, IncidentDraft.Field.Photos))

        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.events.test {
            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.RequestLocationPermission)

            viewModel.onSave()
            advanceUntilIdle()

            // The form stays open with its errors
            expectNoEvents()
        }

        Truth.assertThat(viewModel.uiState.value.errors)
            .containsExactly(IncidentDraft.Field.Site, IncidentDraft.Field.Photos)

        // Saving again after fixing is possible
        coEvery { incidentInteractor.report(any()) } returns 1L
        viewModel.onSave()
        advanceUntilIdle()

        coVerify(exactly = 2) { incidentInteractor.report(any()) }
    }

    @Test
    fun testOnSave_Success() = runTest {
        val draft = slot<IncidentDraft>()

        coEvery { incidentInteractor.capturePosition() } returns POSITION
        coEvery { incidentInteractor.createPhotoFile() } returns PHOTO
        coEvery { incidentInteractor.report(capture(draft)) } returns 1L

        val viewModel = createViewModel()
        viewModel.events.test {
            skipPermissionRequest()
            viewModel.onLocationPermissionResult(isGranted = true)
            viewModel.onTitleChange("Leaking valve")
            viewModel.onCategorySelect(CATEGORY.id)
            viewModel.onSeveritySelect(SEVERITY.id)
            viewModel.onSiteSelect(SITE.id)
            viewModel.onCameraSelect()
            advanceUntilIdle()
            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.TakePhoto(PHOTO.path))
            viewModel.onPhotoTaken(isTaken = true)
            // Attached once its size is checked
            advanceUntilIdle()

            viewModel.onSave()
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.Saved)
        }

        Truth.assertThat(draft.captured).isEqualTo(
            IncidentDraft(
                title = "Leaking valve",
                description = "",
                categoryId = CATEGORY.id,
                severityId = SEVERITY.id,
                siteId = SITE.id,
                locationDescription = "",
                position = POSITION,
                photos = listOf(PHOTO),
            )
        )
        // A saved report keeps its photo files
        coVerify(exactly = 0) { incidentInteractor.discardPhoto(any()) }
    }

    @Test
    fun testOnSave_WhileSaving() = runTest {
        val report = CompletableDeferred<Long>()

        coEvery { incidentInteractor.report(any()) } coAnswers { report.await() }

        val viewModel = createViewModel()
        viewModel.onSave()
        advanceUntilIdle()
        viewModel.onSave()
        report.complete(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { incidentInteractor.report(any()) }
    }

    @Test
    fun testFormState_SurvivesProcessDeath() = runTest {
        coEvery { incidentInteractor.capturePosition() } returns POSITION
        coEvery { incidentInteractor.importPhoto(SOURCE_URI) } returns PHOTO

        val viewModel = createViewModel()
        viewModel.onLocationPermissionResult(isGranted = true)
        viewModel.onTitleChange("Leaking valve")
        viewModel.onSiteSelect(SITE.id)
        viewModel.onPhotoPicked(SOURCE_URI)
        advanceUntilIdle()

        // A new ViewModel on the same saved state, as after the process was killed
        val restored = createViewModel()
        collectUiState(restored)

        restored.events.test {
            advanceUntilIdle()

            // The position was captured already, so it isn't asked for again
            expectNoEvents()
        }

        val uiState = restored.uiState.value
        Truth.assertThat(uiState.title).isEqualTo("Leaking valve")
        Truth.assertThat(uiState.siteId).isEqualTo(SITE.id)
        Truth.assertThat(uiState.photos).containsExactly(PHOTO)
        Truth.assertThat(uiState.position).isEqualTo(POSITION)
    }

    @Test
    fun testOnPhotoTaken_AfterProcessDeath() = runTest {
        coEvery { incidentInteractor.createPhotoFile() } returns PHOTO

        val viewModel = createViewModel()
        viewModel.onTitleChange("Leaking valve")
        viewModel.onCameraSelect()
        advanceUntilIdle()

        // The camera returns to a new ViewModel: the pending file is still known
        val restored = createViewModel()
        collectUiState(restored)
        restored.onPhotoTaken(isTaken = true)
        advanceUntilIdle()

        Truth.assertThat(restored.uiState.value.title).isEqualTo("Leaking valve")
        Truth.assertThat(restored.uiState.value.photos).containsExactly(PHOTO)
    }

    @Test
    fun testOnPhotoTaken_Cancelled() = runTest {
        coEvery { incidentInteractor.createPhotoFile() } returns PHOTO

        val viewModel = createViewModel()
        collectUiState(viewModel)
        viewModel.onCameraSelect()
        advanceUntilIdle()

        viewModel.onPhotoTaken(isTaken = false)
        advanceUntilIdle()

        // The empty file is deleted
        coVerify { incidentInteractor.discardPhoto(PHOTO) }
        Truth.assertThat(viewModel.uiState.value.photos).isEmpty()
    }

    @Test
    fun testOnPhotoTaken_NothingPending() = runTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onPhotoTaken(isTaken = true)
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.photos).isEmpty()
    }

    @Test
    fun testPhotoSourceSheet() = runTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onAddPhotoClick()
        advanceUntilIdle()
        Truth.assertThat(viewModel.uiState.value.isPhotoSourceSheetVisible).isTrue()

        viewModel.onPhotoSourceDismiss()
        advanceUntilIdle()
        Truth.assertThat(viewModel.uiState.value.isPhotoSourceSheetVisible).isFalse()

        viewModel.onAddPhotoClick()
        viewModel.events.test {
            skipPermissionRequest()

            viewModel.onGallerySelect()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.PickPhoto)
        }
        advanceUntilIdle()
        Truth.assertThat(viewModel.uiState.value.isPhotoSourceSheetVisible).isFalse()
    }

    @Test
    fun testOnPhotoPicked() = runTest {
        coEvery { incidentInteractor.importPhoto(SOURCE_URI) } returns PHOTO

        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onPhotoPicked(null)
        viewModel.onPhotoPicked(SOURCE_URI)
        advanceUntilIdle()

        // Copied into the app's storage straight away; nothing happens when nothing was picked
        coVerify(exactly = 1) { incidentInteractor.importPhoto(any()) }
        Truth.assertThat(viewModel.uiState.value.photos).containsExactly(PHOTO)
    }

    @Test
    fun testOnPhotoPicked_TooLarge() = runTest {
        coEvery { incidentInteractor.importPhoto(SOURCE_URI) } throws PhotoTooLargeException()

        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.events.test {
            skipPermissionRequest()

            viewModel.onPhotoPicked(SOURCE_URI)
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.PhotoTooLarge)
        }
        Truth.assertThat(viewModel.uiState.value.photos).isEmpty()
    }

    @Test
    fun testOnPhotoTaken_TooLarge() = runTest {
        coEvery { incidentInteractor.createPhotoFile() } returns PHOTO
        coEvery { incidentInteractor.checkPhoto(PHOTO) } throws PhotoTooLargeException()

        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.events.test {
            skipPermissionRequest()

            viewModel.onCameraSelect()
            advanceUntilIdle()
            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.TakePhoto(PHOTO.path))

            viewModel.onPhotoTaken(isTaken = true)
            advanceUntilIdle()

            // The interactor deleted the file; the screen says why nothing was attached
            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.PhotoTooLarge)
        }
        Truth.assertThat(viewModel.uiState.value.photos).isEmpty()
    }

    @Test
    fun testOnPhotoPicked_NoRoomLeft() = runTest {
        val photos = List(IncidentDraft.MAX_PHOTOS + 1) { Photo.Local("/files/photos/$it.jpg") }
        photos.forEachIndexed { index, photo ->
            coEvery { incidentInteractor.importPhoto("content://$index") } returns photo
        }

        val viewModel = createViewModel()
        collectUiState(viewModel)
        photos.indices.forEach { viewModel.onPhotoPicked("content://$it") }
        advanceUntilIdle()

        // The extra copy isn't kept
        Truth.assertThat(viewModel.uiState.value.photos)
            .containsExactlyElementsIn(photos.take(IncidentDraft.MAX_PHOTOS))
        Truth.assertThat(viewModel.uiState.value.canAddPhoto).isFalse()
        coVerify { incidentInteractor.discardPhoto(photos.last()) }
    }

    @Test
    fun testOnRemovePhoto() = runTest {
        coEvery { incidentInteractor.importPhoto(SOURCE_URI) } returns PHOTO

        val viewModel = createViewModel()
        collectUiState(viewModel)
        viewModel.onPhotoPicked(SOURCE_URI)
        advanceUntilIdle()

        viewModel.onRemovePhoto(PHOTO)
        advanceUntilIdle()

        // The form's own file is deleted with it
        coVerify { incidentInteractor.discardPhoto(PHOTO) }
        Truth.assertThat(viewModel.uiState.value.photos).isEmpty()
    }

    @Test
    fun testFormState_TextChangesWithoutDispatching() = runTest {
        val viewModel = createViewModel()

        // Keystrokes typed faster than the dispatcher runs must all reach the text fields, in order
        viewModel.onTitleChange("B")
        viewModel.onTitleChange("Br")
        viewModel.onDescriptionChange("Rail")
        viewModel.onLocationDescriptionChange("Dock 2")

        val fields = viewModel.formState.value.fields
        Truth.assertThat(fields.title).isEqualTo("Br")
        Truth.assertThat(fields.description).isEqualTo("Rail")
        Truth.assertThat(fields.locationDescription).isEqualTo("Dock 2")
    }

    @Test
    fun testOnCloseRequest_NoChanges() = runTest {
        coEvery { incidentInteractor.capturePosition() } returns POSITION

        val viewModel = createViewModel()
        viewModel.events.test {
            skipPermissionRequest()
            // A captured position alone isn't a change to lose
            viewModel.onLocationPermissionResult(isGranted = true)
            advanceUntilIdle()

            viewModel.onCloseRequest()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.Closed)
        }
    }

    @Test
    fun testOnCloseRequest_UnsavedChanges() = runTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)
        viewModel.onTitleChange("Leaking valve")

        viewModel.events.test {
            skipPermissionRequest()

            viewModel.onCloseRequest()
            advanceUntilIdle()

            // "Discard changes?" instead of closing
            expectNoEvents()
        }
        Truth.assertThat(viewModel.uiState.value.isDiscardDialogVisible).isTrue()

        viewModel.onDiscardDismiss()
        advanceUntilIdle()

        Truth.assertThat(viewModel.uiState.value.isDiscardDialogVisible).isFalse()
        Truth.assertThat(viewModel.uiState.value.title).isEqualTo("Leaking valve")
    }

    @Test
    fun testOnCloseRequest_WhileSaving() = runTest {
        val report = CompletableDeferred<Long>()

        coEvery { incidentInteractor.report(any()) } coAnswers { report.await() }

        val viewModel = createViewModel()
        collectUiState(viewModel)
        viewModel.onTitleChange("Leaking valve")
        viewModel.onSave()
        advanceUntilIdle()

        viewModel.events.test {
            skipPermissionRequest()

            viewModel.onCloseRequest()
            advanceUntilIdle()

            // The form leaves once the save is done, not before
            expectNoEvents()
        }
        Truth.assertThat(viewModel.uiState.value.isDiscardDialogVisible).isFalse()
        report.complete(1L)
    }

    @Test
    fun testOnDiscardConfirm_WhileSaving() = runTest {
        val report = CompletableDeferred<Long>()

        coEvery { incidentInteractor.importPhoto(SOURCE_URI) } returns PHOTO
        coEvery { incidentInteractor.report(any()) } coAnswers { report.await() }

        val viewModel = createViewModel()
        collectUiState(viewModel)
        viewModel.onPhotoPicked(SOURCE_URI)
        advanceUntilIdle()
        // "Discard changes?" is already open when the save starts
        viewModel.onCloseRequest()
        viewModel.onSave()
        advanceUntilIdle()

        viewModel.onDiscardConfirm()
        report.complete(1L)
        advanceUntilIdle()

        // The save may already refer to the photo, so its file stays
        coVerify(exactly = 0) { incidentInteractor.discardPhoto(any()) }
        Truth.assertThat(viewModel.uiState.value.isDiscardDialogVisible).isFalse()
    }

    @Test
    fun testOnDiscardConfirm() = runTest {
        coEvery { incidentInteractor.importPhoto(SOURCE_URI) } returns PHOTO

        val viewModel = createViewModel()
        collectUiState(viewModel)
        viewModel.onPhotoPicked(SOURCE_URI)
        advanceUntilIdle()
        viewModel.onCloseRequest()

        viewModel.events.test {
            skipPermissionRequest()

            viewModel.onDiscardConfirm()
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.Closed)
        }

        // The photos this form created don't outlive it
        coVerify { incidentInteractor.discardPhoto(PHOTO) }
        Truth.assertThat(viewModel.uiState.value.isDiscardDialogVisible).isFalse()
    }

    @Test
    fun testOnCameraSelect_UnexpectedException() = runTest {
        val exception = IllegalStateException()

        coEvery { incidentInteractor.createPhotoFile() } throws exception
        justRun { Logger.error(any(), any(), exception) }

        val viewModel = createViewModel()
        viewModel.events.test {
            skipPermissionRequest()

            viewModel.onCameraSelect()
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.UnexpectedError)
        }

        verifySequence { Logger.error("IncidentFormViewModel", any(), exception) }
    }

    private fun createViewModel(localId: Long? = null): IncidentFormViewModel {
        every { savedStateHandle.toRoute(IncidentForm::class, any()) } returns IncidentForm(localId)
        return IncidentFormViewModel(savedStateHandle, incidentInteractor, catalogInteractor, dispatchers)
    }

    /**
     * Subscribes to the state the way the screen does, so the ViewModel's flows run
     */
    private fun TestScope.collectUiState(viewModel: IncidentFormViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    /**
     * Consumes the permission request every new report starts with
     */
    private suspend fun ReceiveTurbine<IncidentFormEvent>.skipPermissionRequest() {
        Truth.assertThat(awaitItem()).isEqualTo(IncidentFormEvent.RequestLocationPermission)
    }

    private companion object {
        const val SAVED_STATE_HANDLE_KT = "androidx.navigation.SavedStateHandleKt"
        const val SOURCE_URI = "content://media/picker/0/42"

        val CATEGORY = Category("cat-plumbing", "Plumbing")
        val SEVERITY = Severity("sev-medium", "Medium", 2)
        val SITE = Site("site-depot", "Depot A", 53.4, -2.1)
        val POSITION = Position(53.5, -2.2, 12.0)
        val OTHER_POSITION = Position(53.6, -2.3, 5.0)
        val PHOTO = Photo.Local("/files/photos/a.jpg")
        val REMOTE_PHOTO = Photo.Remote("http://10.0.2.2:3001/uploads/b.jpg")

        val STORED_DRAFT = IncidentDraft(
            title = "Leaking valve",
            description = "Dripping steadily",
            categoryId = CATEGORY.id,
            severityId = SEVERITY.id,
            siteId = SITE.id,
            locationDescription = "Pump room B2",
            position = POSITION,
            photos = listOf(REMOTE_PHOTO, PHOTO),
        )
    }
}
