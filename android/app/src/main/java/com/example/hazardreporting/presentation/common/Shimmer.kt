package com.example.hazardreporting.presentation.common

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hazardreporting.presentation.theme.HazardReportingTheme
import com.example.hazardreporting.presentation.theme.extendedColors
import kotlinx.coroutines.launch

/**
 * The app's loading indicator: a highlight band that sweeps across the element, drawn over its content.
 * Apply it to placeholder shapes while content loads, or to a control that is busy.
 * The highlight isn't clipped, so clip the element to its shape first
 *
 * @param highlightColor colour of the sweeping band, usually [ShimmerDefaults.highlightColor]
 * @param enabled whether to shimmer; when false the modifier draws nothing
 *
 * @author Roman Pliskin on 29.10.2026
 */
fun Modifier.shimmer(highlightColor: Color, enabled: Boolean = true): Modifier =
    if (enabled) this then ShimmerElement(highlightColor) else this

/**
 * Default values for [shimmer]
 *
 * @author Roman Pliskin on 29.10.2026
 */
object ShimmerDefaults {

    /**
     * Colour of placeholder shapes that stand in for content while it loads
     */
    val placeholderColor: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest

    /**
     * Highlight colour that reads well over [placeholderColor] in both light and dark themes
     */
    val highlightColor: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.extendedColors.shimmerHighlight
}

private const val SWEEP_DURATION_MILLIS = 1200
private const val BAND_WIDTH_FRACTION = 0.6f

private data class ShimmerElement(val highlightColor: Color) : ModifierNodeElement<ShimmerNode>() {

    override fun create() = ShimmerNode(highlightColor)

    override fun update(node: ShimmerNode) {
        node.highlightColor = highlightColor
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmer"
        properties["highlightColor"] = highlightColor
    }
}

private class ShimmerNode(var highlightColor: Color) : Modifier.Node(), DrawModifierNode {

    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(SWEEP_DURATION_MILLIS, easing = LinearEasing)),
            )
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val bandWidth = size.width * BAND_WIDTH_FRACTION
        val bandStart = -bandWidth + (size.width + bandWidth) * progress.value
        val brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, highlightColor, Color.Transparent),
            start = Offset(bandStart, 0f),
            end = Offset(bandStart + bandWidth, size.height),
        )
        drawRect(brush)
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShimmerPreview() {
    HazardReportingTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlaceholderShape(Modifier.size(40.dp), CircleShape)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceholderShape(Modifier.fillMaxWidth().height(16.dp), MaterialTheme.shapes.small)
                    PlaceholderShape(Modifier.fillMaxWidth(0.6f).height(12.dp), MaterialTheme.shapes.small)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderShape(modifier: Modifier, shape: Shape) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(ShimmerDefaults.placeholderColor)
            .shimmer(ShimmerDefaults.highlightColor)
    )
}
