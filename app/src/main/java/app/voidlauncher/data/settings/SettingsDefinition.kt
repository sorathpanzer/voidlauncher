package app.voidlauncher.data.settings

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import app.voidlauncher.data.Constants
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Annotation for app settings
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Setting(
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
enum class SettingCategory {
    GENERAL,
    APPEARANCE,
    LAYOUT,
    GESTURES,
    SYSTEM
}

/**
 * Types of settings
 */
enum class SettingType {
    TOGGLE,
    SLIDER,
    DROPDOWN,
    BUTTON,
//    COLOR_PICKER,
    APP_PICKER,
    ICON_PACK_PICKER
}

/**
 * Central data class for all application settings
 */
data class AppSettings(

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

    // Appearance settings
    @Setting(
        title = "Theme",
        category = SettingCategory.APPEARANCE,
        type = SettingType.DROPDOWN,
        options = ["System", "Light", "Dark"]
    )
    val appTheme: Int = AppCompatDelegate.MODE_NIGHT_YES,

    @Setting(
        title = "Text Size",
        category = SettingCategory.APPEARANCE,
        type = SettingType.SLIDER,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f
    )
    val textSizeScale: Float = 1.0f,

    @Setting(
        title = "Use Dynamic Theme",
        category = SettingCategory.APPEARANCE,
        type = SettingType.TOGGLE
    )
    val useDynamicTheme: Boolean = false,

    @Setting(
        title = "Show Status Bar",
        category = SettingCategory.LAYOUT,
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
        type = SettingType.APP_PICKER
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



    val homeApps: List<HomeAppPreference> = List(Constants.HomeAppCount.NUM) { HomeAppPreference() }, // Changed from NUM to actual count needed, ensure constant is correct


    // Non-UI settings (not annotated)
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
        // Helper method to get default settings
        fun getDefault(): AppSettings = AppSettings()
    }
}

/**
 * Manager class that handles settings reflection and organization
 */
class SettingsManager {
    /**
     * Get all settings properties with their annotations
     */
    fun getAllSettings(): List<Pair<KProperty1<AppSettings, *>, Setting>> {
        return AppSettings::class.memberProperties
            .mapNotNull { property ->
                val annotation = property.findAnnotation<Setting>()
                if (annotation != null) {
                    property to annotation
                } else {
                    null
                }
            }
    }

    /**
     * Get settings grouped by category
     */
    fun getSettingsByCategory(): Map<SettingCategory, List<Pair<KProperty1<AppSettings, *>, Setting>>> {
        return getAllSettings().groupBy { it.second.category }
    }

    /**
     * Get a setting value from an AppSettings instance
     */
    @Suppress("unused")
    fun getSettingValue(settings: AppSettings, property: KProperty1<AppSettings, *>): Any? {
        return property.get(settings)
    }

    /**
     * Create a new AppSettings instance with an updated value for a property
     */
    fun updateSetting(settings: AppSettings, propertyName: String, value: Any): AppSettings {
        // Create a mutable map of all current property values
        val propertyMap = mutableMapOf<String, Any?>()

        // Fill the map with all current property values
        AppSettings::class.memberProperties.forEach { prop ->
            propertyMap[prop.name] = prop.get(settings)
        }

        // Update the specific property
        propertyMap[propertyName] = value

        // Create a new instance with the updated property
        val constructor = AppSettings::class.constructors.first()
        val parameters = constructor.parameters

        // Map parameter names to values, using the updated property value where applicable
        val parameterValues = parameters.associateWith { param ->
            propertyMap[param.name]
        }

        // Create a new instance with the updated values
        return constructor.callBy(parameterValues)
    }

    /**
     * Check if a setting is enabled based on its dependencies
     */
    fun isSettingEnabled(
        settings: AppSettings,
        property: KProperty1<AppSettings, *>,
        annotation: Setting
    ): Boolean {
        val dependsOn = annotation.dependsOn

        if (dependsOn.isEmpty()) {
            return true
        }

        val dependencyProperty = AppSettings::class.memberProperties.find { it.name == dependsOn }
        return if (dependencyProperty != null) {
            val dependencyValue = dependencyProperty.get(settings)
            when (dependencyValue) {
                is Boolean -> dependencyValue
                else -> true
            }
        } else {
            true
        }
    }
}

@Serializable
data class HomeAppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = "",
)

@Serializable
data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = ""
)
