package app.voidlauncher.data.settings

import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import app.voidlauncher.data.Constants
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1

/**
 * Annotation for app settings
 */
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
    val options: Array<String> = []
)

/**
 * Categories for organizing settings
 */
internal enum class SettingCategory {
    GENERAL,
    APPEARANCE,
    LAYOUT,
    GESTURES,
    SYSTEM
}

/**
 * Types of settings
 */
internal enum class SettingType {
    TOGGLE,
    SLIDER,
    DROPDOWN,
    BUTTON,
    APP_PICKER,
}

/**
 * Serializable preference classes
 */
@Serializable
internal data class HomeAppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = "",
)

@Serializable
internal data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = ""
)

/**
 * Central data class for all application settings
 */
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
        title = "Swipe Down Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val swipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,

    @Setting(
        title = "Swipe Down App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
    )
    val swipeDownApp: AppPreference = AppPreference(),

    @Setting(
        title = "Swipe Up Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val swipeUpAction: Int = Constants.SwipeAction.SEARCH,

    @Setting(
        title = "Swipe Up App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
    )
    val swipeUpApp: AppPreference = AppPreference(),

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
        title = "Swipe Left Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val swipeLeftAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Left Swipe App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
        dependsOn = "swipeLeftEnabled"
    )
    val swipeLeftApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Swipe Right Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val swipeRightAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Right Swipe App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
        dependsOn = "swipeRightEnabled"
    )
    val swipeRightApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "One Tap Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val oneTapAction: Int = Constants.SwipeAction.SEARCH,

    @Setting(
        title = "One Tap App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
        dependsOn = "oneTapEnabled"
    )
    val oneTapApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Double Tap Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"]
    )
    val doubleTapAction: Int = Constants.SwipeAction.LOCKSCREEN,

    @Setting(
        title = "Double Tap App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
        dependsOn = "doubleTapEnabled"
    )
    val doubleTapApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Set Plain Wallpaper",
        category = SettingCategory.APPEARANCE,
        type = SettingType.BUTTON,
        description = "Set a plain black/white wallpaper based on theme"
    )
    val plainWallpaper: Boolean = false,

    val homeApps: List<HomeAppPreference> = List(Constants.HomeAppCount.NUM) { HomeAppPreference() },

    // Non-UI
    val firstOpen: Boolean = true,
    val firstOpenTime: Long = 0L,
    val firstSettingsOpen: Boolean = true,
    val firstHide: Boolean = true,
    val userState: String = Constants.UserState.START,
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

/**
 * Manager class using static setting list (no reflection)
 */
internal class SettingsManager {

    private val allSettings: List<Pair<KProperty1<AppSettings, *>, Setting>> = AppSettings::class.members
        .filterIsInstance<KProperty1<AppSettings, *>>()
        .mapNotNull { property ->
            property.annotations.filterIsInstance<Setting>().firstOrNull()?.let {
                property to it
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
            "swipeDownAction" -> settings.copy(swipeDownAction = value as Int)
            "swipeDownApp" -> settings.copy(swipeDownApp = value as AppPreference)
            "swipeUpAction" -> settings.copy(swipeUpAction = value as Int)
            "swipeUpApp" -> settings.copy(swipeUpApp = value as AppPreference)
            "searchResultsFontSize" -> settings.copy(searchResultsFontSize = value as Float)
            "swipeLeftAction" -> settings.copy(swipeLeftAction = value as Int)
            "swipeLeftApp" -> settings.copy(swipeLeftApp = value as AppPreference)
            "swipeRightAction" -> settings.copy(swipeRightAction = value as Int)
            "swipeRightApp" -> settings.copy(swipeRightApp = value as AppPreference)
            "oneTapAction" -> settings.copy(oneTapAction = value as Int)
            "oneTapApp" -> settings.copy(oneTapApp = value as AppPreference)
            "doubleTapAction" -> settings.copy(doubleTapAction = value as Int)
            "doubleTapApp" -> settings.copy(doubleTapApp = value as AppPreference)
            "plainWallpaper" -> settings.copy(plainWallpaper = value as Boolean)
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
