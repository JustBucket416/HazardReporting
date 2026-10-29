package com.example.hazardreporting.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * The fixed brand palette. Only the theme reads these values;
 * everything else uses `MaterialTheme.colorScheme` or [ExtendedColors]
 *
 * @author Roman Pliskin on 29.10.2026
 */

// region Light scheme
internal val PrimaryLight = Color(0xFF415F91)
internal val OnPrimaryLight = Color(0xFFFFFFFF)
internal val PrimaryContainerLight = Color(0xFFD6E3FF)
internal val OnPrimaryContainerLight = Color(0xFF284777)
internal val SecondaryLight = Color(0xFF565F71)
internal val OnSecondaryLight = Color(0xFFFFFFFF)
internal val SecondaryContainerLight = Color(0xFFDAE2F9)
internal val OnSecondaryContainerLight = Color(0xFF3E4759)
internal val TertiaryLight = Color(0xFF705575)
internal val OnTertiaryLight = Color(0xFFFFFFFF)
internal val TertiaryContainerLight = Color(0xFFFAD8FD)
internal val OnTertiaryContainerLight = Color(0xFF573E5C)
internal val ErrorLight = Color(0xFFBA1A1A)
internal val OnErrorLight = Color(0xFFFFFFFF)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val OnErrorContainerLight = Color(0xFF93000A)
internal val BackgroundLight = Color(0xFFF9F9FF)
internal val OnBackgroundLight = Color(0xFF191C20)
internal val SurfaceLight = Color(0xFFF9F9FF)
internal val OnSurfaceLight = Color(0xFF191C20)
internal val SurfaceVariantLight = Color(0xFFE0E2EC)
internal val OnSurfaceVariantLight = Color(0xFF44474E)
internal val OutlineLight = Color(0xFF74777F)
internal val OutlineVariantLight = Color(0xFFC4C6D0)
internal val ScrimLight = Color(0xFF000000)
internal val InverseSurfaceLight = Color(0xFF2E3036)
internal val InverseOnSurfaceLight = Color(0xFFF0F0F7)
internal val InversePrimaryLight = Color(0xFFAAC7FF)
internal val SurfaceDimLight = Color(0xFFD9D9E0)
internal val SurfaceBrightLight = Color(0xFFF9F9FF)
internal val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
internal val SurfaceContainerLowLight = Color(0xFFF3F3FA)
internal val SurfaceContainerLight = Color(0xFFEDEDF4)
internal val SurfaceContainerHighLight = Color(0xFFE7E8EE)
internal val SurfaceContainerHighestLight = Color(0xFFE2E2E9)
// endregion

// region Dark scheme
internal val PrimaryDark = Color(0xFFAAC7FF)
internal val OnPrimaryDark = Color(0xFF0A305F)
internal val PrimaryContainerDark = Color(0xFF284777)
internal val OnPrimaryContainerDark = Color(0xFFD6E3FF)
internal val SecondaryDark = Color(0xFFBEC6DC)
internal val OnSecondaryDark = Color(0xFF283141)
internal val SecondaryContainerDark = Color(0xFF3E4759)
internal val OnSecondaryContainerDark = Color(0xFFDAE2F9)
internal val TertiaryDark = Color(0xFFDDBCE0)
internal val OnTertiaryDark = Color(0xFF3F2844)
internal val TertiaryContainerDark = Color(0xFF573E5C)
internal val OnTertiaryContainerDark = Color(0xFFFAD8FD)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val OnErrorDark = Color(0xFF690005)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)
internal val BackgroundDark = Color(0xFF111318)
internal val OnBackgroundDark = Color(0xFFE2E2E9)
internal val SurfaceDark = Color(0xFF111318)
internal val OnSurfaceDark = Color(0xFFE2E2E9)
internal val SurfaceVariantDark = Color(0xFF44474E)
internal val OnSurfaceVariantDark = Color(0xFFC4C6D0)
internal val OutlineDark = Color(0xFF8E9099)
internal val OutlineVariantDark = Color(0xFF44474E)
internal val ScrimDark = Color(0xFF000000)
internal val InverseSurfaceDark = Color(0xFFE2E2E9)
internal val InverseOnSurfaceDark = Color(0xFF2E3036)
internal val InversePrimaryDark = Color(0xFF415F91)
internal val SurfaceDimDark = Color(0xFF111318)
internal val SurfaceBrightDark = Color(0xFF37393E)
internal val SurfaceContainerLowestDark = Color(0xFF0C0E13)
internal val SurfaceContainerLowDark = Color(0xFF191C20)
internal val SurfaceContainerDark = Color(0xFF1D2024)
internal val SurfaceContainerHighDark = Color(0xFF282A2F)
internal val SurfaceContainerHighestDark = Color(0xFF33353A)
// endregion

// region Severity and status accents that the Material scheme has no role for
internal val AmberContainerLight = Color(0xFFFFE08B)
internal val OnAmberContainerLight = Color(0xFF241A00)
internal val AmberContainerDark = Color(0xFF574500)
internal val OnAmberContainerDark = Color(0xFFFFE08B)

internal val OrangeContainerLight = Color(0xFFFFDBCA)
internal val OnOrangeContainerLight = Color(0xFF341100)
internal val OrangeContainerDark = Color(0xFF773300)
internal val OnOrangeContainerDark = Color(0xFFFFDBCA)

internal val MutedGreenContainerLight = Color(0xFFCDE8CF)
internal val OnMutedGreenContainerLight = Color(0xFF1B3B20)
internal val MutedGreenContainerDark = Color(0xFF2F4D33)
internal val OnMutedGreenContainerDark = Color(0xFFCDE8CF)

internal val ShimmerHighlightLight = Color.White.copy(alpha = 0.6f)
internal val ShimmerHighlightDark = Color.White.copy(alpha = 0.1f)
// endregion
