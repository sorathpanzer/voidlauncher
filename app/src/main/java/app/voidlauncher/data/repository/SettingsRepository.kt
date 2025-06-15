package app.voidlauncher.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.voidlauncher.data.Constants
import app.voidlauncher.data.settings.AppSettings
import app.voidlauncher.data.settings.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import app.voidlauncher.data.settings.AppPreference
import app.voidlauncher.data.settings.HomeAppPreference
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.catch

// Extension property for Context to access the DataStore instance
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app.voidlauncher.settings")

/**
 * Repository for managing application settings
 */
@Suppress("NullableBooleanElvis")
internal class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false } // Configure Json instance

    private val settingsManager = SettingsManager()

    companion object {
        // Define all preference keys
        val SHOW_APP_NAMES = booleanPreferencesKey("SHOW_APP_NAMES")
        val SHOW_APP_ICONS = booleanPreferencesKey("SHOW_APP_ICONS")
        val AUTO_SHOW_KEYBOARD = booleanPreferencesKey("AUTO_SHOW_KEYBOARD")
        val SHOW_HIDDEN_APPS_IN_SEARCH = booleanPreferencesKey("SHOW_HIDDEN_APPS_IN_SEARCH")
        val AUTO_OPEN_FILTERED_APP = booleanPreferencesKey("AUTO_OPEN_FILTERED_APP")
        val SEARCH_TYPE = intPreferencesKey("SEARCH_TYPE")
        val APP_THEME = intPreferencesKey("APP_THEME")
        val TEXT_SIZE_SCALE = floatPreferencesKey("TEXT_SIZE_SCALE")
        val FONT_WEIGHT = intPreferencesKey("FONT_WEIGHT")
        val USE_SYSTEM_FONT = booleanPreferencesKey("USE_SYSTEM_FONT")
        val USE_DYNAMIC_THEME = booleanPreferencesKey("USE_DYNAMIC_THEME")
        val ICON_CORNER_RADIUS = intPreferencesKey("ICON_CORNER_RADIUS")
        val ITEM_SPACING = intPreferencesKey("ITEM_SPACING")
        val STATUS_BAR = booleanPreferencesKey("STATUS_BAR")
        val SWIPE_DOWN_ACTION = intPreferencesKey("SWIPE_DOWN_ACTION")
        val SWIPE_UP_ACTION = intPreferencesKey("SWIPE_UP_ACTION")
        val TWOFINGER_SWIPE_DOWN_ACTION = intPreferencesKey("TWOFINGER_SWIPE_DOWN_ACTION")
        val TWOFINGER_SWIPE_UP_ACTION = intPreferencesKey("TWOFINGER_SWIPE_UP_ACTION")
        val FIRST_OPEN = booleanPreferencesKey("FIRST_OPEN")
        val FIRST_OPEN_TIME = longPreferencesKey("FIRST_OPEN_TIME")
        val FIRST_SETTINGS_OPEN = booleanPreferencesKey("FIRST_SETTINGS_OPEN")
        val FIRST_HIDE = booleanPreferencesKey("FIRST_HIDE")
        val USER_STATE = stringPreferencesKey("USER_STATE")
        val LOCK_MODE = booleanPreferencesKey("LOCK_MODE")
        val KEYBOARD_MESSAGE = booleanPreferencesKey("KEYBOARD_MESSAGE")
        val PLAIN_WALLPAPER = booleanPreferencesKey("PLAIN_WALLPAPER")
        val APP_LABEL_ALIGNMENT = intPreferencesKey("APP_LABEL_ALIGNMENT")
        val HIDDEN_APPS = stringSetPreferencesKey("HIDDEN_APPS")
        val HIDDEN_APPS_UPDATED = booleanPreferencesKey("HIDDEN_APPS_UPDATED")
        val SHOW_HINT_COUNTER = intPreferencesKey("SHOW_HINT_COUNTER")
        val ABOUT_CLICKED = booleanPreferencesKey("ABOUT_CLICKED")
        val RATE_CLICKED = booleanPreferencesKey("RATE_CLICKED")
        val SHARE_SHOWN_TIME = longPreferencesKey("SHARE_SHOWN_TIME")
        val SEARCH_RESULTS_USE_HOME_FONT = booleanPreferencesKey("SEARCH_RESULTS_USE_HOME_FONT")
        val SEARCH_RESULTS_FONT_SIZE = floatPreferencesKey("SEARCH_RESULTS_FONT_SIZE")
        val SHOW_HOME_SCREEN_ICONS = booleanPreferencesKey("SHOW_HOME_SCREEN_ICONS")
        val SCALE_HOME_APPS = booleanPreferencesKey("SCALE_HOME_APPS")
        val RENAMED_APPS_JSON = stringPreferencesKey("RENAMED_APPS_JSON")

        val HOME_APPS_JSON = stringPreferencesKey("HOME_APPS_JSON")
        val SWIPE_LEFT_APP_JSON = stringPreferencesKey("SWIPE_LEFT_APP_JSON")
        val SWIPE_RIGHT_APP_JSON = stringPreferencesKey("SWIPE_RIGHT_APP_JSON")
        val ONE_TAP_APP_JSON = stringPreferencesKey("ONE_TAP_APP_JSON")
        val DOUBLE_TAP_APP_JSON = stringPreferencesKey("DOUBLE_TAP_APP_JSON")
        val SWIPE_UP_APP_JSON = stringPreferencesKey("SWIPE_UP_APP_JSON")
        val SWIPE_DOWN_APP_JSON = stringPreferencesKey("SWIPE_DOWN_APP_JSON")
        val TWOFINGER_SWIPE_UP_APP_JSON = stringPreferencesKey("TWOFINGER_SWIPE_UP_APP_JSON")
        val TWOFINGER_SWIPE_DOWN_APP_JSON = stringPreferencesKey("TWOFINGER_SWIPE_DOWN_APP_JSON")
        val TWOFINGER_SWIPE_RIGHT_APP_JSON = stringPreferencesKey("TWOFINGER_SWIPE_RIGHT_APP_JSON")
        val TWOFINGER_SWIPE_LEFT_APP_JSON = stringPreferencesKey("TWOFINGER_SWIPE_LEFT_APP_JSON")
        val PINCH_IN_APP_JSON = stringPreferencesKey("PINCH_IN_APP_JSON")
        val PINCH_OUT_APP_JSON = stringPreferencesKey("PINCH_OUT_APP_JSON")

        val HOME_LAYOUT = stringPreferencesKey("HOME_LAYOUT_JSON")

        val LOCK_SETTINGS = booleanPreferencesKey("LOCK_SETTINGS")
        val SETTINGS_LOCK_PIN = stringPreferencesKey("SETTINGS_LOCK_PIN")

        val SWIPE_LEFT_ACTION = intPreferencesKey("SWIPE_LEFT_ACTION")
        val SWIPE_RIGHT_ACTION = intPreferencesKey("SWIPE_RIGHT_ACTION")
        val TWOFINGER_SWIPE_LEFT_ACTION = intPreferencesKey("TWOFINGER_SWIPE_LEFT_ACTION")
        val TWOFINGER_SWIPE_RIGHT_ACTION = intPreferencesKey("TWOFINGER_SWIPE_RIGHT_ACTION")
        val ONE_TAP_ACTION = intPreferencesKey("ONE_TAP_ACTION")
        val DOUBLE_TAP_ACTION = intPreferencesKey("DOUBLE_TAP_ACTION")
        val PINCH_IN_ACTION = intPreferencesKey("PINCH_IN_ACTION")
        val PINCH_OUT_ACTION = intPreferencesKey("PINCH_OUT_ACTION")

        val HOME_SCREEN_ROWS = intPreferencesKey("HOME_SCREEN_ROWS")
        val HOME_SCREEN_COLUMNS = intPreferencesKey("HOME_SCREEN_COLUMNS")

        val SELECTED_ICON_PACK = stringPreferencesKey("SELECTED_ICON_PACK")


    }

    private val defaultAppSettings = AppSettings.getDefault()
    private val defaultSwipeLeftApp: AppPreference = defaultAppSettings.swipeLeftApp
    private val defaultSwipeRightApp: AppPreference = defaultAppSettings.swipeRightApp
    private val defaultOneTapApp: AppPreference = defaultAppSettings.oneTapApp
    private val defaultDoubleTapApp: AppPreference = defaultAppSettings.doubleTapApp
    private val defaultSwipeUpApp: AppPreference = defaultAppSettings.swipeUpApp
    private val defaultSwipeDownApp: AppPreference = defaultAppSettings.swipeDownApp
    private val defaultTwoFingerSwipeUpApp: AppPreference = defaultAppSettings.twoFingerSwipeUpApp
    private val defaultTwoFingerSwipeDownApp: AppPreference = defaultAppSettings.twoFingerSwipeDownApp
    private val defaultTwoFingerSwipeLeftApp: AppPreference = defaultAppSettings.twoFingerSwipeLeftApp
    private val defaultTwoFingerSwipeRightApp: AppPreference = defaultAppSettings.twoFingerSwipeRightApp
    private val defaultPinchInApp: AppPreference = defaultAppSettings.pinchInApp
    private val defaultPinchOutApp: AppPreference = defaultAppSettings.pinchOutApp

    /**
     * Flow of settings that emits whenever any setting changes
     */
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->

        val swipeLeftApp = prefs[SWIPE_LEFT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultSwipeLeftApp)
        } ?: defaultSwipeLeftApp

        val swipeRightApp = prefs[SWIPE_RIGHT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultSwipeRightApp)
        } ?: defaultSwipeRightApp

        val twoFingerSwipeLeftApp = prefs[TWOFINGER_SWIPE_LEFT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultTwoFingerSwipeLeftApp)
        } ?: defaultTwoFingerSwipeLeftApp

        val twoFingerSwipeRightApp = prefs[TWOFINGER_SWIPE_RIGHT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultTwoFingerSwipeRightApp)
        } ?: defaultTwoFingerSwipeRightApp

        val oneTapApp = prefs[ONE_TAP_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultOneTapApp)
        } ?: defaultOneTapApp

        val doubleTapApp = prefs[DOUBLE_TAP_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultDoubleTapApp)
        } ?: defaultDoubleTapApp

        val swipeUpApp = prefs[SWIPE_UP_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultSwipeUpApp)
        } ?: defaultSwipeUpApp

        val swipeDownApp = prefs[SWIPE_DOWN_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultSwipeDownApp)
        } ?: defaultSwipeDownApp

        val twoFingerSwipeUpApp = prefs[TWOFINGER_SWIPE_UP_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultTwoFingerSwipeUpApp)
        } ?: defaultTwoFingerSwipeUpApp

        val twoFingerSwipeDownApp = prefs[TWOFINGER_SWIPE_DOWN_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultTwoFingerSwipeDownApp)
        } ?: defaultTwoFingerSwipeDownApp

        val pinchInApp = prefs[PINCH_IN_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultPinchInApp)
        } ?: defaultPinchInApp

        val pinchOutApp = prefs[PINCH_OUT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultPinchOutApp)
        } ?: defaultPinchOutApp

        val renamedApps = prefs[RENAMED_APPS_JSON]?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
                Log.e("SettingsRepo", "Failed to decode renamed apps JSON: ${e.message}")
                mapOf<String, String>()
            }
        } ?: mapOf()

        AppSettings(
            // General settings
            showAppNames = prefs[SHOW_APP_NAMES] ?: false,
            showHiddenAppsOnSearch = prefs[SHOW_HIDDEN_APPS_IN_SEARCH] ?: false,
            searchType = prefs[SEARCH_TYPE] ?: Constants.SearchType.STARTS_WITH,

            // Appearance settings
            appTheme = prefs[APP_THEME] ?: AppCompatDelegate.MODE_NIGHT_YES,

            // Layout settings
            statusBar = prefs[STATUS_BAR] ?: false,

            // Gestures settings
            swipeDownAction = prefs[SWIPE_DOWN_ACTION] ?: Constants.SwipeAction.NOTIFICATIONS,
            swipeUpAction = prefs[SWIPE_UP_ACTION] ?: Constants.SwipeAction.SEARCH,
            twoFingerSwipeDownAction = prefs[TWOFINGER_SWIPE_DOWN_ACTION] ?: Constants.SwipeAction.NULL,
            twoFingerSwipeUpAction = prefs[TWOFINGER_SWIPE_UP_ACTION] ?: Constants.SwipeAction.NULL,
            twoFingerSwipeRightAction = prefs[TWOFINGER_SWIPE_RIGHT_ACTION] ?: Constants.SwipeAction.NULL,
            twoFingerSwipeLeftAction = prefs[TWOFINGER_SWIPE_LEFT_ACTION] ?: Constants.SwipeAction.NULL,
            swipeLeftAction = prefs[SWIPE_LEFT_ACTION] ?: Constants.SwipeAction.NULL,
            swipeRightAction = prefs[SWIPE_RIGHT_ACTION] ?: Constants.SwipeAction.NULL,
            oneTapAction = prefs[ONE_TAP_ACTION] ?: Constants.SwipeAction.LOCKSCREEN,
            doubleTapAction = prefs[DOUBLE_TAP_ACTION] ?: Constants.SwipeAction.NULL,
            pinchInAction = prefs[PINCH_IN_ACTION] ?: Constants.SwipeAction.NULL,
            pinchOutAction = prefs[PINCH_OUT_ACTION] ?: Constants.SwipeAction.NULL,

            lockSettings = prefs[LOCK_SETTINGS] ?: false,
            settingsLockPin = prefs[SETTINGS_LOCK_PIN] ?: "",

            // Other properties
            firstOpen = prefs[FIRST_OPEN] ?: true,
            firstOpenTime = prefs[FIRST_OPEN_TIME] ?: 0L,
            firstSettingsOpen = prefs[FIRST_SETTINGS_OPEN] ?: true,
            firstHide = prefs[FIRST_HIDE] ?: true,
            lockMode = prefs[LOCK_MODE] ?: false,
            keyboardMessage = prefs[KEYBOARD_MESSAGE] ?: false,
            plainWallpaper = prefs[PLAIN_WALLPAPER] ?: false,
            appLabelAlignment = prefs[APP_LABEL_ALIGNMENT] ?: Gravity.START,
            hiddenApps = prefs[HIDDEN_APPS] ?: emptySet(),
            hiddenAppsUpdated = prefs[HIDDEN_APPS_UPDATED] ?: false,
            showHintCounter = prefs[SHOW_HINT_COUNTER] ?: 1,
            aboutClicked = prefs[ABOUT_CLICKED] ?: false,
            searchResultsFontSize = prefs[SEARCH_RESULTS_FONT_SIZE] ?: 1.0f,

            swipeLeftApp = swipeLeftApp,
            swipeRightApp = swipeRightApp,
            oneTapApp = oneTapApp,
            doubleTapApp = doubleTapApp,
            swipeUpApp = swipeUpApp,
            swipeDownApp = swipeDownApp,
            twoFingerSwipeUpApp = twoFingerSwipeUpApp,
            twoFingerSwipeDownApp = twoFingerSwipeDownApp,
            twoFingerSwipeLeftApp = twoFingerSwipeLeftApp,
            twoFingerSwipeRightApp = twoFingerSwipeRightApp,
            pinchInApp = pinchInApp,
            pinchOutApp = pinchOutApp,
            renamedApps = renamedApps
        )
    }

    private inline fun <reified T> Json.decodeFromStringCatching(jsonString: String, default: T): T {
        return try {
            this.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to decode JSON for ${T::class.simpleName}: ${e.message}. Using default.")
            default
        }
    }

    internal suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        val currentSettings = settings.first()
        val updatedSettings = update(currentSettings)
    
        context.settingsDataStore.edit { prefs ->
            AppSettings::class.java.declaredFields.forEach { field ->
                field.isAccessible = true
                val name = field.name
                val currentValue = field.get(currentSettings)
                val newValue = field.get(updatedSettings)
    
                if (currentValue != newValue) {
                    @Suppress("UNCHECKED_CAST")
                    when (name) {
                        // General settings
                        "showAppNames" -> prefs[SHOW_APP_NAMES] = newValue as Boolean
                        "showHiddenAppsOnSearch" -> prefs[SHOW_HIDDEN_APPS_IN_SEARCH] = newValue as Boolean
                        "searchType" -> prefs[SEARCH_TYPE] = newValue as Int
    
                        // Appearance settings
                        "appTheme" -> prefs[APP_THEME] = newValue as Int
    
                        // Layout settings
                        "statusBar" -> prefs[STATUS_BAR] = newValue as Boolean
                        "showHomeScreenIcons" -> prefs[SHOW_HOME_SCREEN_ICONS] = newValue as Boolean
    
                        // Gestures settings
                        "swipeDownAction" -> prefs[SWIPE_DOWN_ACTION] = newValue as Int
                        "swipeUpAction" -> prefs[SWIPE_UP_ACTION] = newValue as Int
                        "twoFingerSwipeDownAction" -> prefs[TWOFINGER_SWIPE_DOWN_ACTION] = newValue as Int
                        "twoFingerSwipeUpAction" -> prefs[TWOFINGER_SWIPE_UP_ACTION] = newValue as Int
                        "twoFingerSwipeRightAction" -> prefs[TWOFINGER_SWIPE_RIGHT_ACTION] = newValue as Int
                        "twoFingerSwipeLeftAction" -> prefs[TWOFINGER_SWIPE_LEFT_ACTION] = newValue as Int
                        "swipeLeftAction" -> prefs[SWIPE_LEFT_ACTION] = newValue as Int
                        "swipeRightAction" -> prefs[SWIPE_RIGHT_ACTION] = newValue as Int
                        "oneTapAction" -> prefs[ONE_TAP_ACTION] = newValue as Int
                        "doubleTapAction" -> prefs[DOUBLE_TAP_ACTION] = newValue as Int
                        "pinchInAction" -> prefs[PINCH_IN_ACTION] = newValue as Int
                        "pinchOutAction" -> prefs[PINCH_OUT_ACTION] = newValue as Int
    
                        // Search result appearance
                        "searchResultsFontSize" -> prefs[SEARCH_RESULTS_FONT_SIZE] = newValue as Float
    
                        "lockSettings" -> prefs[LOCK_SETTINGS] = newValue as Boolean
                        "settingsLockPin" -> prefs[SETTINGS_LOCK_PIN] = newValue as String
    
                        // Other properties
                        "firstOpen" -> prefs[FIRST_OPEN] = newValue as Boolean
                        "firstOpenTime" -> prefs[FIRST_OPEN_TIME] = newValue as Long
                        "firstSettingsOpen" -> prefs[FIRST_SETTINGS_OPEN] = newValue as Boolean
                        "firstHide" -> prefs[FIRST_HIDE] = newValue as Boolean
                        "lockMode" -> prefs[LOCK_MODE] = newValue as Boolean
                        "keyboardMessage" -> prefs[KEYBOARD_MESSAGE] = newValue as Boolean
                        "plainWallpaper" -> prefs[PLAIN_WALLPAPER] = newValue as Boolean
                        "appLabelAlignment" -> prefs[APP_LABEL_ALIGNMENT] = newValue as Int
                        "hiddenAppsUpdated" -> prefs[HIDDEN_APPS_UPDATED] = newValue as Boolean
                        "showHintCounter" -> prefs[SHOW_HINT_COUNTER] = newValue as Int
                        "aboutClicked" -> prefs[ABOUT_CLICKED] = newValue as Boolean
    
                        // Special handling for complex types
                        "hiddenApps" -> prefs[HIDDEN_APPS] = newValue as Set<String>
    
                        "swipeLeftApp" -> prefs[SWIPE_LEFT_APP_JSON] = json.encodeToString(newValue)
                        "swipeRightApp" -> prefs[SWIPE_RIGHT_APP_JSON] = json.encodeToString(newValue)
                        "oneTapApp" -> prefs[ONE_TAP_APP_JSON] = json.encodeToString(newValue)
                        "doubleTapApp" -> prefs[DOUBLE_TAP_APP_JSON] = json.encodeToString(newValue)
                        "swipeUpApp" -> prefs[SWIPE_UP_APP_JSON] = json.encodeToString(newValue)
                        "swipeDownApp" -> prefs[SWIPE_DOWN_APP_JSON] = json.encodeToString(newValue)
                        "twoFingerSwipeUpApp" -> prefs[TWOFINGER_SWIPE_UP_APP_JSON] = json.encodeToString(newValue)
                        "twoFingerSwipeDownApp" -> prefs[TWOFINGER_SWIPE_DOWN_APP_JSON] = json.encodeToString(newValue)
                        "twoFingerSwipeLeftApp" -> prefs[TWOFINGER_SWIPE_LEFT_APP_JSON] = json.encodeToString(newValue)
                        "twoFingerSwipeRightApp" -> prefs[TWOFINGER_SWIPE_RIGHT_APP_JSON] = json.encodeToString(newValue)
                        "pinchInApp" -> prefs[PINCH_IN_APP_JSON] = json.encodeToString(newValue)
                        "pinchOutApp" -> prefs[PINCH_OUT_APP_JSON] = json.encodeToString(newValue)
                        "renamedApps" -> prefs[RENAMED_APPS_JSON] = json.encodeToString(newValue)
    
                        // Add other fields if needed
    
                        else -> {
                            // Unknown property - optionally log or ignore
                        }
                    }
                }
            }
        }
    }

    /**
     * Update a setting by property name
     */
    internal suspend fun updateSetting(propertyName: String, value: Any) {
        val currentSettings = settings.first()
        val updatedSettings = settingsManager.updateSetting(currentSettings, propertyName, value)
        updateSetting { updatedSettings }
    }

    /**
     * Methods for managing other settable apps
     */
    internal suspend fun setSwipeLeftApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[SWIPE_LEFT_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setSwipeRightApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[SWIPE_RIGHT_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setOneTapApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[ONE_TAP_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setDoubleTapApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[DOUBLE_TAP_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setSwipeUpApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[SWIPE_UP_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setSwipeDownApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[SWIPE_DOWN_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setTwoFingerSwipeUpApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[TWOFINGER_SWIPE_UP_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setTwoFingerSwipeDownApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[TWOFINGER_SWIPE_DOWN_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setTwoFingerSwipeLeftApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[TWOFINGER_SWIPE_LEFT_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setTwoFingerSwipeRightApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[TWOFINGER_SWIPE_RIGHT_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setPinchInApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[PINCH_IN_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun setPinchOutApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[PINCH_OUT_APP_JSON] = json.encodeToString(app)
        }
    }

    internal suspend fun getSwipeLeftApp(): AppPreference {
        return settings.first().swipeLeftApp
    }

    internal suspend fun getSwipeRightApp(): AppPreference {
        return settings.first().swipeRightApp
    }

    internal suspend fun getOneTapApp(): AppPreference {
        return settings.first().oneTapApp
    }

    internal suspend fun getDoubleTapApp(): AppPreference {
        return settings.first().doubleTapApp
    }

    internal suspend fun setSettingsLock(locked: Boolean) {
        updateSetting { it.copy(lockSettings = locked) }
    }

    internal suspend fun setSettingsLockPin(pin: String) {
        updateSetting { it.copy(settingsLockPin = pin) }
    }

    internal suspend fun validateSettingsPin(pin: String): Boolean {
        return settings.first().settingsLockPin == pin
    }

    /**
     * Methods for managing hidden apps
     */
    internal suspend fun toggleAppHidden(packageKey: String) {
        updateSetting {
            val updatedHiddenApps = it.hiddenApps.toMutableSet()
            if (updatedHiddenApps.contains(packageKey)) {
                updatedHiddenApps.remove(packageKey)
            } else {
                updatedHiddenApps.add(packageKey)
            }
            it.copy(hiddenApps = updatedHiddenApps)
        }
    }

    internal suspend fun setFirstOpen(value: Boolean) {
        updateSetting { it.copy(firstOpen = value) }
    }

    private suspend fun setAppTheme(value: Int) {
        updateSetting { it.copy(appTheme = value) }
    }

    internal suspend fun setAppCustomName(appKey: String, customName: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()

        if (customName.isBlank()) {
            updatedRenamedApps.remove(appKey)
        } else {
            updatedRenamedApps[appKey] = customName
        }

        context.settingsDataStore.edit { prefs ->
            prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedRenamedApps)
        }
    }

    internal suspend fun removeAppCustomName(appKey: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()
        updatedRenamedApps.remove(appKey)

        context.settingsDataStore.edit { prefs ->
            prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedRenamedApps)
        }
    }


}
