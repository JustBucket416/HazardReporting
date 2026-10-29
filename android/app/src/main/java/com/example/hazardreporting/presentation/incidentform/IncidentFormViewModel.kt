package com.example.hazardreporting.presentation.incidentform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.hazardreporting.domain.exception.ValidationException
import com.example.hazardreporting.domain.interactor.CatalogInteractor
import com.example.hazardreporting.domain.interactor.IncidentInteractor
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.presentation.common.BaseViewModel
import com.example.hazardreporting.presentation.navigation.IncidentForm
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Manages the report form. Everything the technician entered, the captured position and the file the camera is
 * writing to live in [IncidentFormState], mirrored as JSON into the `SavedStateHandle`, so they survive process death.
 * Validation rules live in [IncidentInteractor]; this class only decides when their errors show:
 * after the first Save, or once a field lost focus.
 *
 * The form reports a new incident when [IncidentForm.localId] is null. Otherwise it edits that incident: it fills
 * [IncidentFormState.fields] and [IncidentFormState.initialFields] from the stored incident once, keeps its original
 * position, and saves through [IncidentInteractor.edit]. An incident that can't be edited, e.g. one deleted meanwhile,
 * closes the form
 *
 * @param savedStateHandle holds the route and the form's state
 * @property incidentInteractor used for validating and saving the report, capturing the position and managing
 * photo files
 * @param catalogInteractor used for the categories, severities and sites to choose from
 * @property dispatchers used for launching off the main thread
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val incidentInteractor: IncidentInteractor,
    catalogInteractor: CatalogInteractor,
    private val dispatchers: CoroutineDispatchers,
) : BaseViewModel() {

    private val editedLocalId = savedStateHandle.toRoute<IncidentForm>().localId
    private val isEditing = editedLocalId != null

    // The handle saves this flow's latest value whenever the app's state is saved; it mirrors form as JSON
    private val savedForm = savedStateHandle.getMutableStateFlow(KEY_FORM, "")
    private val form = MutableStateFlow(IncidentFormState.decodeOrNull(savedForm.value) ?: IncidentFormState())
    private val overlays = MutableStateFlow(Overlays())
    private val isSaving = MutableStateFlow(false)
    private val _events = Channel<IncidentFormEvent>(Channel.BUFFERED)
    private var positionJob: Job? = null

    /**
     * State of the report form
     */
    val uiState: StateFlow<IncidentFormUiState> = combine(
        form,
        catalogInteractor.observeCategories(),
        catalogInteractor.observeSeverities(),
        catalogInteractor.observeSites(),
        overlays,
    ) { form, categories, severities, sites, overlays ->
        toUiState(form, categories, severities, sites, overlays)
    }.stateIn(
        scope = viewModelScope + exceptionHandler + dispatchers.default,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = IncidentFormUiState(isEditing = isEditing, isLoading = isEditing),
    )

    /**
     * The form as the technician edits it. The text fields read their text from here rather than from [uiState]:
     * [uiState] is built on the Default dispatcher, and a text field that gets its text back after a thread hop
     * drops keystrokes typed in between
     */
    val formState: StateFlow<IncidentFormState> = form.asStateFlow()

    /**
     * One-off events of the report form
     */
    val events: Flow<IncidentFormEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            form.collect { state -> savedForm.update { state.encode() } }
        }
        if (editedLocalId != null) {
            // Read once; a form restored after process death keeps what the technician changed since
            if (!form.value.isPrefilled) prefill(editedLocalId)
        } else if (form.value.positionStatus == PositionStatus.Locating) {
            // A new report captures the position once when the form opens. A form restored while locating asks
            // again; with permission already granted, that only captures a new fix
            _events.trySend(IncidentFormEvent.RequestLocationPermission)
        }
    }

    /**
     * Updates the title field
     */
    fun onTitleChange(title: String) {
        updateFields { it.copy(title = title) }
    }

    /**
     * Updates the description field
     */
    fun onDescriptionChange(description: String) {
        updateFields { it.copy(description = description) }
    }

    /**
     * Chooses the category with [categoryId]
     */
    fun onCategorySelect(categoryId: String) {
        updateFields { it.copy(categoryId = categoryId) }
    }

    /**
     * Chooses the severity with [severityId]
     */
    fun onSeveritySelect(severityId: String) {
        updateFields { it.copy(severityId = severityId) }
    }

    /**
     * Chooses the site with [siteId]
     */
    fun onSiteSelect(siteId: String) {
        updateFields { it.copy(siteId = siteId) }
    }

    /**
     * Updates the location description field
     */
    fun onLocationDescriptionChange(locationDescription: String) {
        updateFields { it.copy(locationDescription = locationDescription) }
    }

    /**
     * Marks [field] as touched after it lost focus, so its error shows from now on
     */
    fun onFieldBlur(field: IncidentDraft.Field) {
        form.update { it.copy(touchedFields = it.touchedFields + field) }
    }

    /**
     * Opens the sheet that offers Camera and Gallery
     */
    fun onAddPhotoClick() {
        overlays.update { it.copy(isPhotoSourceSheetVisible = true) }
    }

    /**
     * Closes the photo source sheet without choosing a source
     */
    fun onPhotoSourceDismiss() {
        overlays.update { it.copy(isPhotoSourceSheetVisible = false) }
    }

    /**
     * Prepares a file for a new photo and asks the screen to open the camera on it. The file's path is saved
     * first, so the photo can still be attached if the app is killed while the camera is open
     */
    fun onCameraSelect() {
        overlays.update { it.copy(isPhotoSourceSheetVisible = false) }
        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            val photo = incidentInteractor.createPhotoFile()
            form.update { it.copy(pendingCameraPhotoPath = photo.path) }
            _events.send(IncidentFormEvent.TakePhoto(photo.path))
        }
    }

    /**
     * Attaches the photo the camera took, or deletes its file if no picture was taken
     *
     * @param isTaken whether the camera saved a picture
     */
    fun onPhotoTaken(isTaken: Boolean) {
        val path = form.getAndUpdate { it.copy(pendingCameraPhotoPath = null) }.pendingCameraPhotoPath ?: return
        if (isTaken) attachPhoto(Photo.Local(path)) else discardPhotos(listOf(path))
    }

    /**
     * Asks the screen to open the photo picker
     */
    fun onGallerySelect() {
        overlays.update { it.copy(isPhotoSourceSheetVisible = false) }
        _events.trySend(IncidentFormEvent.PickPhoto)
    }

    /**
     * Copies the picture the technician picked into the app's photo storage straight away and attaches the copy
     *
     * @param sourceUri URI of the picked picture, or null if nothing was picked
     */
    fun onPhotoPicked(sourceUri: String?) {
        if (sourceUri == null) return
        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            attachPhoto(incidentInteractor.importPhoto(sourceUri))
        }
    }

    /**
     * Detaches [photo]. A file this form created is deleted at once
     */
    fun onRemovePhoto(photo: Photo) {
        val formPhoto = FormPhoto.of(photo)
        val path = (formPhoto as? FormPhoto.Local)?.path
        val previous = form.getAndUpdate {
            it.copy(
                fields = it.fields.copy(photos = it.fields.photos - formPhoto),
                addedPhotoPaths = it.addedPhotoPaths.filterNot { added -> added == path },
            )
        }
        if (path != null && path in previous.addedPhotoPaths) discardPhotos(listOf(path))
    }

    /**
     * Captures the position once location permission was answered. Approximate permission is enough;
     * without any, the report is saved without a position
     *
     * @param isGranted whether fine or coarse location permission is granted
     */
    fun onLocationPermissionResult(isGranted: Boolean) {
        if (positionJob?.isActive == true) return
        if (!isGranted) {
            form.update { it.copy(position = null, positionStatus = PositionStatus.Unavailable) }
            return
        }
        form.update { it.copy(positionStatus = PositionStatus.Locating) }
        positionJob = viewModelScope.launch(exceptionHandler + dispatchers.default) {
            val position = incidentInteractor.capturePosition()
            form.update {
                it.copy(
                    position = position?.let { captured -> FormPosition.of(captured) },
                    positionStatus = if (position != null) PositionStatus.Captured else PositionStatus.Unavailable,
                )
            }
        }
    }

    /**
     * Captures the position again; the new result replaces the old one. Ignored while capturing
     */
    fun onRecapturePosition() {
        if (positionJob?.isActive == true) return
        form.update { it.copy(positionStatus = PositionStatus.Locating) }
        _events.trySend(IncidentFormEvent.RequestLocationPermission)
    }

    /**
     * Saves the report, or the edit, on the device at once and leaves the form. If the draft is invalid, nothing is
     * saved and every error shows. Ignored while a save is in flight, and while an edit form is still reading
     * the incident
     */
    fun onSave() {
        if (isEditing && !form.value.isPrefilled) return
        val draft = form.updateAndGet { it.copy(isSaveAttempted = true) }.toDraft()
        if (isSaving.getAndUpdate { true }) return

        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            try {
                if (editedLocalId != null) {
                    incidentInteractor.edit(editedLocalId, draft)
                } else {
                    incidentInteractor.report(draft)
                }
                // Stays saving while the screen leaves, so a second tap can't save twice
                _events.send(IncidentFormEvent.Saved)
            } catch (e: ValidationException) {
                // The errors already show, since the save was attempted
                isSaving.update { false }
            }
        }
    }

    /**
     * Leaves the form on Back or ✕: at once if nothing changed, otherwise after "Discard changes?"
     */
    fun onCloseRequest() {
        if (form.value.hasUnsavedChanges) {
            overlays.update { it.copy(isDiscardDialogVisible = true) }
        } else {
            _events.trySend(IncidentFormEvent.Closed)
        }
    }

    /**
     * Discards the changes: deletes the photo files this form created and leaves the form
     */
    fun onDiscardConfirm() {
        overlays.update { it.copy(isDiscardDialogVisible = false) }
        val addedPhotoPaths = form.value.addedPhotoPaths
        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            addedPhotoPaths.forEach { incidentInteractor.discardPhoto(Photo.Local(it)) }
            _events.send(IncidentFormEvent.Closed)
        }
    }

    /**
     * Closes "Discard changes?" and keeps editing
     */
    fun onDiscardDismiss() {
        overlays.update { it.copy(isDiscardDialogVisible = false) }
    }

    override fun onUnexpected(throwable: Throwable) {
        isSaving.update { false }
        // A failed capture leaves the report without a position rather than locating forever
        if (positionJob?.isActive != true) {
            form.update {
                if (it.positionStatus == PositionStatus.Locating) {
                    it.copy(positionStatus = PositionStatus.Unavailable)
                } else {
                    it
                }
            }
        }
        _events.trySend(IncidentFormEvent.UnexpectedError)
    }

    private fun prefill(localId: Long) {
        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            val draft = incidentInteractor.getDraft(localId)
            if (draft == null) {
                _events.send(IncidentFormEvent.Closed)
                return@launch
            }
            val fields = IncidentFormFields(
                title = draft.title,
                description = draft.description,
                categoryId = draft.categoryId,
                severityId = draft.severityId,
                siteId = draft.siteId,
                locationDescription = draft.locationDescription,
                photos = draft.photos.map { FormPhoto.of(it) },
            )
            val position = draft.position?.let { FormPosition.of(it) }
            form.update {
                it.copy(
                    fields = fields,
                    initialFields = fields,
                    // The original position is kept; an edit never captures a new one
                    position = position,
                    positionStatus = if (position != null) PositionStatus.Captured else PositionStatus.Unavailable,
                    isPrefilled = true,
                )
            }
        }
    }

    private fun updateFields(transform: (IncidentFormFields) -> IncidentFormFields) {
        form.update { it.copy(fields = transform(it.fields)) }
    }

    private fun attachPhoto(photo: Photo.Local) {
        val formPhoto = FormPhoto.Local(photo.path)
        val updated = form.updateAndGet {
            if (it.fields.photos.size < IncidentDraft.MAX_PHOTOS) {
                it.copy(
                    fields = it.fields.copy(photos = it.fields.photos + formPhoto),
                    addedPhotoPaths = it.addedPhotoPaths + photo.path,
                )
            } else {
                it
            }
        }
        // No room left, e.g. after two photos arrived at once: the extra file isn't kept
        if (formPhoto !in updated.fields.photos) discardPhotos(listOf(photo.path))
    }

    private fun discardPhotos(paths: List<String>) {
        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            paths.forEach { incidentInteractor.discardPhoto(Photo.Local(it)) }
        }
    }

    private fun toUiState(
        form: IncidentFormState,
        categories: List<Category>,
        severities: List<Severity>,
        sites: List<Site>,
        overlays: Overlays,
    ): IncidentFormUiState {
        val invalidFields = incidentInteractor.validate(form.toDraft())
        val shownFields = if (form.isSaveAttempted) invalidFields else invalidFields intersect form.touchedFields
        return IncidentFormUiState(
            isEditing = isEditing,
            isLoading = isEditing && !form.isPrefilled,
            title = form.fields.title,
            description = form.fields.description,
            categoryId = form.fields.categoryId,
            severityId = form.fields.severityId,
            siteId = form.fields.siteId,
            locationDescription = form.fields.locationDescription,
            photos = form.fields.photos.map { it.toPhoto() },
            categories = categories,
            severities = severities,
            sites = sites,
            position = form.position?.toPosition(),
            positionStatus = form.positionStatus,
            errors = shownFields,
            isPhotoSourceSheetVisible = overlays.isPhotoSourceSheetVisible,
            isDiscardDialogVisible = overlays.isDiscardDialogVisible,
        )
    }

    /**
     * What shows over the form; not worth keeping across process death
     *
     * @property isPhotoSourceSheetVisible whether the sheet that offers Camera and Gallery shows
     * @property isDiscardDialogVisible whether the "Discard changes?" dialog shows
     */
    private data class Overlays(
        val isPhotoSourceSheetVisible: Boolean = false,
        val isDiscardDialogVisible: Boolean = false,
    )

    private companion object {
        const val KEY_FORM = "form"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
