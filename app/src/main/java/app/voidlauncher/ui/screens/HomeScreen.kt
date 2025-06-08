package app.voidlauncher.ui.screens

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import app.voidlauncher.MainViewModel
import app.voidlauncher.data.Constants
import app.voidlauncher.data.HomeItem
import app.voidlauncher.data.HomeLayout
import app.voidlauncher.data.settings.AppSettings
import app.voidlauncher.helper.expandNotificationDrawer
import app.voidlauncher.ui.dialogs.ResizeAppDialog
import app.voidlauncher.ui.dialogs.ResizeWidgetDialog
import app.voidlauncher.ui.util.detectSwipeGestures
import app.voidlauncher.ui.viewmodels.SettingsViewModel
import kotlin.math.roundToInt
import app.voidlauncher.helper.isAccessServiceEnabled
import android.content.Intent
import android.provider.Settings

private fun checkAccessibilityAndLock(context: android.content.Context, viewModel: MainViewModel) {
    if (!isAccessServiceEnabled(context)) {
        Toast.makeText(context, "Enable accessibility permission to lock screen.", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } else {
        viewModel.lockScreen()
    }
}

@Composable
internal fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    appWidgetHost: AppWidgetHost,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val homeLayoutState by viewModel.homeLayoutState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()

    var showAppContextMenu by remember { mutableStateOf<HomeItem.App?>(null) }
    var appBeingMoved by remember { mutableStateOf<HomeItem.App?>(null) }

    var showWidgetContextMenu by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var showResizeDialog by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var resizeAppDialog by remember { mutableStateOf<HomeItem.App?>(null) }

    // Add state for widget movement
    var widgetBeingMoved by remember { mutableStateOf<HomeItem.Widget?>(null) }

    // Store touch position for hit testing
    var lastTouchPosition by remember { mutableStateOf(Offset.Zero) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                onSwipeUp = { when (settings.swipeUpAction) {
                    Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                    Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                    Constants.SwipeAction.APP -> viewModel.launchSwipeUpApp()
                        Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
                    Constants.SwipeAction.NULL -> {}
                        else -> onNavigateToAppDrawer()
                } },
                onSwipeDown = {
                    when (settings.swipeDownAction) {
                        Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                        Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                        Constants.SwipeAction.APP -> viewModel.launchSwipeDownApp()
                        Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
                        Constants.SwipeAction.NULL -> {}
                        else -> expandNotificationDrawer(context)
                    }
                },
                onSwipeLeft = {
                    when (settings.swipeLeftAction) {
                        Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                        Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                        Constants.SwipeAction.APP -> viewModel.launchSwipeLeftApp()
                        Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
                        Constants.SwipeAction.NULL -> { /* Do nothing */ }
                        else -> { /* Do nothing by default */ }
                    }
                },
                onSwipeRight = {
                    when (settings.swipeRightAction) {
                        Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                        Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                        Constants.SwipeAction.APP -> viewModel.launchSwipeRightApp()
                        Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
                        Constants.SwipeAction.NULL -> {}
                        else -> {}
                    }
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        when (settings.doubleTapAction) {
                            Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                            Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                            Constants.SwipeAction.APP -> viewModel.launchDoubleTapApp()
                            Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
                            Constants.SwipeAction.NULL -> {}
                            else -> {}
                        }

                    },
                    onLongPress = {
                        onNavigateToSettings()
                    },
                    onTap = {
                        when (settings.oneTapAction) {
                            Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                            Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                            Constants.SwipeAction.APP -> viewModel.launchOneTapApp()
                            Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
                            Constants.SwipeAction.NULL -> {}
                            else -> {}
                        }
                    }
                )
            }
    ) {

    }

    ResizeAppDialog(
        appItem = resizeAppDialog,
        currentRows = homeLayoutState.rows,
        currentColumns = homeLayoutState.columns,
        onDismiss = { resizeAppDialog = null },
        onResize = { app, newRowSpan, newColSpan ->
            viewModel.resizeApp(app, newRowSpan, newColSpan)
        }
    )
}

// Helper function to calculate grid position from screen position
private fun calculateGridPosition(
    position: Offset,
    homeLayout: HomeLayout,
    size: IntSize
): Pair<Int, Int>? {
    // Calculate cell size
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows

    // Calculate which grid cell was clicked
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    // Ensure the position is within grid bounds
    if (row >= 0 && row < homeLayout.rows && column >= 0 && column < homeLayout.columns) {
        return Pair(row, column)
    }

    return null
}
