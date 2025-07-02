package app.voidlauncher

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private const val BLACK = 0xFF000000
private const val DARK_GRAY_0 = 0xFF121212
private const val DARK_GRAY_1 = 0xFF1D1D1D
private const val DARK_GRAY_2 = 0xFF1E1E1E
private const val DARK_GRAY_3 = 0xFF2E2E2E
private const val DARK_GRAY_4 = 0xFF2D2D2D
private const val LIGHT_GRAY = 0xFFE0E0E0
private const val MEDIUM_GRAY = 0xFFCCCCCC
private const val LIGHT_GREEN = 0xFF81C784
private const val ORANGE = 0xFFFFB74D
private const val LIGHT_BLUE = 0xFF90CAF9
private const val PINK_RED = 0xFFCF6679

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(LIGHT_BLUE), // light-blue
        onPrimary = Color(BLACK), // black text
        primaryContainer = Color(DARK_GRAY_2), // slightly lighter than background for containers
        onPrimaryContainer = Color(LIGHT_GRAY), // Light text on primary containers
        secondary = Color(LIGHT_GREEN), // light-green
        onSecondary = Color(BLACK), // black text
        secondaryContainer = Color(DARK_GRAY_3),
        onSecondaryContainer = Color(LIGHT_GRAY),
        tertiary = Color(ORANGE), // Orange
        onTertiary = Color(BLACK),
        background = Color(DARK_GRAY_0), // Deep Dark background
        onBackground = Color(LIGHT_GRAY), // Light text
        surface = Color(DARK_GRAY_1), // Dark surface
        onSurface = Color(LIGHT_GRAY),
        surfaceVariant = Color(DARK_GRAY_4), // Variant surface color
        onSurfaceVariant = Color(MEDIUM_GRAY),
        error = Color(PINK_RED), // Error color
        onError = Color(BLACK),
    )

@Composable
internal fun voidLauncherTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false // dark icons off
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
