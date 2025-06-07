package app.voidlauncher

import android.app.Activity.RESULT_OK
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.voidlauncher.data.*
import app.voidlauncher.data.repository.AppRepository
import app.voidlauncher.data.repository.SettingsRepository
import app.voidlauncher.data.settings.AppPreference
import app.voidlauncher.data.settings.AppSettings
import app.voidlauncher.data.settings.HomeAppPreference
import app.voidlauncher.helper.MyAccessibilityService
import app.voidlauncher.helper.getScreenDimensions
import app.voidlauncher.helper.getUserHandleFromString
import app.voidlauncher.ui.UiEvent
import app.voidlauncher.ui.AppDrawerUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.ceil

/**
 * MainViewModel is the primary ViewModel for VoidLauncher that manages app state and user interactions.
 */
internal class MainViewModel(application: Application, private val appWidgetHost: AppWidgetHost) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val settingsRepository = SettingsRepository(appContext)
    private val appRepository = AppRepository(appContext, settingsRepository, viewModelScope)

    private val REQUEST_CODE_CONFIGURE_WIDGET = 101
    private var pendingWidgetInfo: PendingWidgetInfo? = null
    data class PendingWidgetInfo(val appWidgetId: Int, val providerInfo: android.appwidget.AppWidgetProviderInfo)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()

    // UI States
    private val _homeLayoutState = MutableStateFlow(HomeLayout())
    val homeLayoutState: StateFlow<HomeLayout> = _homeLayoutState.asStateFlow()

    private val _appDrawerState = MutableStateFlow(AppDrawerUiState())
    val appDrawerState: StateFlow<AppDrawerUiState> = _appDrawerState.asStateFlow()

    // App list state
    private val _appList = MutableStateFlow<List<AppModel>>(emptyList())
    val appList: StateFlow<List<AppModel>> = _appList.asStateFlow()

    private val _appListAll = MutableStateFlow<List<AppModel>>(emptyList())
    val appListAll: StateFlow<List<AppModel>> = _appListAll.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenApps: StateFlow<List<AppModel>> = _hiddenApps.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Reset launcher state
    private val _launcherResetFailed = MutableStateFlow(false)
    val launcherResetFailed: StateFlow<Boolean> = _launcherResetFailed.asStateFlow()

    val appWidgetManager: AppWidgetManager =  AppWidgetManager.getInstance(appContext)

    init {

        viewModelScope.launch {
            appRepository.appListAll.collect { apps ->
                _appListAll.value = apps
                updateAppDrawerState()
            }
        }

        // Observe app list changes
        viewModelScope.launch {
            appRepository.appList.collect { apps ->
                _appList.value = apps
                updateAppDrawerState()
            }
        }

        // Observe hidden apps changes
        viewModelScope.launch {
            appRepository.hiddenApps.collect { apps ->
                _hiddenApps.value = apps
            }
        }

    }

    private suspend fun updateGridSize(newRows: Int, newColumns: Int) {
        val currentLayout = _homeLayoutState.value

        // Check if any items would be out of bounds with new grid size
        val itemsOutOfBounds = currentLayout.items.filter { item ->
            item.row + item.rowSpan > newRows || item.column + item.columnSpan > newColumns
        }

        if (itemsOutOfBounds.isNotEmpty()) {
            // Move out-of-bounds items to valid positions
            val updatedItems = currentLayout.items.map { item ->
                if (item.row + item.rowSpan > newRows || item.column + item.columnSpan > newColumns) {
                    // Find a new valid position for this item
                    val newPosition = findNextAvailableGridPosition(
                        currentLayout.copy(rows = newRows, columns = newColumns),
                        item.columnSpan,
                        item.rowSpan
                    )

                    when (item) {
                        is HomeItem.App -> item.copy(
                            row = newPosition?.first ?: 0,
                            column = newPosition?.second ?: 0
                        )
                        is HomeItem.Widget -> item.copy(
                            row = newPosition?.first ?: 0,
                            column = newPosition?.second ?: 0
                        )
                    }
                } else {
                    item
                }
            }

            // Save the updated layout
            val newLayout = currentLayout.copy(
                items = updatedItems,
                rows = newRows,
                columns = newColumns
            )
            settingsRepository.saveHomeLayout(newLayout)
        } else {
            // No items out of bounds, just update grid size
            val newLayout = currentLayout.copy(rows = newRows, columns = newColumns)
            settingsRepository.saveHomeLayout(newLayout)
        }
    }

    private fun addAppToHomeScreen(appModel: AppModel) {
        viewModelScope.launch {
            Log.d("HomeScreen", "Attempting to add app: ${appModel.appLabel}")
            val currentLayout = _homeLayoutState.value
            Log.d("HomeScreen", "Current layout has ${currentLayout.items.size} items")

            val nextPos = findNextAvailableGridPosition(currentLayout, 1, 1)
            Log.d("HomeScreen", "Next position: $nextPos")

            if (nextPos != null) {
                val appModelWithUserString = appModel.copy(userString = appModel.user.toString())

                val appItem = HomeItem.App(
                    appModel = appModelWithUserString,
                    row = nextPos.first,
                    column = nextPos.second
                )

                // Check if this app is already on the home screen
                val existingItem = currentLayout.items.find {
                    it is HomeItem.App && it.appModel.getKey() == appModel.getKey()
                }

                // If it exists, don't add it again
                if (existingItem == null) {
                    val newItems = currentLayout.items + appItem
                    Log.d("HomeScreen", "Adding app at position (${nextPos.first}, ${nextPos.second})")
                    settingsRepository.saveHomeLayout(currentLayout.copy(items = newItems))
                    Log.d("HomeScreen", "New layout has ${newItems.size} items")
                } else {
                    Log.d("HomeScreen", "App already exists on home screen")
                }
            } else {
                Log.d("HomeScreen", "No space available on home screen")
                _errorMessage.value = "No space available on home screen."
            }
        }
    }

    // Function to remove an app from the home screen layout
    internal fun removeAppFromHomeScreen(appItem: HomeItem.App) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val newItems = currentLayout.items.filterNot { it.id == appItem.id }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = newItems))
        }
    }

    private fun getCellSizeDp(screenWidthDp: Int, screenHeightDp: Int, rows: Int, columns: Int): Pair<Float, Float> {
        val cellWidthDp = screenWidthDp.toFloat() / columns
        val cellHeightDp = screenHeightDp.toFloat() / rows // Use available height
        return Pair(cellWidthDp, cellHeightDp)
    }

    private fun findNextAvailableGridPosition(layout: HomeLayout, widthSpan: Int, heightSpan: Int): Pair<Int, Int>? {
        val occupied = Array(layout.rows) { BooleanArray(layout.columns) }

        // Mark occupied cells
        layout.items.forEach { item ->
            for (r in item.row until (item.row + item.rowSpan).coerceAtMost(layout.rows)) {
                for (c in item.column until (item.column + item.columnSpan).coerceAtMost(layout.columns)) {
                    if (r >= 0 && c >= 0) { // Basic bounds check
                        occupied[r][c] = true
                    }
                }
            }
        }

        // Find the first available top-left corner for the required span
        for (r in 0 .. layout.rows - heightSpan) {
            for (c in 0 .. layout.columns - widthSpan) {
                if (isSpaceFreeInternal(occupied, r, c, widthSpan, heightSpan, layout.rows, layout.columns)) {
                    return Pair(r, c) // Found a spot
                }
            }
        }

        return null // No space found
    }

    private fun isSpaceFreeInternal(occupiedGrid: Array<BooleanArray>, startRow: Int, startCol: Int, spanW: Int, spanH: Int, maxRows: Int, maxCols: Int): Boolean {
        // if (startRow + spanH > maxRows || startCol + spanW > maxCols) return false

        // Check all cells within the desired span
        for (r in startRow until startRow + spanH) {
            for (c in startCol until startCol + spanW) {
                if (r >= maxRows || c >= maxCols || occupiedGrid[r][c]) {
                    return false // occupied cell
                }
            }
        }
        return true
    }


    private fun updateAppDrawerState() {
        _appDrawerState.value = _appDrawerState.value.copy(
            apps = _appList.value,
            isLoading = false
        )
    }

    internal fun startWidgetConfiguration(providerInfo: android.appwidget.AppWidgetProviderInfo) {
        viewModelScope.launch {
            try {
                @Suppress("SENSELESS_COMPARISON")
                if (providerInfo == null) {
                    Log.e("WidgetDebug", "CRITICAL: providerInfo is NULL in startWidgetConfiguration")
                    _errorMessage.value = "Internal error: Widget provider information missing."
                    return@launch
                }

                val componentName = providerInfo.provider
                if (componentName == null) {
                    Log.e("WidgetDebug", "CRITICAL: providerInfo.provider is NULL")
                    _errorMessage.value = "Internal error: Widget component name missing."
                    return@launch
                }

                Log.i("WidgetDebug", "Starting widget configuration for: ${componentName.flattenToString()}")

                // Allocate widget ID
                val appWidgetId = appWidgetHost.allocateAppWidgetId()

                // Try to bind directly
                val bindSuccess = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, componentName)

                if (bindSuccess) {
                    Log.d("WidgetDebug", "Widget binding successful for ID: $appWidgetId")

                    // Check if configuration is needed
                    if (providerInfo.configure != null) {
                        // Save pending info for result handling
                        pendingWidgetInfo = PendingWidgetInfo(appWidgetId, providerInfo)

                        // Request configuration via activity
                        emitEvent(UiEvent.ConfigureWidget(appWidgetId, providerInfo))
                    }
                    addWidgetToLayout(appWidgetId, providerInfo)
                } else {
                    Log.d("WidgetDebug", "Widget binding needs permission for ID: $appWidgetId")

                    // Save pending info
                    pendingWidgetInfo = PendingWidgetInfo(appWidgetId, providerInfo)

                    // Create binding permission intent
                    val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, componentName)
                    }

                    // Request binding via activity
                    emitEvent(UiEvent.StartActivityForResult(bindIntent, Constants.REQUEST_CODE_BIND_WIDGET))
                }
            } catch (e: Exception) {
                Log.e("WidgetDebug", "Error in startWidgetConfiguration", e)
                _errorMessage.value = "Failed to start widget configuration: ${e.message}"
            }
        }
    }

    private fun addWidgetToLayout(appWidgetId: Int, providerInfo: android.appwidget.AppWidgetProviderInfo) {
        viewModelScope.launch {
            try {
                Log.d("WidgetDebug", "Adding widget to layout: ID=$appWidgetId, Provider=${providerInfo.provider.flattenToString()}")

                // Get screen dimensions to calculate appropriate cell sizes
                val screenDimensions = getScreenDimensions(context = appContext)
                val screenWidthDp = screenDimensions.first
                val screenHeightDp = screenDimensions.second

                // Calculate widget size in cells
                val currentLayout = _homeLayoutState.value
                val cellWidthDp = screenWidthDp / currentLayout.columns
                val cellHeightDp = screenHeightDp / currentLayout.rows

                // Calculate how many cells the widget needs
                val widgetWidthCells = 1.coerceAtLeast(ceil(providerInfo.minWidth.toDouble() / cellWidthDp).toInt())
                val widgetHeightCells = 1.coerceAtLeast(ceil(providerInfo.minHeight.toDouble() / cellHeightDp).toInt())

                Log.d("WidgetDebug", "Widget size: ${providerInfo.minWidth}x${providerInfo.minHeight}dp")
                Log.d("WidgetDebug", "Cell size: ${cellWidthDp}x${cellHeightDp}dp")
                Log.d("WidgetDebug", "Widget cells: ${widgetWidthCells}x${widgetHeightCells}")

                // Find next available position
                val nextPos = findNextAvailableGridPosition(currentLayout, widgetWidthCells, widgetHeightCells)

                if (nextPos != null) {
                    // Create widget item
                    val widgetItem = HomeItem.Widget(
                        id = UUID.randomUUID().toString(),
                        appWidgetId = appWidgetId,
                        packageName = providerInfo.provider.packageName,
                        providerClassName = providerInfo.provider.className,
                        row = nextPos.first,
                        column = nextPos.second,
                        rowSpan = widgetHeightCells,
                        columnSpan = widgetWidthCells
                    )

                    // Update layout with the new widget
                    val newItems = currentLayout.items + widgetItem
                    Log.d("WidgetDebug", "Saving layout with new widget. Total items: ${newItems.size}")
                    settingsRepository.saveHomeLayout(currentLayout.copy(items = newItems))

                    // Update widget options with the size
                    val options = Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, providerInfo.minWidth)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, providerInfo.minWidth)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, providerInfo.minHeight)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, providerInfo.minHeight)
                    }
                    appWidgetManager.updateAppWidgetOptions(appWidgetId, options)

                    Log.d("WidgetDebug", "Widget added successfully to layout")

                    // Force refresh of home layout (if needed)
                    settingsRepository.triggerHomeLayoutRefresh()
                } else {
                    Log.e("WidgetDebug", "No space available for widget")
                    _errorMessage.value = "No space available for widget on home screen."
                    appWidgetHost.deleteAppWidgetId(appWidgetId) // Clean up
                }
            } catch (e: Exception) {
                Log.e("WidgetDebug", "Error adding widget to layout", e)
                _errorMessage.value = "Failed to add widget: ${e.message}"
                try {
                    appWidgetHost.deleteAppWidgetId(appWidgetId) // Clean up on error
                } catch (e2: Exception) {
                    Log.e("WidgetDebug", "Error cleaning up widget ID", e2)
                }
            }
        }
    }

private fun checkResizeValidity(layout: HomeLayout, widgetToResize: HomeItem.Widget, newRowSpan: Int, newColSpan: Int): Boolean {
        val targetRow = widgetToResize.row
        val targetCol = widgetToResize.column

        if (targetRow + newRowSpan > layout.rows || targetCol + newColSpan > layout.columns) {
            return false
        }

        for (item in layout.items) {
            if (item.id == widgetToResize.id) continue // Skip self

            val horizontalOverlap = (item.column < targetCol + newColSpan) && (item.column + item.columnSpan > targetCol)
            val verticalOverlap = (item.row < targetRow + newRowSpan) && (item.row + item.rowSpan > targetRow)

            if (horizontalOverlap && verticalOverlap) {
                return false
            }
        }
        return true
    }

    internal fun renameApp(app: AppModel, newName: String) {
        viewModelScope.launch {
            val appKey = app.getKey()
            if (newName.isBlank() || newName == app.appLabel) {
                settingsRepository.removeAppCustomName(appKey)
            } else {
                settingsRepository.setAppCustomName(appKey, newName)
            }
            // Reload apps to reflect changes
            loadApps()
        }
    }


    internal fun resizeWidget(widgetItem: HomeItem.Widget, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!checkResizeValidity(currentLayout, widgetItem, newRowSpan, newColSpan)) {
                _errorMessage.value = "Cannot resize widget: overlaps or out of bounds."
                return@launch
            }

            val newItems = currentLayout.items.map {
                if (it.id == widgetItem.id && it is HomeItem.Widget) {
                    it.copy(rowSpan = newRowSpan, columnSpan = newColSpan)
                } else {
                    it
                }
            }
            val newLayout = currentLayout.copy(items = newItems)

            // Update state immediately
            _homeLayoutState.value = newLayout

            // Update widget options
            val screenWidthDp = getScreenDimensions(context = appContext).first
            val screenHeightDp = getScreenDimensions(appContext).second
            val (cellWidthDp, cellHeightDp) = getCellSizeDp(
                screenWidthDp, screenHeightDp, currentLayout.rows, currentLayout.columns
            )

            val minWidth = (newColSpan * cellWidthDp).toInt()
            val maxWidth = minWidth
            val minHeight = (newRowSpan * cellHeightDp).toInt()
            val maxHeight = minHeight

            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)
            }

            try {
                appWidgetManager.updateAppWidgetOptions(widgetItem.appWidgetId, options)
                // Persist the changes
                settingsRepository.saveHomeLayout(newLayout)
                // Force refresh
                settingsRepository.triggerHomeLayoutRefresh()
            } catch (e: Exception) {
                Log.e("ViewModelWidget", "Failed to update widget options for ID ${widgetItem.appWidgetId}", e)
            }
        }
    }

    internal fun removeWidget(widgetItem: HomeItem.Widget) {
        viewModelScope.launch {
            try {
                // First, remove from layout
                val currentLayout = _homeLayoutState.value
                val newItems = currentLayout.items.filterNot { it.id == widgetItem.id }
                val newLayout = currentLayout.copy(items = newItems)

                // Update the layout state immediately
                _homeLayoutState.value = newLayout

                // Then delete the widget ID and persist
                appWidgetHost.deleteAppWidgetId(widgetItem.appWidgetId)
                settingsRepository.saveHomeLayout(newLayout)

                // Force a refresh of the widget host
                settingsRepository.triggerHomeLayoutRefresh()

            } catch (e: Exception) {
                Log.e("ViewModelWidget", "Error deleting widget ID ${widgetItem.appWidgetId}", e)
                _errorMessage.value = "Failed to remove widget."
            }
        }
    }


    internal fun requestWidgetReconfigure(widgetItem: HomeItem.Widget) {
        viewModelScope.launch {
            val providerInfo = getAppWidgetInfo(widgetItem.packageName, widgetItem.providerClassName)
            if (providerInfo?.configure != null) {
                try {
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = providerInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetItem.appWidgetId)
                    }
                    // Store necessary info if result needs handling (e.g., update state on success/cancel)
                    // pendingReconfigureWidgetId = widgetItem.appWidgetId
                    emitEvent(UiEvent.StartActivityForResult(configIntent, REQUEST_CODE_CONFIGURE_WIDGET)) // Use same code or new one
                } catch (e: Exception) {
                    Log.e("ViewModelWidget", "Error requesting reconfigure for ${widgetItem.appWidgetId}", e)
                    _errorMessage.value = "Failed to reconfigure widget."
                }
            } else {
                _errorMessage.value = "This widget cannot be reconfigured."
            }
        }
    }

    // Helper to get provider info at runtime
    private fun getAppWidgetInfo(packageName: String, className: String): android.appwidget.AppWidgetProviderInfo? {
        return appWidgetManager.installedProviders.find {
            it.provider.packageName == packageName && it.provider.className == className
        }
    }

    internal fun moveApp(appItem: HomeItem.App, newRow: Int, newColumn: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            if (newRow + appItem.rowSpan > currentLayout.rows ||
                newColumn + appItem.columnSpan > currentLayout.columns
            ) {
                _errorMessage.value = "Cannot move app: out of bounds"
                return@launch
            }
            val updatedItems = currentLayout.items.map { item ->
                if (item.id == appItem.id && item is HomeItem.App) {
                    item.copy(row = newRow, column = newColumn)
                } else {
                    item
                }
            }
            val newLayout = currentLayout.copy(items = updatedItems)
            settingsRepository.saveHomeLayout(newLayout)
        }
    }

    internal fun resizeApp(appItem: HomeItem.App, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val updatedItems = currentLayout.items.map { item ->
                if (item.id == appItem.id && item is HomeItem.App) {
                    item.copy(rowSpan = newRowSpan, columnSpan = newColSpan)
                } else {
                    item
                }
            }
            val newLayout = currentLayout.copy(items = updatedItems)
            settingsRepository.saveHomeLayout(newLayout)
        }
    }

    // Handle result from widget configuration Activity
    internal fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CONFIGURE_WIDGET) {
            val widgetId = pendingWidgetInfo?.appWidgetId ?: data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (resultCode == RESULT_OK) {
                    pendingWidgetInfo?.let { info ->
                        if(info.appWidgetId == widgetId) {
                            addWidgetToLayout(info.appWidgetId, info.providerInfo)
                        }
                    } ?: run {
                        // Handle reconfigure success - force widget refresh
                        Log.d("ViewModelWidget", "Widget ID $widgetId reconfigured successfully.")
                        viewModelScope.launch {
                            // Force immediate UI refresh (maybe too many refreshes?)
                            val currentLayout = _homeLayoutState.value
                            _homeLayoutState.value = currentLayout.copy() // Trigger recomposition
                            settingsRepository.triggerHomeLayoutRefresh()
                        }
                    }
                } else {
                    Log.w("ViewModelWidget", "Widget configuration cancelled/failed for ID $widgetId")
                    appWidgetHost.deleteAppWidgetId(widgetId)
                    _errorMessage.value = "Widget configuration cancelled."
                }
            }
            pendingWidgetInfo = null
        }
    }


    /**
     * Handle first open of the app
     */
    internal fun firstOpen(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFirstOpen(value)
        }
    }

    /**
     * Load all apps and visible apps
     */
    internal fun loadApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = true)
                appRepository.loadApps()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load apps: ${e.message}"
                _appDrawerState.value =
                    _appDrawerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Load hidden apps
     */
    fun getHiddenApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = true)
                appRepository.loadHiddenApps()
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load hidden apps: ${e.message}"
                _appDrawerState.value =
                    _appDrawerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Toggle app hidden state
     */
    fun toggleAppHidden(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.toggleAppHidden(app)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle app visibility: ${e.message}"
            }
        }
    }

    /**
     * Launch an app
     */
    fun launchApp(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.launchApp(app)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to launch app: ${e.message}"
            }
        }
    }

    /**
     * Handle app selection for various functions
     */
    fun selectedApp(appModel: AppModel, flag: Int) {
        when (flag) {
            Constants.FLAG_LAUNCH_APP, Constants.FLAG_HIDDEN_APPS -> {
                launchApp(appModel)
            }

            Constants.FLAG_SET_SWIPE_LEFT_APP -> {
                setSwipeLeftApp(appModel)
            }

            Constants.FLAG_SET_SWIPE_RIGHT_APP -> {
                setSwipeRightApp(appModel)
            }

            Constants.FLAG_SET_DOUBLE_TAP_APP -> {
                setDoubleTapApp(appModel)
            }

            Constants.FLAG_SET_SWIPE_UP_APP -> {
                setSwipeUpApp(appModel)
            }

            Constants.FLAG_SET_SWIPE_DOWN_APP -> {
                setSwipeDownApp(appModel)
            }
            in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_HOME_APP_16 -> {
                val position = flag - Constants.FLAG_SET_HOME_APP_1
                setHomeApp(appModel, position)

                addAppToHomeScreen(appModel)
            }
        }
    }

    private fun setHomeApp(app: AppModel, position: Int) {
        viewModelScope.launch {
            settingsRepository.setHomeApp(position, HomeAppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()))
        }
    }

    private fun setSwipeLeftApp(app: AppModel) {
        viewModelScope.launch {
            // Create AppPreference object
            val appPreference = AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            )

            // Save using the JSON serialization approach
            settingsRepository.setSwipeLeftApp(appPreference)
        }
    }

    private fun setSwipeRightApp(app: AppModel) {
        viewModelScope.launch {
            val appPreference = AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            )

            settingsRepository.setSwipeRightApp(appPreference)
        }
    }

    private fun setDoubleTapApp(app: AppModel) {
        viewModelScope.launch {
            val appPreference = AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            )

            settingsRepository.setDoubleTapApp(appPreference)
        }
    }

    fun launchSwipeUpApp() {
        viewModelScope.launch {
            val swipeUpApp = settingsRepository.settings.first().swipeUpApp
            if (swipeUpApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeUpApp.label,
                    key = null,
                    appPackage = swipeUpApp.packageName,
                    activityClassName = swipeUpApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeUpApp.userString)
                )
                launchApp(app)
            }
        }
    }

    fun launchSwipeDownApp() {
        viewModelScope.launch {
            val swipeDownApp = settingsRepository.settings.first().swipeDownApp
            if (swipeDownApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeDownApp.label,
                    key = null,
                    appPackage = swipeDownApp.packageName,
                    activityClassName = swipeDownApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeDownApp.userString)
                )
                launchApp(app)
            }
        }
    }

    fun launchSwipeLeftApp() {
        viewModelScope.launch {
            val swipeLeftApp = settingsRepository.getSwipeLeftApp()
            if (swipeLeftApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeLeftApp.label,
                    key = null,
                    appPackage = swipeLeftApp.packageName,
                    activityClassName = swipeLeftApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeLeftApp.userString)
                )
                launchApp(app)
            }
        }
    }

    fun launchSwipeRightApp() {
        viewModelScope.launch {
            val swipeRightApp = settingsRepository.getSwipeRightApp()
            if (swipeRightApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeRightApp.label,
                    key = null,
                    appPackage = swipeRightApp.packageName,
                    activityClassName = swipeRightApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeRightApp.userString)
                )
                launchApp(app)
            }
        }
    }

    fun launchDoubleTapApp() {
        viewModelScope.launch {
            val doubleTapApp = settingsRepository.getDoubleTapApp()
            if (doubleTapApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = doubleTapApp.label,
                    key = null,
                    appPackage = doubleTapApp.packageName,
                    activityClassName = doubleTapApp.activityClassName,
                    user = getUserHandleFromString(appContext, doubleTapApp.userString)
                )
                launchApp(app)
            }
        }
    }

    private fun setSwipeUpApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.setSwipeUpApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setSwipeDownApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.setSwipeDownApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    fun lockScreen() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            // if (settings.doubleTapToLock) {
                // Use accessibility service to lock screen
                val intent = Intent(appContext, MyAccessibilityService::class.java)
                intent.action = "LOCK_SCREEN"
                appContext.startService(intent)
            // }
        }
    }

    /**
     * Search apps by query
     */
    fun searchApps(query: String) {
        viewModelScope.launch {
            _appDrawerState.value = _appDrawerState.value.copy(
                searchQuery = query,
                isLoading = true
            )

            try {
                val settings = settingsRepository.settings.first()
                val searchType = settings.searchType

                val filteredApps = if (query.isBlank()) {
                    _appList.value
                } else {
                    val listToFilter = if (settings.showHiddenAppsOnSearch) appListAll else appList

                    // if (searchType) {
                        // Default startswith search
                        listToFilter.value.filter { app ->
                            app.appLabel.startsWith(query, ignoreCase = true)
                        }
                    // }
                }

                _appDrawerState.value = _appDrawerState.value.copy(
                    filteredApps = filteredApps,
                    isLoading = false
                )

                // Auto-open single match if enabled
                if (filteredApps.size == 1 && query.isNotEmpty() && settings.autoOpenFilteredApp) {
                    launchApp(filteredApps[0])
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
                _appDrawerState.value = _appDrawerState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun moveWidget(widgetItem: HomeItem.Widget, newRow: Int, newColumn: Int) {
        viewModelScope.launch {
            Log.d("WidgetDebug", "Moving widget ${widgetItem.id} from (${widgetItem.row}, ${widgetItem.column}) to ($newRow, $newColumn)")

            val currentLayout = _homeLayoutState.value

            // Check if the new position would cause the widget to go out of bounds
            if (newRow + widgetItem.rowSpan > currentLayout.rows ||
                newColumn + widgetItem.columnSpan > currentLayout.columns) {
                Log.d("WidgetDebug", "New position would cause widget to go out of bounds")
                _errorMessage.value = "Cannot move widget: would go out of bounds"
                return@launch
            }

            // Check if the new position would overlap with other items
            val wouldOverlap = currentLayout.items.any { item ->
                if (item.id == widgetItem.id) return@any false // Skip the widget being moved

                val itemEndRow = item.row + item.rowSpan
                val itemEndCol = item.column + item.columnSpan
                val newEndRow = newRow + widgetItem.rowSpan
                val newEndCol = newColumn + widgetItem.columnSpan

                // Check for overlap
                !(newRow >= itemEndRow || // Widget is below item
                        newEndRow <= item.row || // Widget is above item
                        newColumn >= itemEndCol || // Widget is to the right of item
                        newEndCol <= item.column) // Widget is to the left of item
            }

            if (wouldOverlap) {
                Log.d("WidgetDebug", "New position would overlap with existing items")
                _errorMessage.value = "Cannot move widget: would overlap with other items"
                return@launch
            }

            // Update the widget's position
            val updatedItems = currentLayout.items.map { item ->
                if (item.id == widgetItem.id && item is HomeItem.Widget) {
                    item.copy(row = newRow, column = newColumn)
                } else {
                    item
                }
            }

            // Save the updated layout
            val newLayout = currentLayout.copy(items = updatedItems)
            settingsRepository.saveHomeLayout(newLayout)

            Log.d("WidgetDebug", "Widget moved successfully")
        }
    }


    private fun fuzzyMatch(text: String, pattern: String): Boolean {
        val textLower = text.lowercase()
        val patternLower = pattern.lowercase()

        var textIndex = 0
        var patternIndex = 0

        while (textIndex < textLower.length && patternIndex < patternLower.length) {
            if (textLower[textIndex] == patternLower[patternIndex]) {
                patternIndex++
            }
            textIndex++
        }

        return patternIndex == patternLower.length
    }


    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
        _appDrawerState.value = _appDrawerState.value.copy(error = null)
    }
    
    /**
     * Emit UI event
     */
    fun emitEvent(event: UiEvent) {
        viewModelScope.launch {
            _eventsFlow.emit(event)
        }
    }
}
