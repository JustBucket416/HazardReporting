package com.example.hazardreporting.presentation.incidentdetail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hazardreporting.R
import com.example.hazardreporting.domain.model.IncidentDetails
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Priority
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.presentation.common.CollectEvents
import com.example.hazardreporting.presentation.common.PhotoImage
import com.example.hazardreporting.presentation.common.SeverityChip
import com.example.hazardreporting.presentation.common.ShimmerPlaceholder
import com.example.hazardreporting.presentation.common.StatusChip
import com.example.hazardreporting.presentation.common.formatLocalDateTime
import com.example.hazardreporting.presentation.common.label
import com.example.hazardreporting.presentation.theme.HazardReportingTheme
import kotlinx.coroutines.launch

/**
 * The incident detail screen bound to its [IncidentDetailViewModel]
 *
 * @param viewModel manages the screen's state
 * @param onBack called when the screen should be left: from the top bar's back button, or after a withdrawal
 * @param onEdit called when the user picks Edit in the ⋮ menu
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Composable
fun IncidentDetailRoute(viewModel: IncidentDetailViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val refreshFailedMessage = stringResource(R.string.incident_detail_refresh_failed)
    val unexpectedErrorMessage = stringResource(R.string.error_unexpected)

    CollectEvents(viewModel.events) { event ->
        when (event) {
            IncidentDetailEvent.RefreshFailed -> coroutineScope.launch {
                snackbarHostState.showSnackbar(refreshFailedMessage)
            }
            IncidentDetailEvent.UnexpectedError -> coroutineScope.launch {
                snackbarHostState.showSnackbar(unexpectedErrorMessage)
            }
            IncidentDetailEvent.Withdrawn -> onBack()
        }
    }

    IncidentDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = IncidentDetailActions(
            onBack = onBack,
            onRefresh = viewModel::onRefresh,
            onRetry = viewModel::onRetry,
            onEdit = onEdit,
            onWithdraw = viewModel::onWithdrawClick,
            onWithdrawConfirm = viewModel::onWithdrawConfirm,
            onWithdrawDismiss = viewModel::onWithdrawDismiss,
            onDismissStatusChangeNotice = viewModel::onDismissStatusChangeNotice,
        ),
    )
}

/**
 * What the incident detail screen's controls do
 *
 * @property onBack called when the user taps the back button
 * @property onRefresh called on a pull-to-refresh
 * @property onRetry called when the user taps Retry under "Details not downloaded"
 * @property onEdit called when the user picks Edit in the ⋮ menu
 * @property onWithdraw called when the user picks Withdraw… in the ⋮ menu
 * @property onWithdrawConfirm called when the user confirms the withdrawal
 * @property onWithdrawDismiss called when the user keeps the report instead
 * @property onDismissStatusChangeNotice called when the user dismisses the Status Change Notice
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentDetailActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onRetry: () -> Unit,
    val onEdit: () -> Unit,
    val onWithdraw: () -> Unit,
    val onWithdrawConfirm: () -> Unit,
    val onWithdrawDismiss: () -> Unit,
    val onDismissStatusChangeNotice: () -> Unit,
)

/**
 * One incident: its photos in a pager, chips, and every field a technician may see (no assignee).
 * The fields that need downloaded details shimmer until they arrive, or offer Retry if downloading failed.
 * The ⋮ menu with Edit and Withdraw… shows only once the details are downloaded and while the incident is Open;
 * Withdraw… asks for confirmation. A Status Change Notice shows as a banner until it is dismissed
 *
 * @param uiState state of the screen
 * @param snackbarHostState shows the screen's snackbars
 * @param actions what the screen's controls do
 * @param modifier modifier for the screen's root
 *
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    uiState: IncidentDetailUiState,
    snackbarHostState: SnackbarHostState,
    actions: IncidentDetailActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.incident_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (uiState.isMenuVisible) IncidentMenu(actions.onEdit, actions.onWithdraw)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = actions.onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                uiState.incident?.let { IncidentContent(it, actions) }
            }
        }
    }

    if (uiState.isWithdrawDialogVisible) WithdrawDialog(actions)
}

@Composable
private fun IncidentMenu(onEdit: () -> Unit, onWithdraw: () -> Unit) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.action_more_options))
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.incident_action_edit)) },
                onClick = {
                    isExpanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.incident_action_withdraw)) },
                onClick = {
                    isExpanded = false
                    onWithdraw()
                },
            )
        }
    }
}

@Composable
private fun WithdrawDialog(actions: IncidentDetailActions) {
    AlertDialog(
        onDismissRequest = actions.onWithdrawDismiss,
        title = { Text(stringResource(R.string.incident_withdraw_title)) },
        text = { Text(stringResource(R.string.incident_withdraw_text)) },
        confirmButton = {
            TextButton(onClick = actions.onWithdrawConfirm) { Text(stringResource(R.string.incident_withdraw_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = actions.onWithdrawDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun IncidentContent(incident: IncidentDetailContent, actions: IncidentDetailActions) {
    when (val details = incident.details) {
        is IncidentDetailsState.Loaded -> if (details.details.photos.isNotEmpty()) PhotoPager(details.details.photos)
        IncidentDetailsState.Loading -> ShimmerPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PHOTO_ASPECT_RATIO),
            shape = MaterialTheme.shapes.extraSmall,
        )
        IncidentDetailsState.NotDownloaded -> Unit
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = incident.title, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(incident.status)
            SeverityChip(incident.severity)
        }
        if (incident.hasStatusChangeNotice) {
            StatusChangeNotice(incident.status, actions.onDismissStatusChangeNotice)
        }
        if (incident.isPending) PendingIndicator()

        val missing = stringResource(R.string.value_missing)
        DetailField(stringResource(R.string.incident_field_category), incident.categoryName ?: missing)
        DetailField(stringResource(R.string.incident_field_site), incident.siteName ?: missing)
        DetailField(stringResource(R.string.incident_field_reported), formatLocalDateTime(incident.createdAt))

        when (val details = incident.details) {
            is IncidentDetailsState.Loaded -> LoadedDetails(details.details)
            IncidentDetailsState.Loading -> LoadingDetails()
            IncidentDetailsState.NotDownloaded -> DetailsNotDownloaded(actions.onRetry)
        }
    }
}

@Composable
private fun StatusChangeNotice(status: IncidentStatus, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Decorative: the text says it all
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null)
                Text(
                    text = stringResource(R.string.incident_status_change_notice_banner, status.label()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
    }
}

@Composable
private fun PhotoPager(photos: List<Photo>) {
    val pagerState = rememberPagerState { photos.size }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pagerState) { page ->
            PhotoImage(
                photo = photos[page],
                contentDescription = stringResource(R.string.incident_photo_description, page + 1, photos.size),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PHOTO_ASPECT_RATIO),
            )
        }
        if (photos.size > 1) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(photos.size) { page ->
                    val color = if (page == pagerState.currentPage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(PAGE_INDICATOR_SIZE)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingIndicator() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_cloud_upload),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.incident_pending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadedDetails(details: IncidentDetails) {
    val missing = stringResource(R.string.value_missing)
    DetailField(stringResource(R.string.incident_field_priority), details.priority?.label() ?: missing)
    DetailField(stringResource(R.string.incident_field_description), details.description.ifBlank { missing })
    DetailField(
        stringResource(R.string.incident_field_location_description),
        details.locationDescription.ifBlank { missing },
    )
    DetailField(stringResource(R.string.incident_field_position), details.position?.label() ?: missing)
}

@Composable
private fun LoadingDetails() {
    listOf(
        R.string.incident_field_priority,
        R.string.incident_field_description,
        R.string.incident_field_location_description,
        R.string.incident_field_position,
    ).forEach { label ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FieldLabel(stringResource(label))
            ShimmerPlaceholder(Modifier.fillMaxWidth(0.6f).height(20.dp))
        }
    }
}

@Composable
private fun DetailsNotDownloaded(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.incident_details_not_downloaded),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FieldLabel(label: String) {
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Priority.label(): String = stringResource(
    when (this) {
        Priority.Low -> R.string.incident_priority_low
        Priority.Medium -> R.string.incident_priority_medium
        Priority.High -> R.string.incident_priority_high
        Priority.Urgent -> R.string.incident_priority_urgent
        Priority.Unknown -> R.string.incident_priority_unknown
    }
)

private const val PHOTO_ASPECT_RATIO = 4f / 3f
private val PAGE_INDICATOR_SIZE = 8.dp

@Preview(name = "Details, light")
@Preview(name = "Details, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncidentDetailScreenPreview() {
    IncidentDetailScreenPreviewContent(
        PREVIEW_CONTENT.copy(
            details = IncidentDetailsState.Loaded(
                IncidentDetails(
                    description = "The edge of the belt is fraying over about two metres.",
                    locationDescription = "Line 3 transfer point",
                    position = Position(53.50534, -2.20063, 10.0),
                    photos = listOf(Photo.Remote("http://10.0.2.2:3001/uploads/seed/photo-05.png")),
                    priority = null,
                )
            )
        )
    )
}

@Preview(name = "Loading")
@Composable
private fun IncidentDetailScreenLoadingPreview() {
    IncidentDetailScreenPreviewContent(PREVIEW_CONTENT)
}

@Preview(name = "Status Change Notice")
@Composable
private fun IncidentDetailScreenNoticePreview() {
    IncidentDetailScreenPreviewContent(
        PREVIEW_CONTENT.copy(
            status = IncidentStatus.Acknowledged,
            isPending = false,
            hasStatusChangeNotice = true,
            details = IncidentDetailsState.NotDownloaded,
        )
    )
}

@Preview(name = "Not downloaded")
@Composable
private fun IncidentDetailScreenNotDownloadedPreview() {
    IncidentDetailScreenPreviewContent(PREVIEW_CONTENT.copy(details = IncidentDetailsState.NotDownloaded))
}

@Composable
private fun IncidentDetailScreenPreviewContent(content: IncidentDetailContent) {
    HazardReportingTheme {
        IncidentDetailScreen(
            uiState = IncidentDetailUiState(incident = content),
            snackbarHostState = remember { SnackbarHostState() },
            actions = IncidentDetailActions(
                onBack = {},
                onRefresh = {},
                onRetry = {},
                onEdit = {},
                onWithdraw = {},
                onWithdrawConfirm = {},
                onWithdrawDismiss = {},
                onDismissStatusChangeNotice = {},
            ),
        )
    }
}

private val PREVIEW_CONTENT = IncidentDetailContent(
    title = "Conveyor belt 3 fraying",
    status = IncidentStatus.Open,
    severity = Severity("sev-high", "High", 3),
    categoryName = "Equipment",
    siteName = "North Plant",
    createdAt = 1_793_015_100_000L,
    isPending = true,
    hasStatusChangeNotice = false,
    details = IncidentDetailsState.Loading,
)
