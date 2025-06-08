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

            Constants.FLAG_SET_ONE_TAP_APP -> {
                setOneTapApp(appModel)
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
        }
    }

    private fun AppModel.toPreference() = AppPreference(
        label = appLabel,
        packageName = appPackage,
        activityClassName = activityClassName,
        userString = user.toString()
    )

    private fun launchAppFromPreference(pref: AppPreference?) {
        pref?.takeIf { it.packageName.isNotEmpty() }?.let {
            val app = AppModel(
                appLabel = pref.label,
                key = null,
                appPackage = pref.packageName,
                activityClassName = pref.activityClassName,
                user = getUserHandleFromString(appContext, pref.userString)
            )
            launchApp(app)
        }
    }

    private fun setSwipeLeftApp(app: AppModel) {
        viewModelScope.launch { settingsRepository.setSwipeLeftApp(app.toPreference()) }
    }

    private fun setSwipeRightApp(app: AppModel) {
        viewModelScope.launch { settingsRepository.setSwipeRightApp(app.toPreference()) }
    }

    private fun setOneTapApp(app: AppModel) {
        viewModelScope.launch { settingsRepository.setOneTapApp(app.toPreference()) }
    }

    private fun setDoubleTapApp(app: AppModel) {
        viewModelScope.launch { settingsRepository.setDoubleTapApp(app.toPreference()) }
    }

    private fun setSwipeUpApp(app: AppModel) {
        viewModelScope.launch { settingsRepository.setSwipeUpApp(app.toPreference()) }
    }

    private fun setSwipeDownApp(app: AppModel) {
        viewModelScope.launch { settingsRepository.setSwipeDownApp(app.toPreference()) }
    }

    fun launchSwipeUpApp() {
        viewModelScope.launch { launchAppFromPreference(settingsRepository.settings.first().swipeUpApp) }
    }

    fun launchSwipeDownApp() {
        viewModelScope.launch { launchAppFromPreference(settingsRepository.settings.first().swipeDownApp) }
    }

    fun launchSwipeLeftApp() {
        viewModelScope.launch { launchAppFromPreference(settingsRepository.settings.first().swipeLeftApp) }
    }

    fun launchSwipeRightApp() {
        viewModelScope.launch { launchAppFromPreference(settingsRepository.settings.first().swipeRightApp) }
    }

    fun launchOneTapApp() {
        viewModelScope.launch { launchAppFromPreference(settingsRepository.settings.first().oneTapApp) }
    }

    fun launchDoubleTapApp() {
        viewModelScope.launch { launchAppFromPreference(settingsRepository.settings.first().doubleTapApp) }
    }

    fun lockScreen() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
                // Use accessibility service to lock screen
                val intent = Intent(appContext, MyAccessibilityService::class.java)
                intent.action = "LOCK_SCREEN"
                appContext.startService(intent)
        }
    }

    /**
     * Search apps by query
     */
    fun searchApps(query: String) {
        viewModelScope.launch {

            try {
                val settings = settingsRepository.settings.first()
                val searchType = settings.searchType

                val filteredApps = if (query.isBlank()) {
                    _appList.value
                } else {
                    val listToFilter = if (settings.showHiddenAppsOnSearch) appListAll else appList

                        // Default startswith search
                        listToFilter.value.filter { app ->
                            app.appLabel.startsWith(query, ignoreCase = true)
                        }
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
