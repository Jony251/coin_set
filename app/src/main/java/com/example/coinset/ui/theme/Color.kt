package com.example.coinset.ui.theme

import androidx.compose.ui.graphics.Color

// "Burnished Bronze" — one confident signature hue (bronze/copper) plus warm
// neutrals, fitting a numismatics/collector app instead of the unedited
// Compose-template purple. Every M3 ColorScheme role is specified explicitly
// (not just primary/secondary/tertiary) so nothing silently falls back to
// stock Material defaults.

// Light
val md_light_primary = Color(0xFF8A5A2B)
val md_light_onPrimary = Color(0xFFFFFFFF)
val md_light_primaryContainer = Color(0xFFFFDDB3)
val md_light_onPrimaryContainer = Color(0xFF2E1500)

val md_light_secondary = Color(0xFF6F5B40)
val md_light_onSecondary = Color(0xFFFFFFFF)
val md_light_secondaryContainer = Color(0xFFFBDFB6)
val md_light_onSecondaryContainer = Color(0xFF271904)

// tertiary = patina green = the app's one semantic "success/verified/active"
// accent (e.g. the PRO checkmark). Do not introduce a separate green token —
// this is it, chosen deliberately since bronze/copper coins literally patina
// green with age.
val md_light_tertiary = Color(0xFF4C6355)
val md_light_onTertiary = Color(0xFFFFFFFF)
val md_light_tertiaryContainer = Color(0xFFCEEADA)
val md_light_onTertiaryContainer = Color(0xFF092016)

val md_light_error = Color(0xFFBA1A1A)
val md_light_onError = Color(0xFFFFFFFF)
val md_light_errorContainer = Color(0xFFFFDAD6)
val md_light_onErrorContainer = Color(0xFF410002)

val md_light_background = Color(0xFFFFF8F3)
val md_light_onBackground = Color(0xFF201A13)
val md_light_surface = Color(0xFFFFF8F3)
val md_light_onSurface = Color(0xFF201A13)
val md_light_surfaceVariant = Color(0xFFF0E0CF)
val md_light_onSurfaceVariant = Color(0xFF4F4539)
val md_light_outline = Color(0xFF817566)
val md_light_outlineVariant = Color(0xFFD3C4B4)

val md_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_light_surfaceContainerLow = Color(0xFFFBF1E7)
val md_light_surfaceContainer = Color(0xFFF5EBE0)
val md_light_surfaceContainerHigh = Color(0xFFEFE5DB)
val md_light_surfaceContainerHighest = Color(0xFFE9E0D5)

val md_light_inverseSurface = Color(0xFF362F27)
val md_light_inverseOnSurface = Color(0xFFFBEEE0)
val md_light_inversePrimary = Color(0xFFFFB870)
val md_light_scrim = Color(0xFF000000)

// Dark — same hues, dark-optimized tones (roles swap per M3 convention)
val md_dark_primary = Color(0xFFFFB870)
val md_dark_onPrimary = Color(0xFF4A2800)
val md_dark_primaryContainer = Color(0xFF693D07)
val md_dark_onPrimaryContainer = Color(0xFFFFDDB3)

val md_dark_secondary = Color(0xFFDDC3A0)
val md_dark_onSecondary = Color(0xFF3C2E16)
val md_dark_secondaryContainer = Color(0xFF55442A)
val md_dark_onSecondaryContainer = Color(0xFFFBDFB6)

val md_dark_tertiary = Color(0xFFB2CEBE)
val md_dark_onTertiary = Color(0xFF1E3728)
val md_dark_tertiaryContainer = Color(0xFF344E3E)
val md_dark_onTertiaryContainer = Color(0xFFCEEADA)

val md_dark_error = Color(0xFFFFB4AB)
val md_dark_onError = Color(0xFF690005)
val md_dark_errorContainer = Color(0xFF93000A)
val md_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_dark_background = Color(0xFF17130D)
val md_dark_onBackground = Color(0xFFECE1D4)
val md_dark_surface = Color(0xFF17130D)
val md_dark_onSurface = Color(0xFFECE1D4)
val md_dark_surfaceVariant = Color(0xFF4F4539)
val md_dark_onSurfaceVariant = Color(0xFFD3C4B4)
val md_dark_outline = Color(0xFF9C8F7E)
val md_dark_outlineVariant = Color(0xFF4F4539)

val md_dark_surfaceContainerLowest = Color(0xFF110D08)
val md_dark_surfaceContainerLow = Color(0xFF201A13)
val md_dark_surfaceContainer = Color(0xFF241E16)
val md_dark_surfaceContainerHigh = Color(0xFF2F2820)
val md_dark_surfaceContainerHighest = Color(0xFF3A332A)

val md_dark_inverseSurface = Color(0xFFECE1D4)
val md_dark_inverseOnSurface = Color(0xFF362F27)
val md_dark_inversePrimary = Color(0xFF8A5A2B)
val md_dark_scrim = Color(0xFF000000)
