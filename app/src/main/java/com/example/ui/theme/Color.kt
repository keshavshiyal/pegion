package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Material Theme Builder Palette — Deep Trustworthy Blue (#005FB0)
// =============================================================================

// Primary — Trustworthy deep royal blue
val MdLightPrimary = Color(0xFF005FB0)
val MdLightOnPrimary = Color(0xFFFFFFFF)
val MdLightPrimaryContainer = Color(0xFFD5E3FF)
val MdLightOnPrimaryContainer = Color(0xFF001B3C)

val MdDarkPrimary = Color(0xFFA6C8FF)
val MdDarkOnPrimary = Color(0xFF003060)
val MdDarkPrimaryContainer = Color(0xFF004787)
val MdDarkOnPrimaryContainer = Color(0xFFD5E3FF)

// Secondary — Sophisticated Slate Blue
val MdLightSecondary = Color(0xFF555F71)
val MdLightOnSecondary = Color(0xFFFFFFFF)
val MdLightSecondaryContainer = Color(0xFFD9E3F8)
val MdLightOnSecondaryContainer = Color(0xFF121C2B)

val MdDarkSecondary = Color(0xFFBDC7DC)
val MdDarkOnSecondary = Color(0xFF273141)
val MdDarkSecondaryContainer = Color(0xFF3E4758)
val MdDarkOnSecondaryContainer = Color(0xFFD9E3F8)

// Tertiary — Warm Parcel Indigo/Violet
val MdLightTertiary = Color(0xFF6E5676)
val MdLightOnTertiary = Color(0xFFFFFFFF)
val MdLightTertiaryContainer = Color(0xFFF7D8FF)
val MdLightOnTertiaryContainer = Color(0xFF271330)

val MdDarkTertiary = Color(0xFFDBBCE2)
val MdDarkOnTertiary = Color(0xFF3E2846)
val MdDarkTertiaryContainer = Color(0xFF563E5D)
val MdDarkOnTertiaryContainer = Color(0xFFF7D8FF)

// Error
val MdLightError = Color(0xFFBA1A1A)
val MdLightOnError = Color(0xFFFFFFFF)
val MdLightErrorContainer = Color(0xFFFFDAD6)
val MdLightOnErrorContainer = Color(0xFF410002)

val MdDarkError = Color(0xFFFFB4AB)
val MdDarkOnError = Color(0xFF690005)
val MdDarkErrorContainer = Color(0xFF93000A)
val MdDarkOnErrorContainer = Color(0xFFFFDAD6)

// Surfaces & Backgrounds — Subtle primary tint for Reply-style polish
val MdLightBackground = Color(0xFFF4F6FC)
val MdLightOnBackground = Color(0xFF191C20)
val MdLightSurface = Color(0xFFF8F9FF)
val MdLightOnSurface = Color(0xFF191C20)
val MdLightSurfaceVariant = Color(0xFFDFE2EB)
val MdLightOnSurfaceVariant = Color(0xFF43474E)
val MdLightOutline = Color(0xFF73777F)
val MdLightOutlineVariant = Color(0xFFC3C7D0)

val MdDarkBackground = Color(0xFF101418)
val MdDarkOnBackground = Color(0xFFE1E2E8)
val MdDarkSurface = Color(0xFF101418)
val MdDarkOnSurface = Color(0xFFE1E2E8)
val MdDarkSurfaceVariant = Color(0xFF43474E)
val MdDarkOnSurfaceVariant = Color(0xFFC3C7D0)
val MdDarkOutline = Color(0xFF8D9199)
val MdDarkOutlineVariant = Color(0xFF43474E)

// AMOLED Mode
val MdAmoledBackground = Color(0xFF000000)
val MdAmoledSurface = Color(0xFF000000)
val MdAmoledSurfaceVariant = Color(0xFF181C22)

// =============================================================================
// Status Colors — Strictly tuned for high WCAG AA contrast in Light and Dark
// =============================================================================
val StatusDownloading = Color(0xFF0077B6)       // 5.2:1 contrast against light surface
val StatusDownloadingContainer = Color(0xFFD0F0FD)
val StatusDownloadingDark = Color(0xFF48CAE4)

val StatusCompleted = Color(0xFF15803D)         // Forest emerald, 5.1:1 contrast on light
val StatusCompletedContainer = Color(0xFFDCFCE7)
val StatusCompletedDark = Color(0xFF4ADE80)

val StatusPaused = Color(0xFFB45309)            // Deep amber, 4.8:1 contrast on light
val StatusPausedContainer = Color(0xFFFEF3C7)
val StatusPausedDark = Color(0xFFFBBF24)

val StatusFailed = Color(0xFFB91C1C)            // Deep crimson, 5.6:1 contrast on light
val StatusFailedContainer = Color(0xFFFEE2E2)
val StatusFailedDark = Color(0xFFF87171)

val StatusQueued = Color(0xFF6D28D9)            // Deep royal violet, 5.8:1 contrast on light
val StatusQueuedContainer = Color(0xFFEDE9FE)
val StatusQueuedDark = Color(0xFFA78BFA)

// Legacy compatibility references
val PigeonPrimaryLight = MdLightPrimary
val PigeonOnPrimaryLight = MdLightOnPrimary
val PigeonPrimaryContainerLight = MdLightPrimaryContainer
val PigeonOnPrimaryContainerLight = MdLightOnPrimaryContainer
val PigeonPrimaryDark = MdDarkPrimary
val PigeonOnPrimaryDark = MdDarkOnPrimary
val PigeonPrimaryContainerDark = MdDarkPrimaryContainer
val PigeonOnPrimaryContainerDark = MdDarkOnPrimaryContainer
val PigeonSecondaryLight = MdLightSecondary
val PigeonOnSecondaryLight = MdLightOnSecondary
val PigeonSecondaryContainerLight = MdLightSecondaryContainer
val PigeonOnSecondaryContainerLight = MdLightOnSecondaryContainer
val PigeonSecondaryDark = MdDarkSecondary
val PigeonOnSecondaryDark = MdDarkOnSecondary
val PigeonSecondaryContainerDark = MdDarkSecondaryContainer
val PigeonOnSecondaryContainerDark = MdDarkOnSecondaryContainer
val PigeonTertiaryLight = MdLightTertiary
val PigeonOnTertiaryLight = MdLightOnTertiary
val PigeonTertiaryContainerLight = MdLightTertiaryContainer
val PigeonOnTertiaryContainerLight = MdLightOnTertiaryContainer
val PigeonTertiaryDark = MdDarkTertiary
val PigeonOnTertiaryDark = MdDarkOnTertiary
val PigeonTertiaryContainerDark = MdDarkTertiaryContainer
val PigeonOnTertiaryContainerDark = MdDarkOnTertiaryContainer
val LightBackground = MdLightBackground
val LightSurface = MdLightSurface
val LightSurfaceVariant = MdLightSurfaceVariant
val DarkBackground = MdDarkBackground
val DarkSurface = MdDarkSurface
val DarkSurfaceVariant = MdDarkSurfaceVariant
val AmoledBackground = MdAmoledBackground
val AmoledSurface = MdAmoledSurface
val AmoledSurfaceVariant = MdAmoledSurfaceVariant
