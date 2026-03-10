package de.ingomc.nozio.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

object ExpressiveMotion {
    const val DurationShort = 180
    const val DurationMedium = 280
    const val DurationLong = 360

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
