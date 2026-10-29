package com.example.hazardreporting.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.example.hazardreporting.R
import com.example.hazardreporting.domain.model.Position
import kotlin.math.roundToInt

/**
 * @author Roman Pliskin on 29.10.2026
 */

/**
 * Returns the position as its accuracy and coordinates, e.g. "±12 m · 53.50534, -2.20063"
 */
@Composable
@ReadOnlyComposable
fun Position.label(): String = stringResource(R.string.position_value, accuracyM.roundToInt(), lat, lng)
