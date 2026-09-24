package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PigeonPrimaryDark,
    onPrimary = PigeonOnPrimaryDark,
    primaryContainer = PigeonPrimaryContainerDark,
    onPrimaryContainer = PigeonOnPrimaryContainerDark,
    secondary = PigeonSecondaryDark,
    onSecondary = PigeonOnSecondaryDark,
    secondaryContainer = PigeonSecondaryContainerDark,
    onSecondaryContainer = PigeonOnSecondaryContainerDark,
    tertiary = PigeonTertiaryDark,
    onTertiary = PigeonOnTertiaryDark,
    tertiaryContainer = PigeonTertiaryContainerDark,
    onTertiaryContainer = PigeonOnTertiaryContainerDark,
    background = DarkBackground,
    onBackground = Color(0xFFE2E2E6),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBCC8CF)
)

private val AmoledColorScheme = darkColorScheme(
    primary = PigeonPrimaryDark,
    onPrimary = PigeonOnPrimaryDark,
    primaryContainer = PigeonPrimaryContainerDark,
    onPrimaryContainer = PigeonOnPrimaryContainerDark,
    secondary = PigeonSecondaryDark,
    onSecondary = PigeonOnSecondaryDark,
    secondaryContainer = PigeonSecondaryContainerDark,
    onSecondaryContainer = PigeonOnSecondaryContainerDark,
    tertiary = PigeonTertiaryDark,
    onTertiary = PigeonOnTertiaryDark,
    tertiaryContainer = PigeonTertiaryContainerDark,
    onTertiaryContainer = PigeonOnTertiaryContainerDark,
    background = AmoledBackground,
    onBackground = Color(0xFFE2E2E6),
    surface = AmoledSurface,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = Color(0xFF9FB2BD)
)

private val LightColorScheme = lightColorScheme(
    primary = PigeonPrimaryLight,
    onPrimary = PigeonOnPrimaryLight,
    primaryContainer = PigeonPrimaryContainerLight,
    onPrimaryContainer = PigeonOnPrimaryContainerLight,
    secondary = PigeonSecondaryLight,
    onSecondary = PigeonOnSecondaryLight,
    secondaryContainer = PigeonSecondaryContainerLight,
    onSecondaryContainer = PigeonOnSecondaryContainerLight,
    tertiary = PigeonTertiaryLight,
    onTertiary = PigeonOnTertiaryLight,
    tertiaryContainer = PigeonTertiaryContainerLight,
    onTertiaryContainer = PigeonOnTertiaryContainerLight,
    background = LightBackground,
    onBackground = Color(0xFF191C1E),
    surface = LightSurface,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF40484C)
)

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK, AMOLED
}

@Composable
fun PegionTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.AMOLED -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        themeMode == AppThemeMode.AMOLED -> AmoledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    PegionTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}
