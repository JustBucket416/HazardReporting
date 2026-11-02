package com.example.hazardreporting.presentation.incidentform

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hazardreporting.R
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.presentation.common.CollectEvents
import com.example.hazardreporting.presentation.common.PhotoImage
import com.example.hazardreporting.presentation.common.ShimmerDefaults
import com.example.hazardreporting.presentation.common.label
import com.example.hazardreporting.presentation.common.shimmer
import com.example.hazardreporting.presentation.theme.HazardReportingTheme
import com.example.hazardreporting.presentation.theme.extendedColors
import java.io.File
import kotlinx.coroutines.launch

/**
 * The report form bound to its [IncidentFormViewModel]. Owns the system interactions the ViewModel asks for:
 * the location permission request, the camera (through the app's `FileProvider`) and the photo picker
 *
 * @param viewModel manages the form's state
 * @param onClose called when the form closes without saving
 * @param onSaved called once the report is saved
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Composable
fun IncidentFormRoute(viewModel: IncidentFormViewModel, onClose: () -> Unit, onSaved: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val noCameraMessage = stringResource(R.string.incident_form_no_camera)
    val photoTooLargeMessage = stringResource(R.string.incident_form_photo_too_large)
    val unexpectedErrorMessage = stringResource(R.string.error_unexpected)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Approximate location is enough
        viewModel.onLocationPermissionResult(isGranted = results.values.any { it })
    }
    val requestEnableLocation = rememberEnableLocationRequest { isEnabled ->
        viewModel.onEnableLocationResult(isEnabled)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isTaken ->
        viewModel.onPhotoTaken(isTaken)
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onPhotoPicked(uri?.toString())
    }

    CollectEvents(viewModel.events) { event ->
        when (event) {
            IncidentFormEvent.RequestLocationPermission -> permissionLauncher.launch(LOCATION_PERMISSIONS)
            IncidentFormEvent.RequestEnableLocation -> requestEnableLocation()
            is IncidentFormEvent.TakePhoto -> {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}$FILE_PROVIDER_AUTHORITY_SUFFIX",
                    File(event.path),
                )
                try {
                    cameraLauncher.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    viewModel.onPhotoTaken(isTaken = false)
                    coroutineScope.launch { snackbarHostState.showSnackbar(noCameraMessage) }
                }
            }
            IncidentFormEvent.PickPhoto -> photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            IncidentFormEvent.PhotoTooLarge -> coroutineScope.launch {
                snackbarHostState.showSnackbar(photoTooLargeMessage)
            }
            IncidentFormEvent.Saved -> onSaved()
            IncidentFormEvent.Closed -> onClose()
            IncidentFormEvent.UnexpectedError -> coroutineScope.launch {
                snackbarHostState.showSnackbar(unexpectedErrorMessage)
            }
        }
    }

    BackHandler(onBack = viewModel::onCloseRequest)

    IncidentFormScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = IncidentFormActions(
            onClose = viewModel::onCloseRequest,
            onSave = viewModel::onSave,
            onTitleChange = viewModel::onTitleChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCategorySelect = viewModel::onCategorySelect,
            onSeveritySelect = viewModel::onSeveritySelect,
            onSiteSelect = viewModel::onSiteSelect,
            onLocationDescriptionChange = viewModel::onLocationDescriptionChange,
            onFieldBlur = viewModel::onFieldBlur,
            onAddPhotoClick = viewModel::onAddPhotoClick,
            onRemovePhoto = viewModel::onRemovePhoto,
            onPhotoSourceDismiss = viewModel::onPhotoSourceDismiss,
            onCameraSelect = viewModel::onCameraSelect,
            onGallerySelect = viewModel::onGallerySelect,
            onRecapturePosition = viewModel::onRecapturePosition,
            onDiscardConfirm = viewModel::onDiscardConfirm,
            onDiscardDismiss = viewModel::onDiscardDismiss,
        ),
    )
}

/**
 * What the report form's controls do
 *
 * @property onClose called when the user taps ✕
 * @property onSave called when the user taps Save
 * @property onTitleChange called when the title text changes
 * @property onDescriptionChange called when the description text changes
 * @property onCategorySelect called with the id of the chosen category
 * @property onSeveritySelect called with the id of the chosen severity
 * @property onSiteSelect called with the id of the chosen site
 * @property onLocationDescriptionChange called when the location description text changes
 * @property onFieldBlur called when a field loses focus or a dropdown closes
 * @property onAddPhotoClick called when the user taps "Add photo"
 * @property onRemovePhoto called with the photo whose ✕ the user tapped
 * @property onPhotoSourceDismiss called when the photo source sheet closes without a choice
 * @property onCameraSelect called when the user chooses Camera
 * @property onGallerySelect called when the user chooses Gallery
 * @property onRecapturePosition called when the user taps Re-capture
 * @property onDiscardConfirm called when the user confirms discarding the changes
 * @property onDiscardDismiss called when the user keeps editing instead
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentFormActions(
    val onClose: () -> Unit,
    val onSave: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onCategorySelect: (String) -> Unit,
    val onSeveritySelect: (String) -> Unit,
    val onSiteSelect: (String) -> Unit,
    val onLocationDescriptionChange: (String) -> Unit,
    val onFieldBlur: (IncidentDraft.Field) -> Unit,
    val onAddPhotoClick: () -> Unit,
    val onRemovePhoto: (Photo) -> Unit,
    val onPhotoSourceDismiss: () -> Unit,
    val onCameraSelect: () -> Unit,
    val onGallerySelect: () -> Unit,
    val onRecapturePosition: () -> Unit,
    val onDiscardConfirm: () -> Unit,
    val onDiscardDismiss: () -> Unit,
)

/**
 * The report form: photos first, then title, description, category, severity, site, location description
 * and the captured position, which only a new report can re-capture. Field errors show as supporting text once
 * [IncidentFormUiState.errors] names them. An edit form shows its fields once it has read the incident
 *
 * @param uiState state of the form
 * @param snackbarHostState shows the form's snackbars
 * @param actions what the form's controls do
 * @param modifier modifier for the screen's root
 *
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFormScreen(
    uiState: IncidentFormUiState,
    snackbarHostState: SnackbarHostState,
    actions: IncidentFormActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val titleRes = if (uiState.isEditing) {
                        R.string.incident_form_edit_title
                    } else {
                        R.string.incident_form_new_title
                    }
                    Text(stringResource(titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close))
                    }
                },
                actions = {
                    TextButton(onClick = actions.onSave) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Reading the incident takes a moment; empty fields would flash meanwhile
            if (!uiState.isLoading) FormFields(uiState, actions)
        }
    }

    if (uiState.isPhotoSourceSheetVisible) PhotoSourceSheet(actions)
    if (uiState.isDiscardDialogVisible) DiscardDialog(actions)
}

@Composable
private fun FormFields(uiState: IncidentFormUiState, actions: IncidentFormActions) {
    PhotoStrip(
        photos = uiState.photos,
        canAddPhoto = uiState.canAddPhoto,
        error = uiState.errorFor(IncidentDraft.Field.Photos),
        onAddPhotoClick = actions.onAddPhotoClick,
        onRemovePhoto = actions.onRemovePhoto,
    )
    FormTextField(
        value = uiState.title,
        onValueChange = actions.onTitleChange,
        label = stringResource(R.string.incident_form_title),
        error = uiState.errorFor(IncidentDraft.Field.Title),
        maxLength = IncidentDraft.MAX_TITLE_LENGTH,
        onBlur = { actions.onFieldBlur(IncidentDraft.Field.Title) },
        singleLine = true,
    )
    FormTextField(
        value = uiState.description,
        onValueChange = actions.onDescriptionChange,
        label = stringResource(R.string.incident_form_description),
        error = uiState.errorFor(IncidentDraft.Field.Description),
        maxLength = IncidentDraft.MAX_DESCRIPTION_LENGTH,
        onBlur = { actions.onFieldBlur(IncidentDraft.Field.Description) },
        minLines = DESCRIPTION_MIN_LINES,
    )
    CatalogDropdown(
        label = stringResource(R.string.incident_field_category),
        options = uiState.categories.map { it.id to it.name },
        selectedId = uiState.categoryId,
        error = uiState.errorFor(IncidentDraft.Field.Category),
        onSelect = actions.onCategorySelect,
        onBlur = { actions.onFieldBlur(IncidentDraft.Field.Category) },
    )
    SeveritySelector(
        severities = uiState.severities,
        selectedId = uiState.severityId,
        error = uiState.errorFor(IncidentDraft.Field.Severity),
        onSelect = actions.onSeveritySelect,
    )
    CatalogDropdown(
        label = stringResource(R.string.incident_field_site),
        options = uiState.sites.map { it.id to it.name },
        selectedId = uiState.siteId,
        error = uiState.errorFor(IncidentDraft.Field.Site),
        onSelect = actions.onSiteSelect,
        onBlur = { actions.onFieldBlur(IncidentDraft.Field.Site) },
    )
    FormTextField(
        value = uiState.locationDescription,
        onValueChange = actions.onLocationDescriptionChange,
        label = stringResource(R.string.incident_field_location_description),
        error = null,
        maxLength = null,
        onBlur = {},
        singleLine = true,
    )
    PositionLine(
        position = uiState.position,
        status = uiState.positionStatus,
        // An edit keeps the original position
        onRecapture = if (uiState.isEditing) null else actions.onRecapturePosition,
    )
}

@Composable
private fun PhotoStrip(
    photos: List<Photo>,
    canAddPhoto: Boolean,
    error: String?,
    onAddPhotoClick: () -> Unit,
    onRemovePhoto: (Photo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            photos.forEachIndexed { index, photo ->
                Box(modifier = Modifier.size(PHOTO_TILE_SIZE)) {
                    PhotoImage(
                        photo = photo,
                        contentDescription = stringResource(
                            R.string.incident_photo_description,
                            index + 1,
                            photos.size,
                        ),
                        modifier = Modifier
                            .size(PHOTO_TILE_SIZE)
                            .clip(MaterialTheme.shapes.medium),
                    )
                    FilledTonalIconButton(
                        onClick = { onRemovePhoto(photo) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(REMOVE_BUTTON_SIZE),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.incident_form_remove_photo, index + 1),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            if (canAddPhoto) {
                OutlinedCard(onClick = onAddPhotoClick, modifier = Modifier.size(PHOTO_TILE_SIZE)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                        Text(
                            text = stringResource(R.string.incident_form_add_photo),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        if (error != null) ErrorText(error)
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    maxLength: Int?,
    onBlur: () -> Unit,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    var wasFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (it.isFocused) {
                    wasFocused = true
                } else if (wasFocused) {
                    wasFocused = false
                    onBlur()
                }
            },
        label = { Text(label) },
        supportingText = if (error != null || maxLength != null) {
            {
                Row {
                    Text(text = error.orEmpty(), modifier = Modifier.weight(1f))
                    if (maxLength != null) {
                        Text(stringResource(R.string.incident_form_character_count, value.trim().length, maxLength))
                    }
                }
            }
        } else {
            null
        },
        isError = error != null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        singleLine = singleLine,
        minLines = minLines,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    error: String?,
    onSelect: (String) -> Unit,
    onBlur: () -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val close = {
        isExpanded = false
        onBlur()
    }
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { if (it) isExpanded = true else close() },
    ) {
        OutlinedTextField(
            value = options.find { it.first == selectedId }?.second.orEmpty(),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            supportingText = error?.let { { Text(it) } },
            isError = error != null,
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = close) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        close()
                    },
                )
            }
        }
    }
}

@Composable
private fun SeveritySelector(
    severities: List<Severity>,
    selectedId: String?,
    error: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(stringResource(R.string.incident_form_severity))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            severities.forEachIndexed { index, severity ->
                val colors = MaterialTheme.extendedColors.severity.forRank(severity.rank)
                SegmentedButton(
                    selected = severity.id == selectedId,
                    onClick = { onSelect(severity.id) },
                    shape = SegmentedButtonDefaults.itemShape(index, severities.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.container,
                        activeContentColor = colors.content,
                    ),
                    label = { Text(text = severity.name, maxLines = 1) },
                )
            }
        }
        if (error != null) ErrorText(error)
    }
}

@Composable
private fun PositionLine(position: Position?, status: PositionStatus, onRecapture: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FieldLabel(stringResource(R.string.incident_field_position))
            val text = when (status) {
                PositionStatus.Locating -> stringResource(R.string.incident_form_locating)
                PositionStatus.Captured -> position?.label() ?: stringResource(R.string.incident_form_no_position)
                PositionStatus.Unavailable -> stringResource(R.string.incident_form_no_position)
                PositionStatus.LocationOff -> stringResource(R.string.incident_form_location_off)
            }
            Text(
                text = text,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .shimmer(ShimmerDefaults.highlightColor, enabled = status == PositionStatus.Locating),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (onRecapture != null) {
            TextButton(onClick = onRecapture, enabled = status != PositionStatus.Locating) {
                Text(stringResource(R.string.incident_form_recapture))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(actions: IncidentFormActions) {
    ModalBottomSheet(onDismissRequest = actions.onPhotoSourceDismiss) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.incident_form_camera)) },
            modifier = Modifier.clickable(onClick = actions.onCameraSelect),
            leadingContent = { Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.incident_form_gallery)) },
            modifier = Modifier.clickable(onClick = actions.onGallerySelect),
            leadingContent = { Icon(painterResource(R.drawable.ic_photo_library), contentDescription = null) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DiscardDialog(actions: IncidentFormActions) {
    AlertDialog(
        onDismissRequest = actions.onDiscardDismiss,
        title = { Text(stringResource(R.string.incident_form_discard_title)) },
        confirmButton = {
            TextButton(onClick = actions.onDiscardConfirm) { Text(stringResource(R.string.incident_form_discard)) }
        },
        dismissButton = {
            TextButton(onClick = actions.onDiscardDismiss) { Text(stringResource(R.string.incident_form_keep_editing)) }
        },
    )
}

@Composable
private fun FieldLabel(label: String) {
    Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorText(error: String) {
    Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun IncidentFormUiState.errorFor(field: IncidentDraft.Field): String? {
    if (field !in errors) return null
    return when (field) {
        IncidentDraft.Field.Title -> pluralStringResource(
            R.plurals.incident_form_error_title,
            IncidentDraft.MAX_TITLE_LENGTH,
            IncidentDraft.MAX_TITLE_LENGTH,
        )
        IncidentDraft.Field.Description -> pluralStringResource(
            R.plurals.incident_form_error_description,
            IncidentDraft.MAX_DESCRIPTION_LENGTH,
            IncidentDraft.MAX_DESCRIPTION_LENGTH,
        )
        IncidentDraft.Field.Category -> stringResource(R.string.incident_form_error_category)
        IncidentDraft.Field.Severity -> stringResource(R.string.incident_form_error_severity)
        IncidentDraft.Field.Site -> stringResource(R.string.incident_form_error_site)
        IncidentDraft.Field.Photos -> pluralStringResource(
            R.plurals.incident_form_error_photos,
            IncidentDraft.MAX_PHOTOS,
            IncidentDraft.MIN_PHOTOS,
            IncidentDraft.MAX_PHOTOS,
        )
    }
}

/**
 * Suffix of the app's `FileProvider` authority, as declared in the manifest
 */
private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)
private const val DESCRIPTION_MIN_LINES = 3
private val PHOTO_TILE_SIZE = 96.dp
private val REMOVE_BUTTON_SIZE = 28.dp

@Preview(name = "Filled, light")
@Preview(name = "Filled, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncidentFormScreenPreview() {
    IncidentFormScreenPreviewContent(
        PREVIEW_STATE.copy(
            title = "Conveyor belt fraying",
            categoryId = "cat-equipment",
            severityId = "sev-high",
            photos = listOf(Photo.Remote("http://10.0.2.2:3001/uploads/seed/photo-05.png")),
            position = Position(53.50534, -2.20063, 12.0),
            positionStatus = PositionStatus.Captured,
        )
    )
}

@Preview(name = "Editing")
@Composable
private fun IncidentFormScreenEditingPreview() {
    IncidentFormScreenPreviewContent(
        PREVIEW_STATE.copy(
            isEditing = true,
            title = "Conveyor belt fraying",
            categoryId = "cat-equipment",
            severityId = "sev-high",
            siteId = "site-north-plant",
            photos = listOf(Photo.Remote("http://10.0.2.2:3001/uploads/seed/photo-05.png")),
            position = Position(53.50534, -2.20063, 12.0),
            positionStatus = PositionStatus.Captured,
        )
    )
}

@Preview(name = "Errors")
@Composable
private fun IncidentFormScreenErrorsPreview() {
    IncidentFormScreenPreviewContent(
        PREVIEW_STATE.copy(
            positionStatus = PositionStatus.Unavailable,
            errors = setOf(
                IncidentDraft.Field.Title,
                IncidentDraft.Field.Category,
                IncidentDraft.Field.Severity,
                IncidentDraft.Field.Site,
                IncidentDraft.Field.Photos,
            ),
        )
    )
}

@Composable
private fun IncidentFormScreenPreviewContent(uiState: IncidentFormUiState) {
    HazardReportingTheme {
        IncidentFormScreen(
            uiState = uiState,
            snackbarHostState = remember { SnackbarHostState() },
            actions = IncidentFormActions(
                onClose = {},
                onSave = {},
                onTitleChange = {},
                onDescriptionChange = {},
                onCategorySelect = {},
                onSeveritySelect = {},
                onSiteSelect = {},
                onLocationDescriptionChange = {},
                onFieldBlur = {},
                onAddPhotoClick = {},
                onRemovePhoto = {},
                onPhotoSourceDismiss = {},
                onCameraSelect = {},
                onGallerySelect = {},
                onRecapturePosition = {},
                onDiscardConfirm = {},
                onDiscardDismiss = {},
            ),
        )
    }
}

private val PREVIEW_STATE = IncidentFormUiState(
    categories = listOf(Category("cat-equipment", "Equipment"), Category("cat-electrical", "Electrical")),
    severities = listOf(
        Severity("sev-low", "Low", 1),
        Severity("sev-medium", "Medium", 2),
        Severity("sev-high", "High", 3),
        Severity("sev-critical", "Critical", 4),
    ),
    sites = listOf(Site("site-north-plant", "North Plant", 53.5, -2.2)),
)
