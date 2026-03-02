package de.ingomc.nozio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccentPrimary,
    onPrimary = DarkAccentOnPrimary,
    primaryContainer = DarkAccentPrimaryPressed,
    onPrimaryContainer = DarkTextPrimary,
    inversePrimary = LightAccentPrimary,
    secondary = DarkAccentPrimaryPressed,
    onSecondary = DarkTextPrimary,
    secondaryContainer = DarkSurface2,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkStatusWarning,
    onTertiary = DarkAccentOnPrimary,
    tertiaryContainer = DarkSurface2,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBaseBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface1,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface3,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = DarkAccentPrimary,
    inverseSurface = LightSurface1,
    inverseOnSurface = LightTextPrimary,
    error = DarkStatusError,
    onError = DarkAccentOnPrimary,
    errorContainer = DarkSurface2,
    onErrorContainer = DarkStatusError,
    outline = DarkSurfaceBorder,
    outlineVariant = DarkSurfaceBorder.copy(alpha = 0.55f),
    scrim = DarkOverlayScrim
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentPrimary,
    onPrimary = LightAccentOnPrimary,
    primaryContainer = LightAccentPrimaryPressed,
    onPrimaryContainer = LightAccentOnPrimary,
    inversePrimary = DarkAccentPrimary,
    secondary = LightAccentPrimaryPressed,
    onSecondary = LightAccentOnPrimary,
    secondaryContainer = LightSurface2,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightStatusWarning,
    onTertiary = LightAccentOnPrimary,
    tertiaryContainer = LightSurface2,
    onTertiaryContainer = LightTextPrimary,
    background = LightBaseBg,
    onBackground = LightTextPrimary,
    surface = LightSurface1,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurface3,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = LightAccentPrimary,
    inverseSurface = DarkSurface1,
    inverseOnSurface = DarkTextPrimary,
    error = LightStatusError,
    onError = LightAccentOnPrimary,
    errorContainer = LightSurface2,
    onErrorContainer = LightStatusError,
    outline = LightSurfaceBorder,
    outlineVariant = LightSurfaceBorder.copy(alpha = 0.65f),
    scrim = LightOverlayScrim
)

val LocalNozioExtraColors = staticCompositionLocalOf { DarkExtraColors }

val MaterialTheme.nozioColors: NozioExtraColors
    @Composable
    get() = LocalNozioExtraColors.current

@Composable
fun NozioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalNozioExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
