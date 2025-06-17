package app.voidlauncher.data.settings

import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import app.voidlauncher.data.Constants
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Setting(
    val title: String,
    val description: String = "",
    val category: SettingCategory,
    val type: SettingType,
    val dependsOn: String = "",
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val rowId: Int = -1, // <- Add this line
    val options: Array<String> = []
)

internal enum class SettingCategory(val label: String)  {
    GENERAL("GENERAL"),
    APPEARANCE("APPEARANCE"),
    SWIPE_GESTURES("SWIPE GESTURES"),
    SWIPE_2FINGERS_GESTURES("2FINGERS SWIPE GESTURES"),
    PINCH_AND_TAP_GESTURES("PINCH & TAP GESTURES"),
    SYSTEM("SYSTEM");

    override fun toString(): String = label
}

internal enum class SettingType {
    TOGGLE,
    SLIDER,
    DROPDOWN,
    BUTTON,
    APP_PICKER,
}

@Serializable
internal data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = ""
)

internal data class AppSettings(
    @Setting(
        title = "Show App Names",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val showAppNames: Boolean = false,

    val autoShowKeyboard: Boolean = true,

    @Setting(
        title = "Show Hidden in Search",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val showHiddenAppsOnSearch: Boolean = false,

    val autoOpenFilteredApp: Boolean = true,
    val searchType: Int = Constants.SearchType.STARTS_WITH,
    val appTheme: Int = AppCompatDelegate.MODE_NIGHT_YES,
    val useDynamicTheme: Boolean = false,

    @Setting(
        title = "Show Status Bar",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val statusBar: Boolean = false,

    val lockSettings: Boolean = false,
    val settingsLockPin: String = "",

    @Setting(
        title = "Search Results Font Size",
        category = SettingCategory.APPEARANCE,
        type = SettingType.SLIDER,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f
    )
    val searchResultsFontSize: Float = 1.0f,

    @Setting(
        title = "Set Plain Wallpaper",
        category = SettingCategory.APPEARANCE,
        type = SettingType.BUTTON,
        description = "Set a plain black/white wallpaper based on theme"
    )
    val plainWallpaper: Boolean = false,

    @Setting(
        title = "Swipe Up",
        category = SettingCategory.SWIPE_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 1,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val swipeUpAction: Int = Constants.SwipeAction.SEARCH,
    val swipeUpApp: AppPreference = AppPreference(),

    @Setting(
        title = "Swipe Down",
        category = SettingCategory.SWIPE_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 2,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val swipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,
    val swipeDownApp: AppPreference = AppPreference(),

    @Setting(
        title = "Swipe Right",
        category = SettingCategory.SWIPE_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 2,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val swipeRightAction: Int = Constants.SwipeAction.NULL,
    val swipeRightApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Swipe Left",
        category = SettingCategory.SWIPE_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 1,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val swipeLeftAction: Int = Constants.SwipeAction.NULL,
    val swipeLeftApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "2Finger Swipe Up",
        category = SettingCategory.SWIPE_2FINGERS_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 1,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val twoFingerSwipeUpAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeUpApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "2Finger Swipe Down",
        category = SettingCategory.SWIPE_2FINGERS_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 2,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val twoFingerSwipeDownAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeDownApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "2Finger Swipe Right",
        category = SettingCategory.SWIPE_2FINGERS_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 1,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val twoFingerSwipeRightAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeRightApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "2Finger Swipe Left",
        category = SettingCategory.SWIPE_2FINGERS_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 2,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val twoFingerSwipeLeftAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeLeftApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "One Tap",
        category = SettingCategory.PINCH_AND_TAP_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 1,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val oneTapAction: Int = Constants.SwipeAction.LOCKSCREEN,
    val oneTapApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Double Tap",
        category = SettingCategory.PINCH_AND_TAP_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 2,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val doubleTapAction: Int = Constants.SwipeAction.NULL,
    val doubleTapApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Pinch In",
        category = SettingCategory.PINCH_AND_TAP_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 1,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val pinchInAction: Int = Constants.SwipeAction.NULL,
    val pinchInApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Pinch Out",
        category = SettingCategory.PINCH_AND_TAP_GESTURES,
        type = SettingType.DROPDOWN,
        rowId = 2,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val pinchOutAction: Int = Constants.SwipeAction.NULL,
    val pinchOutApp: AppPreference = AppPreference(label = "Not set"),

    // Non-UI
    val firstOpen: Boolean = true,
    val firstOpenTime: Long = 0L,
    val firstSettingsOpen: Boolean = true,
    val firstHide: Boolean = true,
    val lockMode: Boolean = false,
    val keyboardMessage: Boolean = false,
    val renamedApps: Map<String, String> = mapOf(),
    val appLabelAlignment: Int = Gravity.START,
    val hiddenApps: Set<String> = emptySet(),
    val hiddenAppsUpdated: Boolean = false,
    val showHintCounter: Int = 1,
    val aboutClicked: Boolean = false,
    val rateClicked: Boolean = false,
    val shareShownTime: Long = 0L
) {
    companion object {
        internal fun getDefault(): AppSettings = AppSettings()
    }
}

internal class SettingsManager {

    private val orderedSettingNames = listOf(
        "showAppNames",
        "showHiddenAppsOnSearch",
        "statusBar",
        "swipeUpAction",
        "swipeUpApp",
        "swipeDownAction",
        "swipeDownApp",
        "swipeLeftAction",
        "swipeLeftApp",
        "swipeRightAction",
        "swipeRightApp",
        "twoFingerSwipeUpAction",
        "twoFingerSwipeUpApp",
        "twoFingerSwipeDownAction",
        "twoFingerSwipeDownApp",
        "twoFingerSwipeRightAction",
        "twoFingerSwipeRightApp",
        "twoFingerSwipeLeftAction",
        "twoFingerSwipeLeftApp",
        "oneTapAction",
        "oneTapApp",
        "doubleTapAction",
        "doubleTapApp",
        "pinchInAction",
        "pinchInApp",
        "pinchOutAction",
        "pinchOutApp",
        "plainWallpaper",
        "searchResultsFontSize"
    )

    private val allSettings: List<Pair<KProperty1<AppSettings, *>, Setting>> = orderedSettingNames.mapNotNull { name ->
        AppSettings::class.members
            .filterIsInstance<KProperty1<AppSettings, *>>()
            .find { it.name == name }
            ?.let { prop ->
                prop.annotations.filterIsInstance<Setting>().firstOrNull()?.let { annotation ->
                    prop to annotation
                }
            }
    }

    internal fun getSettingsByCategory(): Map<SettingCategory, List<Pair<KProperty1<AppSettings, *>, Setting>>> {
        return allSettings.groupBy { it.second.category }
    }

    internal fun updateSetting(settings: AppSettings, propertyName: String, value: Any): AppSettings {
        return when (propertyName) {
            "showAppNames" -> settings.copy(showAppNames = value as Boolean)
            "showHiddenAppsOnSearch" -> settings.copy(showHiddenAppsOnSearch = value as Boolean)
            "statusBar" -> settings.copy(statusBar = value as Boolean)
            "searchResultsFontSize" -> settings.copy(searchResultsFontSize = value as Float)
            "plainWallpaper" -> settings.copy(plainWallpaper = value as Boolean)
            "swipeUpAction" -> settings.copy(swipeUpAction = value as Int)
            "swipeUpApp" -> settings.copy(swipeUpApp = value as AppPreference)
            "swipeDownAction" -> settings.copy(swipeDownAction = value as Int)
            "swipeDownApp" -> settings.copy(swipeDownApp = value as AppPreference)
            "swipeRightAction" -> settings.copy(swipeRightAction = value as Int)
            "swipeRightApp" -> settings.copy(swipeRightApp = value as AppPreference)
            "swipeLeftAction" -> settings.copy(swipeLeftAction = value as Int)
            "swipeLeftApp" -> settings.copy(swipeLeftApp = value as AppPreference)
            "twoFingerSwipeUpAction" -> settings.copy(twoFingerSwipeUpAction = value as Int)
            "twoFingerSwipeUpApp" -> settings.copy(twoFingerSwipeUpApp = value as AppPreference)
            "twoFingerSwipeDownAction" -> settings.copy(twoFingerSwipeDownAction = value as Int)
            "twoFingerSwipeDownApp" -> settings.copy(twoFingerSwipeDownApp = value as AppPreference)
            "twoFingerSwipeRightAction" -> settings.copy(twoFingerSwipeRightAction = value as Int)
            "twoFingerSwipeRightApp" -> settings.copy(twoFingerSwipeRightApp = value as AppPreference)
            "twoFingerSwipeLeftAction" -> settings.copy(twoFingerSwipeLeftAction = value as Int)
            "twoFingerSwipeLeftApp" -> settings.copy(twoFingerSwipeLeftApp = value as AppPreference)
            "oneTapAction" -> settings.copy(oneTapAction = value as Int)
            "oneTapApp" -> settings.copy(oneTapApp = value as AppPreference)
            "doubleTapAction" -> settings.copy(doubleTapAction = value as Int)
            "doubleTapApp" -> settings.copy(doubleTapApp = value as AppPreference)
            "pinchInAction" -> settings.copy(pinchInAction = value as Int)
            "pinchInApp" -> settings.copy(pinchInApp = value as AppPreference)
            "pinchOutAction" -> settings.copy(pinchOutAction = value as Int)
            "pinchOutApp" -> settings.copy(pinchOutApp = value as AppPreference)
            else -> settings
        }
    }

    internal fun isSettingEnabled(
        settings: AppSettings,
        property: KProperty1<AppSettings, *>,
        annotation: Setting
    ): Boolean {
        val dependsOn = annotation.dependsOn
        if (dependsOn.isEmpty()) return true

        val dependency = AppSettings::class.members
            .filterIsInstance<KProperty1<AppSettings, *>>()
            .find { it.name == dependsOn }

        return when (val value = dependency?.get(settings)) {
            is Boolean -> value
            else -> true
        }
    }
}
