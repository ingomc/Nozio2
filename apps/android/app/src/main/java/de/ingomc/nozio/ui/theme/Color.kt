package de.ingomc.nozio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Dark tokens
val DarkBaseBg = Color(0xFF020B0C)
val DarkBaseBgElevated = Color(0xFF081012)
val DarkSurface1 = Color(0xFF1B2223)
val DarkSurface2 = Color(0xFF232B2C)
val DarkSurface3 = Color(0xFF313A3B)
val DarkSurfaceBorder = Color(0xFF5C6869)
val DarkTextPrimary = Color(0xFFEEF3F3)
val DarkTextSecondary = Color(0xFFC1C8C9)
val DarkTextMuted = Color(0xFF8D9798)
val DarkAccentPrimary = Color(0xFF14F2CB)
val DarkAccentPrimaryPressed = Color(0xFF0FC7A8)
val DarkAccentOnPrimary = Color(0xFF002019)
val DarkStatusSuccess = Color(0xFF22C55E)
val DarkStatusWarning = Color(0xFFF4C542)
val DarkStatusError = Color(0xFFFF6E70)
val DarkStatusInfo = Color(0xFF62A8FF)
val DarkOverlayScrim = Color(0x99000000)
val DarkLockOverlayBg = Color(0xCC2A3132)
val DarkLockOverlayStripe = Color(0x339AA3A4)
val DarkMacroPrimary = Color(0xFF14F2CB)

// Light tokens
val LightBaseBg = Color(0xFFF4F7F7)
val LightBaseBgElevated = Color(0xFFECF2F2)
val LightSurface1 = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFF1F5F5)
val LightSurface3 = Color(0xFFE6ECEC)
val LightSurfaceBorder = Color(0xFFC5D0D1)
val LightTextPrimary = Color(0xFF111718)
val LightTextSecondary = Color(0xFF4E5B5D)
val LightTextMuted = Color(0xFF6C797B)
val LightAccentPrimary = Color(0xFF00C8AA)
val LightAccentPrimaryPressed = Color(0xFF00A78D)
val LightAccentOnPrimary = Color(0xFFFFFFFF)
val LightStatusSuccess = Color(0xFF1F9E4A)
val LightStatusWarning = Color(0xFFBF8C00)
val LightStatusError = Color(0xFFD7484A)
val LightStatusInfo = Color(0xFF2D78D6)
val LightOverlayScrim = Color(0x33000000)
val LightLockOverlayBg = Color(0xCCD7DFE0)
val LightLockOverlayStripe = Color(0x337E8A8B)
val LightMacroPrimary = Color(0xFF00C8AA)

@Immutable
data class NozioExtraColors(
    val baseBgElevated: Color,
    val surface2: Color,
    val borderStrong: Color,
    val mutedText: Color,
    val subduedPrimary: Color,
    val emphasisContainer: Color,
    val emphasisOnContainer: Color,
    val macroPrimary: Color,
    val ringTrack: Color,
    val lockOverlayBg: Color,
    val lockOverlayStripe: Color,
    val success: Color,
    val warning: Color,
    val info: Color
)

val DarkExtraColors = NozioExtraColors(
    baseBgElevated = DarkBaseBgElevated,
    surface2 = DarkSurface2,
    borderStrong = DarkSurfaceBorder,
    mutedText = DarkTextMuted,
    subduedPrimary = DarkAccentPrimary.copy(alpha = 0.14f),
    emphasisContainer = DarkAccentPrimary.copy(alpha = 0.22f),
    emphasisOnContainer = DarkTextPrimary,
    macroPrimary = DarkMacroPrimary,
    ringTrack = DarkSurfaceBorder,
    lockOverlayBg = DarkLockOverlayBg,
    lockOverlayStripe = DarkLockOverlayStripe,
    success = DarkStatusSuccess,
    warning = DarkStatusWarning,
    info = DarkStatusInfo
)

val LightExtraColors = NozioExtraColors(
    baseBgElevated = LightBaseBgElevated,
    surface2 = LightSurface2,
    borderStrong = LightSurfaceBorder,
    mutedText = LightTextMuted,
    subduedPrimary = LightAccentPrimary.copy(alpha = 0.12f),
    emphasisContainer = LightAccentPrimary.copy(alpha = 0.18f),
    emphasisOnContainer = LightTextPrimary,
    macroPrimary = LightMacroPrimary,
    ringTrack = LightSurfaceBorder,
    lockOverlayBg = LightLockOverlayBg,
    lockOverlayStripe = LightLockOverlayStripe,
    success = LightStatusSuccess,
    warning = LightStatusWarning,
    info = LightStatusInfo
)
