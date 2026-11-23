package com.txnrelay.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---- Light: clean white surfaces ----
val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8E9FF),
    onPrimaryContainer = Color(0xFF1B1A6B),
    secondary = Color(0xFF5A5F6E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111318),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFF2F3F8),
    onSurfaceVariant = Color(0xFF5A5F6E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FC),
    surfaceContainer = Color(0xFFF2F3F8),
    surfaceContainerHigh = Color(0xFFECEEF5),
    surfaceContainerHighest = Color(0xFFE6E8F0),
    outline = Color(0xFFC9CCD8),
    outlineVariant = Color(0xFFE4E6EE),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

// ---- Dark: deep blue-grey, not pure black ----
val DarkColors = darkColorScheme(
    primary = Color(0xFF9AA0FF),
    onPrimary = Color(0xFF14137A),
    primaryContainer = Color(0xFF2B2A8F),
    onPrimaryContainer = Color(0xFFE2E3FF),
    secondary = Color(0xFFB4B9C7),
    onSecondary = Color(0xFF1E222B),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF1C222B),
    onSurfaceVariant = Color(0xFF9BA1B0),
    surfaceContainerLowest = Color(0xFF0A0D11),
    surfaceContainerLow = Color(0xFF151922),
    surfaceContainer = Color(0xFF1A1F29),
    surfaceContainerHigh = Color(0xFF212733),
    surfaceContainerHighest = Color(0xFF282F3C),
    outline = Color(0xFF3A4150),
    outlineVariant = Color(0xFF262C38),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3D0A07),
)

/** Colours for sync status chips / stat tiles (kept outside Material's scheme on purpose). */
@Immutable
data class StatusPalette(
    val sent: Color,
    val sentBg: Color,
    val pending: Color,
    val pendingBg: Color,
    val failed: Color,
    val failedBg: Color,
)

val LightStatus = StatusPalette(
    sent = Color(0xFF1B7F4B), sentBg = Color(0xFFE3F5EA),
    pending = Color(0xFFA15C00), pendingBg = Color(0xFFFFF1DB),
    failed = Color(0xFFB3261E), failedBg = Color(0xFFFCE8E6),
)

val DarkStatus = StatusPalette(
    sent = Color(0xFF6FD39B), sentBg = Color(0xFF17301F),
    pending = Color(0xFFF2B35E), pendingBg = Color(0xFF3A2A10),
    failed = Color(0xFFFF8A80), failedBg = Color(0xFF3D1A18),
)

val LocalStatusPalette = staticCompositionLocalOf { LightStatus }

/** Small brand-tinted badge colours for the app avatar in the list. */
val BkashBadge = Color(0xFFE2136E)
val NagadBadge = Color(0xFFF7941D)
