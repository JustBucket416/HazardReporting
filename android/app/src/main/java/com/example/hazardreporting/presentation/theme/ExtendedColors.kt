package com.example.hazardreporting.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A container colour and the colour of content drawn on it, e.g. a chip's background and label
 *
 * @property container background colour
 * @property content colour of text and icons on [container]
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Immutable
data class TonalColors(val container: Color, val content: Color)

/**
 * Colours of a severity by its rank: Low is neutral, Medium amber, High orange and Critical uses `error`
 *
 * @property low colours for rank 1
 * @property medium colours for rank 2
 * @property high colours for rank 3
 * @property critical colours for rank 4
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Immutable
data class SeverityColors(
    val low: TonalColors,
    val medium: TonalColors,
    val high: TonalColors,
    val critical: TonalColors,
) {

    /**
     * Returns the colours of a severity with the given [rank] (1 = Low … 4 = Critical).
     * Ranks outside that range are clamped to it
     */
    fun forRank(rank: Int): TonalColors = when {
        rank <= 1 -> low
        rank == 2 -> medium
        rank == 3 -> high
        else -> critical
    }
}

/**
 * Tonal colours of the status chips
 *
 * @property open colours for Open: primary container
 * @property acknowledged colours for Acknowledged: secondary container
 * @property inProgress colours for In Progress: tertiary container
 * @property resolved colours for Resolved: muted green
 * @property rejected colours for Rejected: outline
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Immutable
data class StatusColors(
    val open: TonalColors,
    val acknowledged: TonalColors,
    val inProgress: TonalColors,
    val resolved: TonalColors,
    val rejected: TonalColors,
)

/**
 * Colour tokens the Material colour scheme has no role for
 *
 * @property severity colours of severity chips and buttons
 * @property status colours of status chips
 * @property shimmerHighlight colour of the highlight that sweeps across shimmering placeholders
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Immutable
data class ExtendedColors(
    val severity: SeverityColors,
    val status: StatusColors,
    val shimmerHighlight: Color,
)

/**
 * The [ExtendedColors] of the current theme
 */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

/**
 * Provides [ExtendedColors] down the tree; set by [HazardReportingTheme]
 */
internal val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("No ExtendedColors provided. Wrap the content in HazardReportingTheme")
}

/**
 * Builds the [ExtendedColors] that match [colorScheme]
 *
 * @param colorScheme the theme's Material colour scheme
 * @param darkTheme whether [colorScheme] is the dark one
 */
internal fun extendedColorsOf(colorScheme: ColorScheme, darkTheme: Boolean): ExtendedColors {
    val severity = SeverityColors(
        low = TonalColors(colorScheme.surfaceContainerHighest, colorScheme.onSurfaceVariant),
        medium = if (darkTheme) {
            TonalColors(AmberContainerDark, OnAmberContainerDark)
        } else {
            TonalColors(AmberContainerLight, OnAmberContainerLight)
        },
        high = if (darkTheme) {
            TonalColors(OrangeContainerDark, OnOrangeContainerDark)
        } else {
            TonalColors(OrangeContainerLight, OnOrangeContainerLight)
        },
        critical = TonalColors(colorScheme.error, colorScheme.onError),
    )
    val status = StatusColors(
        open = TonalColors(colorScheme.primaryContainer, colorScheme.onPrimaryContainer),
        acknowledged = TonalColors(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer),
        inProgress = TonalColors(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer),
        resolved = if (darkTheme) {
            TonalColors(MutedGreenContainerDark, OnMutedGreenContainerDark)
        } else {
            TonalColors(MutedGreenContainerLight, OnMutedGreenContainerLight)
        },
        rejected = TonalColors(colorScheme.outline, colorScheme.surface),
    )
    return ExtendedColors(
        severity = severity,
        status = status,
        shimmerHighlight = if (darkTheme) ShimmerHighlightDark else ShimmerHighlightLight,
    )
}
