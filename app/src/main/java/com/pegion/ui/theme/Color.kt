package com.pegion.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Material 3 Expressive Palette — Global High-End Tech Aesthetic
// Electric Sky Azure (#0284C7 / #38BDF8) & Deep Space Slate (#0B0F17)
// =============================================================================

// --- Light Theme Colors ---
val MdLightPrimary = Color(0xFF0284C7)             // Sky 600 - Vivid Electric Azure
val MdLightOnPrimary = Color(0xFFFFFFFF)
val MdLightPrimaryContainer = Color(0xFFE0F2FE)    // Sky 100 - Gentle airy container
val MdLightOnPrimaryContainer = Color(0xFF0369A1)  // Sky 700 - Deep readable text

val MdLightSecondary = Color(0xFF475569)           // Slate 600 - Neutral slate
val MdLightOnSecondary = Color(0xFFFFFFFF)
val MdLightSecondaryContainer = Color(0xFFF1F5F9)  // Slate 100
val MdLightOnSecondaryContainer = Color(0xFF0F172A)// Slate 900

val MdLightTertiary = Color(0xFF6366F1)            // Indigo 500 - Electric accent
val MdLightOnTertiary = Color(0xFFFFFFFF)
val MdLightTertiaryContainer = Color(0xFFEEF2FF)
val MdLightOnTertiaryContainer = Color(0xFF3730A3)

val MdLightError = Color(0xFFDC2626)               // Red 600
val MdLightOnError = Color(0xFFFFFFFF)
val MdLightErrorContainer = Color(0xFFFEE2E2)
val MdLightOnErrorContainer = Color(0xFF991B1B)

val MdLightBackground = Color(0xFFF8FAFC)          // Slate 50 - Ultra clean, bright background
val MdLightOnBackground = Color(0xFF0F172A)
val MdLightSurface = Color(0xFFFFFFFF)             // Pure crisp white cards
val MdLightOnSurface = Color(0xFF0F172A)
val MdLightSurfaceVariant = Color(0xFFF1F5F9)      // Slate 100
val MdLightOnSurfaceVariant = Color(0xFF64748B)    // Slate 500
val MdLightOutline = Color(0xFFCBD5E1)             // Slate 300
val MdLightOutlineVariant = Color(0xFFE2E8F0)      // Slate 200

// --- Dark Theme Colors (Deep Space Midnight) ---
val MdDarkPrimary = Color(0xFF38BDF8)              // Sky 400 - High-contrast luminous cyan
val MdDarkOnPrimary = Color(0xFF082F49)            // Sky 950
val MdDarkPrimaryContainer = Color(0xFF0C4A6E)     // Sky 900
val MdDarkOnPrimaryContainer = Color(0xFFE0F2FE)

val MdDarkSecondary = Color(0xFF94A3B8)            // Slate 400
val MdDarkOnSecondary = Color(0xFF0F172A)
val MdDarkSecondaryContainer = Color(0xFF1E293B)   // Slate 800
val MdDarkOnSecondaryContainer = Color(0xFFE2E8F0)

val MdDarkTertiary = Color(0xFF818CF8)             // Indigo 400
val MdDarkOnTertiary = Color(0xFF1E1B4B)
val MdDarkTertiaryContainer = Color(0xFF312E81)
val MdDarkOnTertiaryContainer = Color(0xFFE0E7FF)

val MdDarkError = Color(0xFFF87171)                // Red 400
val MdDarkOnError = Color(0xFF450A0A)
val MdDarkErrorContainer = Color(0xFF7F1D1D)
val MdDarkOnErrorContainer = Color(0xFFFECACA)

val MdDarkBackground = Color(0xFF0B0F17)           // Deep Space Midnight Slate
val MdDarkOnBackground = Color(0xFFF8FAFC)
val MdDarkSurface = Color(0xFF0F172A)              // Rich Slate 900 Surface
val MdDarkOnSurface = Color(0xFFF8FAFC)
val MdDarkSurfaceVariant = Color(0xFF1E293B)       // Slate 800
val MdDarkOnSurfaceVariant = Color(0xFF94A3B8)     // Slate 400
val MdDarkOutline = Color(0xFF334155)              // Slate 700
val MdDarkOutlineVariant = Color(0xFF1E293B)       // Slate 800

// --- AMOLED Theme Colors (True Pitch Black & Cyber Accents) ---
val MdAmoledBackground = Color(0xFF000000)         // Absolute 0% Oled Black
val MdAmoledSurface = Color(0xFF000000)            // Pitch Black
val MdAmoledSurfaceVariant = Color(0xFF111622)     // Ultra-dark elevated container
val MdAmoledOutline = Color(0xFF1E293B)
val MdAmoledOutlineVariant = Color(0xFF161E2E)

// =============================================================================
// Status Colors — Vibrant, crisp, high-contrast semantic indicators
// =============================================================================
val StatusDownloading = Color(0xFF0284C7)          // Vivid Sky Blue
val StatusDownloadingContainer = Color(0xFFE0F2FE)
val StatusDownloadingDark = Color(0xFF38BDF8)

val StatusCompleted = Color(0xFF16A34A)            // Emerald Green
val StatusCompletedContainer = Color(0xFFDCFCE7)
val StatusCompletedDark = Color(0xFF4ADE80)

val StatusPaused = Color(0xFFD97706)               // Amber
val StatusPausedContainer = Color(0xFFFEF3C7)
val StatusPausedDark = Color(0xFFFBBF24)

val StatusFailed = Color(0xFFDC2626)               // Crimson Red
val StatusFailedContainer = Color(0xFFFEE2E2)
val StatusFailedDark = Color(0xFFF87171)

val StatusQueued = Color(0xFF7C3AED)               // Electric Violet
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
