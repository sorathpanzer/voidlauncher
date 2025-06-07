package app.voidlauncher.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * UI Events for navigation and actions
 */
internal sealed class UiEvent {
    // Navigation events
    object NavigateToAppDrawer : UiEvent()
    object NavigateToSettings : UiEvent()
    object NavigateToHiddenApps : UiEvent()
    object NavigateBack : UiEvent()

    // Dialog events
    data class ShowDialog(val dialogType: String) : UiEvent()

    data class LaunchWidgetBindIntent(val intent: Intent) : UiEvent()

    // System events
    object ResetLauncher : UiEvent()
    data class ShowToast(val message: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    data class NavigateToAppSelection(val selectionType: AppSelectionType) : UiEvent()
    data class ShowAppSelectionDialog(val selectionType: AppSelectionType) : UiEvent()

    data object NavigateToWidgetPicker : UiEvent()
    data class StartActivityForResult(val intent: Intent, val requestCode: Int) : UiEvent()

    data class ConfigureWidget(val widgetId: Int, val providerInfo: AppWidgetProviderInfo) : UiEvent()



}


/**
 * Class to manage events
 */
internal class EventsManager {
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    private suspend fun emitEvent(event: UiEvent) {
        _events.emit(event)
    }
}

internal enum class AppSelectionType {
    HOME_APP_1,
    HOME_APP_2,
    HOME_APP_3,
    HOME_APP_4,
    HOME_APP_5,
    HOME_APP_6,
    HOME_APP_7,
    HOME_APP_8,
    HOME_APP_9,
    HOME_APP_10,
    HOME_APP_11,
    HOME_APP_12,
    HOME_APP_13,
    HOME_APP_14,
    HOME_APP_15,
    HOME_APP_16,
    SWIPE_LEFT_APP,
    SWIPE_RIGHT_APP,
    DOUBLE_TAP_APP,
    SWIPE_UP_APP,
    SWIPE_DOWN_APP,
}
