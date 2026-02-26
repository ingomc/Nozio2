package de.ingomc.nozio.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun CalorieRing(
    consumed: Double,
    goal: Double,
    centerValue: String = consumed.toInt().toString(),
    centerLabel: String = "von ${goal.toInt()} kcal",
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (consumed / goal).toFloat() else 0f
    val clampedProgress = min(progress, 1f)
    val arcStartAngle = 140f
    val arcSweepAngle = 260f

    var animatedTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(clampedProgress) {
        animatedTarget = clampedProgress
    }
    val animatedProgress by animateFloatAsState(
        targetValue = animatedTarget,
        animationSpec = tween(durationMillis = 800),
        label = "calorieRingAnimation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val overColor = MaterialTheme.colorScheme.error

    val ringColor = if (progress > 1f) overColor else primaryColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(140.dp)
    ) {
        Canvas(modifier = Modifier.size(152.dp)) {
            val strokeWidth = 14.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            // Track
            drawArc(
                color = trackColor,
                startAngle = arcStartAngle,
                sweepAngle = arcSweepAngle,
                useCenter = false,
                style = stroke
            )

            // Progress
            drawArc(
                color = ringColor,
                startAngle = arcStartAngle,
                sweepAngle = arcSweepAngle * animatedProgress,
                useCenter = false,
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
