package com.example.hazardreporting.presentation.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hazardreporting.R
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.presentation.theme.HazardReportingTheme
import com.example.hazardreporting.presentation.theme.TonalColors
import com.example.hazardreporting.presentation.theme.extendedColors

/**
 * @author Roman Pliskin on 29.10.2026
 */

/**
 * A chip that shows an incident's severity, coloured by its rank. Shows "—" while the severity is unknown,
 * e.g. before the catalog is downloaded
 *
 * @param severity the incident's severity, or null if it isn't in the stored catalog
 * @param modifier modifier for the chip
 */
@Composable
fun SeverityChip(severity: Severity?, modifier: Modifier = Modifier) {
    val colors = if (severity != null) {
        MaterialTheme.extendedColors.severity.forRank(severity.rank)
    } else {
        MaterialTheme.extendedColors.severity.low
    }
    TonalChip(
        text = severity?.name ?: stringResource(R.string.value_missing),
        colors = colors,
        modifier = modifier,
    )
}

/**
 * A chip that shows an incident's status in its tonal colour
 *
 * @param status the incident's status
 * @param modifier modifier for the chip
 */
@Composable
fun StatusChip(status: IncidentStatus, modifier: Modifier = Modifier) {
    TonalChip(text = status.label(), colors = status.colors(), modifier = modifier)
}

/**
 * Returns the display name of the status
 */
@Composable
@ReadOnlyComposable
fun IncidentStatus.label(): String = stringResource(
    when (this) {
        IncidentStatus.Open -> R.string.incident_status_open
        IncidentStatus.Acknowledged -> R.string.incident_status_acknowledged
        IncidentStatus.InProgress -> R.string.incident_status_in_progress
        IncidentStatus.Resolved -> R.string.incident_status_resolved
        IncidentStatus.Rejected -> R.string.incident_status_rejected
        IncidentStatus.Withdrawn -> R.string.incident_status_withdrawn
        IncidentStatus.Unknown -> R.string.incident_status_unknown
    }
)

@Composable
@ReadOnlyComposable
private fun IncidentStatus.colors(): TonalColors {
    val status = MaterialTheme.extendedColors.status
    return when (this) {
        IncidentStatus.Open -> status.open
        IncidentStatus.Acknowledged -> status.acknowledged
        IncidentStatus.InProgress -> status.inProgress
        IncidentStatus.Resolved -> status.resolved
        // The other final statuses and unknown ones share Rejected's muted outline
        IncidentStatus.Rejected, IncidentStatus.Withdrawn, IncidentStatus.Unknown -> status.rejected
    }
}

@Composable
private fun TonalChip(text: String, colors: TonalColors, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncidentChipsPreview() {
    HazardReportingTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeverityChip(Severity("sev-critical", "Critical", 4))
                SeverityChip(Severity("sev-medium", "Medium", 2))
                StatusChip(IncidentStatus.InProgress)
                StatusChip(IncidentStatus.Resolved)
            }
        }
    }
}
