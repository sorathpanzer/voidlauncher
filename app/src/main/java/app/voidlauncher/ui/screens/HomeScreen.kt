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

@Composable
fun HomeScreen(
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
                    Constants.SwipeDownAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                    Constants.SwipeDownAction.SEARCH -> onNavigateToAppDrawer()
                    Constants.SwipeDownAction.APP -> viewModel.launchSwipeUpApp()
                    Constants.SwipeDownAction.NULL -> {}
                        else -> onNavigateToAppDrawer()
                } },
                onSwipeDown = {
                    when (settings.swipeDownAction) {
                        Constants.SwipeDownAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                        Constants.SwipeDownAction.SEARCH -> onNavigateToAppDrawer()
                        Constants.SwipeDownAction.APP -> viewModel.launchSwipeDownApp()
                        Constants.SwipeDownAction.NULL -> {null}
                        else -> expandNotificationDrawer(context)
                    }
                },
                onSwipeLeft = { viewModel.launchSwipeLeftApp() },
                onSwipeRight = { viewModel.launchSwipeRightApp() }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapToLock) {
                            // Substituir lockScreen() por código para abrir a Camera
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage("app.grapheneos.camera")
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    // Caso o Signal não esteja instalado
                                    Toast.makeText(context, "A Canera não está instalado", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Erro ao abrir a Camera", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onLongPress = { offset ->
                        // Store the touch position for hit testing
                        lastTouchPosition = offset

                        // Check if we're in widget movement mode
                        if (widgetBeingMoved != null) {
                            // End movement mode
                            widgetBeingMoved = null
                            return@detectTapGestures
                        }

                        // Find which widget was long-pressed
                        val widget = findWidgetAtPosition(homeLayoutState, offset, this.size)
                        if (widget != null) {
                            // Show context menu for this widget
                            showWidgetContextMenu = widget
                        } else {
                            // Long press on empty space, go to settings
                            onNavigateToSettings()
                        }
                    },
                    onTap = { offset ->
                        // If we're in widget movement mode, handle the tap as a move destination
                        if (widgetBeingMoved != null) {
                            // Calculate the grid position from the tap location
                            val gridPosition = calculateGridPosition(offset, homeLayoutState, this.size)
                            if (gridPosition != null) {
                                // Move the widget to this position
                                viewModel.moveWidget(widgetBeingMoved!!, gridPosition.first, gridPosition.second)
                                // Exit movement mode
                                widgetBeingMoved = null
                            }
                        }
                         if (appBeingMoved != null) {
                                val gridPosition = calculateGridPosition(offset, homeLayoutState, this.size)
                                if (gridPosition != null) {
                                    viewModel.moveApp(appBeingMoved!!, gridPosition.first, gridPosition.second)
                                    appBeingMoved = null
                                }
                        }
                    }
                )
            }
    ) {
        HomeScreenContent(
            homeLayout = homeLayoutState,
            settings = settings,
            appWidgetHost = appWidgetHost,
            onAppClick = { item -> viewModel.launchApp(item.appModel) },
            onAppLongPress = { item -> showAppContextMenu = item },
            onWidgetLongPress = { item -> showWidgetContextMenu = item },
            widgetBeingMoved = widgetBeingMoved
        )

        // Show visual indicator if a widget is being moved
        if (widgetBeingMoved != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tap where you want to move the widget",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                )
            }
        }

        showAppContextMenu?.let {
            HomeAppContextMenu(
                appItem = it,
                onDismiss = { showAppContextMenu = null },
                onRemove = { app ->
                    viewModel.removeAppFromHomeScreen(app)
                    showAppContextMenu = null
                },
                onResize = { app ->
                    resizeAppDialog = app
                    showAppContextMenu = null
                },
                onMove = { app ->
                    appBeingMoved = app
                    showAppContextMenu = null
                    Toast.makeText(context, "Tap where you want to move the app", Toast.LENGTH_SHORT).show()
                }
            )
        }

    }

    WidgetContextMenu(
        widgetItem = showWidgetContextMenu,
        onDismiss = { showWidgetContextMenu = null },
        onRemove = { widget ->
            appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
            viewModel.removeWidget(widget)
            showWidgetContextMenu = null
        },
        onResize = { widget ->
            showResizeDialog = widget
            showWidgetContextMenu = null
        },
        onConfigure = { widget ->
            viewModel.requestWidgetReconfigure(widget)
            showWidgetContextMenu = null
        },
        onMove = { widget ->
            widgetBeingMoved = widget
            showWidgetContextMenu = null
            Toast.makeText(context, "Tap where you want to move the widget", Toast.LENGTH_SHORT).show()
        }
    )

    ResizeWidgetDialog(
        widgetItem = showResizeDialog,
        currentRows = homeLayoutState.rows,
        currentColumns = homeLayoutState.columns,
        onDismiss = { showResizeDialog = null },
        onResize = { widget, newRowSpan, newColSpan ->
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, newColSpan)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, newColSpan)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, newRowSpan)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, newRowSpan)
            }
            viewModel.appWidgetManager.updateAppWidgetOptions(widget.appWidgetId, options)
            viewModel.resizeWidget(widget, newRowSpan, newColSpan)
        }
    )

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
fun HomeScreenContent(
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
fun WidgetContextMenu(
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
fun HomeAppContextMenu(
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

