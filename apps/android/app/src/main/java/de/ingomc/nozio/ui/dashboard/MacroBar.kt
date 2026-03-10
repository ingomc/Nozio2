package de.ingomc.nozio.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun MacroBar(
    label: String,
    consumed: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progressRatio = if (goal > 0) (consumed / goal).toFloat() else 0f
    val targetFillRatio = if (goal > 0.0) progressRatio.coerceIn(0f, 1f) else 0f
    val overRatio = if (goal > 0.0 && progressRatio > 1f) {
        (progressRatio - 1f).coerceIn(0f, 1f)
    } else {
        0f
    }

    var animatedFillTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetFillRatio) {
        animatedFillTarget = targetFillRatio
    }
    val animatedFillRatio by animateFloatAsState(
        targetValue = animatedFillTarget,
        animationSpec = tween(durationMillis = 600),
        label = "macroBarFillAnimation"
    )
    val overColor = MaterialTheme.colorScheme.error
    val trackColor = color.copy(alpha = 0.25f)

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(top = 2.dp)
        ) {
            val radius = size.height / 2f
            drawRoundRect(
                color = trackColor,
                cornerRadius = CornerRadius(radius, radius),
                size = size
            )

            val fillWidth = (size.width * animatedFillRatio).coerceIn(0f, size.width)
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(radius, radius),
                    size = Size(fillWidth, size.height)
                )
            }

            val overStartX = (size.width * (1f - overRatio)).coerceIn(0f, size.width)
            if (overRatio > 0f && fillWidth > overStartX) {
                drawRoundRect(
                    color = overColor,
                    topLeft = Offset(overStartX, 0f),
                    cornerRadius = CornerRadius(radius, radius),
                    size = Size(fillWidth - overStartX, size.height)
                )
            }
        }
        Text(
            text = "${consumed.roundToInt()} / ${goal.roundToInt()} g",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
