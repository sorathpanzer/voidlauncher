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
import app.voidlauncher.ui.composables.HomeAppItem
import app.voidlauncher.ui.composables.WidgetHostViewContainer
import app.voidlauncher.ui.composables.WidgetSizeData
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

// Helper function to find which widget was clicked
private fun findWidgetAtPosition(
    homeLayout: HomeLayout,
    position: Offset,
    size: IntSize
): HomeItem.Widget? {
    // Calculate cell size
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows

    // Calculate which grid cell was clicked
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    // Find a widget that contains this cell
    return homeLayout.items.filterIsInstance<HomeItem.Widget>().find { widget ->
        row >= widget.row &&
                row < widget.row + widget.rowSpan &&
                column >= widget.column &&
                column < widget.column + widget.columnSpan
    }
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun HomeScreenContent(
    homeLayout: HomeLayout,
    settings: AppSettings,
    appWidgetHost: AppWidgetHost,
    onAppClick: (HomeItem.App) -> Unit,
    onAppLongPress: (HomeItem.App) -> Unit,
    onWidgetLongPress: (HomeItem.Widget) -> Unit,
    widgetBeingMoved: HomeItem.Widget? = null
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentWidthDp = maxWidth
        val parentHeightDp = maxHeight

        // Define consistent padding
        val horizontalPadding = 16.dp
        val verticalPadding = 16.dp
        val usableWidth = parentWidthDp - horizontalPadding * 2
        val usableHeight = parentHeightDp - verticalPadding * 2

        val cellWidth = usableWidth / homeLayout.columns
        val cellHeight = usableHeight / homeLayout.rows

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            val refs = homeLayout.items.associate { it.id to createRef() }
            val widgetManager = AppWidgetManager.getInstance(LocalContext.current)

            homeLayout.items.forEach { item ->
                val itemModifier = Modifier.constrainAs(refs.getValue(item.id)) {

                    top.linkTo(parent.top, margin = cellHeight * item.row)
                    start.linkTo(parent.start, margin = cellWidth * item.column)

                    width = androidx.constraintlayout.compose.Dimension.value(cellWidth * item.columnSpan)
                    height = androidx.constraintlayout.compose.Dimension.value(cellHeight * item.rowSpan)
                }

                when (item) {
                    is HomeItem.App -> {
                        HomeAppItem(
                            modifier = itemModifier.padding(2.dp),
                            app = item.appModel,
                            settings = settings,
                            appWidth = cellWidth * item.columnSpan,
                            appHeight = cellHeight * item.rowSpan,
                            onClick = { onAppClick(item) },
                            onLongClick = { onAppLongPress(item) }
                        )
                    }

                    is HomeItem.Widget -> {
                        // Add visual indicator if this widget is being moved
                        val isBeingMoved = widgetBeingMoved?.id == item.id

                        val widgetModifier = if (isBeingMoved) {
                            itemModifier
                                .padding(2.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .alpha(0.7f)
                        } else {
                            itemModifier.padding(2.dp)
                        }

                        val providerInfo = remember(item.packageName, item.providerClassName) {
                            // Lookup provider info at runtime
                            widgetManager.installedProviders.find {
                                it.provider.packageName == item.packageName && it.provider.className == item.providerClassName
                            }
                        }

                        if (providerInfo != null) {
                            val sizeData = remember(
                                item.columnSpan,
                                item.rowSpan,
                                cellWidth,
                                cellHeight,
                                density
                            ) {
                                with(density) {
                                    // Calculate sizes based on determined cell dimensions
                                    val wDp = cellWidth * item.columnSpan
                                    val hDp = cellHeight * item.rowSpan
                                    WidgetSizeData(
                                        width = wDp.toPx().roundToInt(),
                                        height = hDp.toPx().roundToInt(),
                                        minWidthDp = wDp, maxWidthDp = wDp,
                                        minHeightDp = hDp, maxHeightDp = hDp
                                    )
                                }
                            }
                            WidgetHostViewContainer(
                                modifier = widgetModifier,
                                appWidgetId = item.appWidgetId,
                                providerInfo = providerInfo,
                                appWidgetHost = appWidgetHost,
                                widgetSizeData = sizeData,
                                onLongPress = { onWidgetLongPress(item) }
                            )
                        } else {
                            Box(itemModifier) { /* missing widget so...? */ }
                            Log.w(
                                "HomeScreen",
                                "Provider not found for widget ID ${item.appWidgetId}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetContextMenu(
    widgetItem: HomeItem.Widget?,
    onDismiss: () -> Unit,
    onRemove: (HomeItem.Widget) -> Unit,
    onResize: (HomeItem.Widget) -> Unit,
    onConfigure: (HomeItem.Widget) -> Unit,
    onMove: (HomeItem.Widget) -> Unit
) {
    if (widgetItem == null) return

    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    val providerInfo = remember(widgetItem) {
        widgetManager.installedProviders.find {
            it.provider.packageName == widgetItem.packageName && it.provider.className == widgetItem.providerClassName
        }
    }
    val canReconfigure = providerInfo?.configure != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget Options") },
        text = {
            Column {
                DropdownMenuItem(text = { Text("Move") }, onClick = { onMove(widgetItem); onDismiss() })
                DropdownMenuItem(text = { Text("Resize") }, onClick = { onResize(widgetItem); onDismiss() })
                if (canReconfigure) {
                    DropdownMenuItem(text = { Text("Configure") }, onClick = { onConfigure(widgetItem); onDismiss() })
                }
                DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(widgetItem); onDismiss() })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun HomeAppContextMenu(
    appItem: HomeItem.App,
    onDismiss: () -> Unit,
    onRemove: (HomeItem.App) -> Unit,
    onResize: (HomeItem.App) -> Unit,
    onMove: (HomeItem.App) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Options") },
        text = {
            Column {
                DropdownMenuItem(text = { Text("Move") }, onClick = { onMove(appItem); onDismiss() })
                DropdownMenuItem(text = { Text("Resize") }, onClick = { onResize(appItem); onDismiss() })
                DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(appItem); onDismiss() })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

