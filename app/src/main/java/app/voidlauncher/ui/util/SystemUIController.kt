package app.voidlauncher.ui.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private class AppLoadingException(
    message: String,
    cause: Throwable,
) : Exception(message, cause)

// * Controls system UI elements like status bar
@Composable
internal fun systemUIController(showStatusBar: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val window = remember { (context as? Activity)?.window }

    DisposableEffect(showStatusBar) {
        if (window != null) {
            if (showStatusBar) {
                showStatusBar(window, view)
            } else {
                hideStatusBar(window, view)
            }
        }

        onDispose { }
    }
}

// * Non-composable function to safely update status bar visibility
internal fun updateStatusBarVisibility(
    activity: Activity?,
    showStatusBar: Boolean,
) {
    if (activity == null || !activity.window.isActive) return

    try {
        val window = activity.window
        val decorView = window.decorView
        updateStatusBarApi30(window, showStatusBar)
    } catch (e: IllegalStateException) {
        throw AppLoadingException("Failed to update status bar", e)
    }
}

private fun updateStatusBarApi30(
    window: Window,
    show: Boolean,
) {
    val controller = window.insetsController ?: return
    if (show) {
        controller.show(WindowInsets.Type.statusBars())
    } else {
        controller.hide(WindowInsets.Type.statusBars())
        controller.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

private fun showStatusBar(
    window: android.view.Window,
    view: View,
) {
    try {
        window.insetsController?.show(WindowInsets.Type.statusBars())
    } catch (e: IllegalStateException) {
        throw AppLoadingException("Failed to show status bar", e)
    }
}

private fun hideStatusBar(
    window: android.view.Window,
    view: View,
) {
    try {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } catch (e: IllegalStateException) {
        throw AppLoadingException("Failed to hide status bar", e)
    }
}
