package app.voidlauncher

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.voidlauncher.data.repository.SettingsRepository
import app.voidlauncher.data.settings.AppSettings

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF90CAF9), // light-blue
        onPrimary = Color(0xFF000000), // black text
        primaryContainer = Color(0xFF1E1E1E), // slightly lighter than background for containers
        onPrimaryContainer = Color(0xFFE0E0E0), // Light text on primary containers
        secondary = Color(0xFF81C784), // light-green
        onSecondary = Color(0xFF000000), // black text
        secondaryContainer = Color(0xFF2E2E2E),
        onSecondaryContainer = Color(0xFFE0E0E0),
        tertiary = Color(0xFFFFB74D), // Orange
        onTertiary = Color(0xFF000000),
        background = Color(0xFF121212), // Dark background
        onBackground = Color(0xFFE0E0E0), // Light text
        surface = Color(0xFF1D1D1D), // Dark surface
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF2D2D2D), // Variant surface color
        onSurfaceVariant = Color(0xFFCCCCCC),
        error = Color(0xFFCF6679), // Error color
        onError = Color(0xFF000000),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF1976D2), // Blue
        onPrimary = Color(0xFFE0E0E0), // light-gray text
        primaryContainer = Color(0xFFE3F2FD), // Light blue
        onPrimaryContainer = Color(0xFF0D47A1), // Dark blue
        secondary = Color(0xFF388E3C), // Green
        onSecondary = Color(0xFFE0E0E0),
        secondaryContainer = Color(0xFFE8F5E9), // Light green
        onSecondaryContainer = Color(0xFF1B5E20), // Dark green
        tertiary = Color(0xFFE65100), // Orange
        onTertiary = Color(0xFFE0E0E0), // White
        background = Color(0xFFF5F5F5), // Light
        onBackground = Color(0xFF212121),
        surface = Color(0xFFE0E0E0),
        onSurface = Color(0xFF212121),
        surfaceVariant = Color(0xFFEEEEEE),
        onSurfaceVariant = Color(0xFF616161), // Gray
        error = Color(0xFFB00020),
        onError = Color(0xFFFFFFFF),
    )

private fun defaultTypography() =
    Typography(
        displayLarge =
            TextStyle(
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = 0.sp,
            ),
        displayMedium =
            TextStyle(
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
        titleSmall =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        labelLarge =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

@Composable
private fun scaledTypography(scaleFactor: Float): Typography {
    val defaultTypo = defaultTypography()

    return Typography(
        displayLarge = defaultTypo.displayLarge.copy(fontSize = defaultTypo.displayLarge.fontSize * scaleFactor),
        displayMedium = defaultTypo.displayMedium.copy(fontSize = defaultTypo.displayMedium.fontSize * scaleFactor),
        displaySmall = defaultTypo.displaySmall.copy(fontSize = defaultTypo.displaySmall.fontSize * scaleFactor),
        headlineLarge = defaultTypo.headlineLarge.copy(fontSize = defaultTypo.headlineLarge.fontSize * scaleFactor),
        headlineMedium = defaultTypo.headlineMedium.copy(fontSize = defaultTypo.headlineMedium.fontSize * scaleFactor),
        headlineSmall = defaultTypo.headlineSmall.copy(fontSize = defaultTypo.headlineSmall.fontSize * scaleFactor),
        titleLarge = defaultTypo.titleLarge.copy(fontSize = defaultTypo.titleLarge.fontSize * scaleFactor),
        titleMedium = defaultTypo.titleMedium.copy(fontSize = defaultTypo.titleMedium.fontSize * scaleFactor),
        titleSmall = defaultTypo.titleSmall.copy(fontSize = defaultTypo.titleSmall.fontSize * scaleFactor),
        bodyLarge = defaultTypo.bodyLarge.copy(fontSize = defaultTypo.bodyLarge.fontSize * scaleFactor),
        bodyMedium = defaultTypo.bodyMedium.copy(fontSize = defaultTypo.bodyMedium.fontSize * scaleFactor),
        bodySmall = defaultTypo.bodySmall.copy(fontSize = defaultTypo.bodySmall.fontSize * scaleFactor),
        labelLarge = defaultTypo.labelLarge.copy(fontSize = defaultTypo.labelLarge.fontSize * scaleFactor),
        labelMedium = defaultTypo.labelMedium.copy(fontSize = defaultTypo.labelMedium.fontSize * scaleFactor),
        labelSmall = defaultTypo.labelSmall.copy(fontSize = defaultTypo.labelSmall.fontSize * scaleFactor),
    )
}

@Composable
internal fun voidLauncherTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }

    val settings = settingsRepository.settings.collectAsState(initial = AppSettings()).value

    val appTheme = settings.appTheme
    val darkTheme =
        when (appTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> isSystemInDarkTheme() // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

    // val dynamicColor = settings.useDynamicTheme

    val colorScheme =
        when {
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
