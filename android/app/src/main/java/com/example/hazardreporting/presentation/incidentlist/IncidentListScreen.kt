package com.example.hazardreporting.presentation.incidentlist

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hazardreporting.R
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.presentation.common.CollectEvents
import com.example.hazardreporting.presentation.common.SeverityChip
import com.example.hazardreporting.presentation.common.ShimmerPlaceholder
import com.example.hazardreporting.presentation.common.StatusChip
import com.example.hazardreporting.presentation.common.formatLocalDate
import com.example.hazardreporting.presentation.theme.HazardReportingTheme
import kotlinx.coroutines.launch

/**
 * The incident list bound to its [IncidentListViewModel]
 *
 * @param viewModel manages the screen's state
 * @param reportSaved whether the report form just saved a report; shows the "Report saved" snackbar once
 * @param onReportSavedShown called once the "saved" result is consumed, so it isn't shown again
 * @param onIncidentClick called with the local id of the tapped incident
 * @param onReportClick called when the user taps "Report incident"
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Composable
fun IncidentListRoute(
    viewModel: IncidentListViewModel,
    reportSaved: Boolean,
    onReportSavedShown: () -> Unit,
    onIncidentClick: (Long) -> Unit,
    onReportClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val refreshFailedMessage = stringResource(R.string.incident_list_refresh_failed)
    val unexpectedErrorMessage = stringResource(R.string.error_unexpected)
    val reportSavedMessage = stringResource(R.string.incident_list_report_saved)

    CollectEvents(viewModel.events) { event ->
        val message = when (event) {
            IncidentListEvent.RefreshFailed -> refreshFailedMessage
            IncidentListEvent.UnexpectedError -> unexpectedErrorMessage
        }
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(reportSaved) {
        if (reportSaved) {
            // Launched outside the effect, which restarts as soon as the result is consumed
            coroutineScope.launch { snackbarHostState.showSnackbar(reportSavedMessage) }
            onReportSavedShown()
        }
    }

    IncidentListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onRetry,
        onIncidentClick = onIncidentClick,
        onReportClick = onReportClick,
    )
}

/**
 * The technician's incidents, newest first, with pull-to-refresh and a "Report incident" button that
 * collapses to its icon while the list scrolls. Shows skeleton rows during the first load, an empty state,
 * or an error card with Retry when nothing is stored
 *
 * @param uiState state of the screen
 * @param snackbarHostState shows the screen's snackbars
 * @param onRefresh called on a pull-to-refresh
 * @param onRetry called when the user taps Retry on the error card
 * @param onIncidentClick called with the local id of the tapped incident
 * @param onReportClick called when the user taps "Report incident"
 * @param modifier modifier for the screen's root
 *
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    uiState: IncidentListUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onIncidentClick: (Long) -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.incident_list_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.incident_list_report)) },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                onClick = onReportClick,
                expanded = !listState.isScrollInProgress,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val content = uiState.content) {
                IncidentListContent.Loading -> SkeletonRows()
                IncidentListContent.Empty -> EmptyState()
                IncidentListContent.Error -> ErrorCard(onRetry)
                is IncidentListContent.Incidents -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
                ) {
                    items(content.items, key = { it.localId }) { item ->
                        IncidentRow(item, onClick = { onIncidentClick(item.localId) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentRow(item: IncidentListItem, onClick: () -> Unit) {
    val missing = stringResource(R.string.value_missing)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.isPending) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = stringResource(R.string.incident_pending),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(ROW_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.hasStatusChangeNotice) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = stringResource(R.string.incident_status_change_notice),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(ROW_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(
            text = stringResource(
                R.string.incident_list_category_and_site,
                item.categoryName ?: missing,
                item.siteName ?: missing,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeverityChip(item.severity)
            StatusChip(item.status)
            Spacer(Modifier.weight(1f))
            Text(
                text = formatLocalDate(item.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SkeletonRows() {
    // Scrollable, so pull-to-refresh works over it like over the list
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        repeat(SKELETON_ROW_COUNT) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShimmerPlaceholder(Modifier.fillMaxWidth(0.7f).height(20.dp))
                ShimmerPlaceholder(Modifier.fillMaxWidth(0.5f).height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerPlaceholder(Modifier.width(64.dp).height(24.dp))
                    ShimmerPlaceholder(Modifier.width(88.dp).height(24.dp))
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyState() {
    CenteredScrollableBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.incident_list_empty),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.incident_list_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    CenteredScrollableBox {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.incident_list_load_failed),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
    }
}

@Composable
private fun CenteredScrollableBox(content: @Composable () -> Unit) {
    // Scrollable, so pull-to-refresh works over it like over the list
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private const val SKELETON_ROW_COUNT = 6
private val ROW_ICON_SIZE = 18.dp
private val FAB_CLEARANCE = 88.dp

@Preview(name = "Incidents, light")
@Preview(name = "Incidents, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncidentListScreenPreview() {
    IncidentListScreenPreviewContent(
        IncidentListUiState(
            content = IncidentListContent.Incidents(
                listOf(
                    IncidentListItem(
                        localId = 1,
                        title = "Conveyor belt 3 fraying",
                        categoryName = "Equipment",
                        siteName = "North Plant",
                        severity = Severity("sev-high", "High", 3),
                        status = IncidentStatus.Open,
                        createdAt = PREVIEW_CREATED_AT,
                        isPending = true,
                        hasStatusChangeNotice = false,
                    ),
                    IncidentListItem(
                        localId = 2,
                        title = "Leaking valve in pump room",
                        categoryName = "Plumbing",
                        siteName = "Depot A",
                        severity = Severity("sev-medium", "Medium", 2),
                        status = IncidentStatus.Acknowledged,
                        createdAt = PREVIEW_CREATED_AT,
                        isPending = false,
                        hasStatusChangeNotice = true,
                    ),
                )
            )
        )
    )
}

@Preview(name = "Loading")
@Composable
private fun IncidentListScreenLoadingPreview() {
    IncidentListScreenPreviewContent(IncidentListUiState(content = IncidentListContent.Loading))
}

@Preview(name = "Empty")
@Composable
private fun IncidentListScreenEmptyPreview() {
    IncidentListScreenPreviewContent(IncidentListUiState(content = IncidentListContent.Empty))
}

@Preview(name = "Error")
@Composable
private fun IncidentListScreenErrorPreview() {
    IncidentListScreenPreviewContent(IncidentListUiState(content = IncidentListContent.Error))
}

@Composable
private fun IncidentListScreenPreviewContent(uiState: IncidentListUiState) {
    HazardReportingTheme {
        IncidentListScreen(
            uiState = uiState,
            snackbarHostState = remember { SnackbarHostState() },
            onRefresh = {},
            onRetry = {},
            onIncidentClick = {},
            onReportClick = {},
        )
    }
}

private const val PREVIEW_CREATED_AT = 1_793_015_100_000L
