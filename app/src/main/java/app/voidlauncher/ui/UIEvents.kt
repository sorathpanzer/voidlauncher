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
    SWIPE_LEFT_APP,
    SWIPE_RIGHT_APP,
    ONE_TAP_APP,
    DOUBLE_TAP_APP,
    SWIPE_UP_APP,
    SWIPE_DOWN_APP,
    TWOFINGER_SWIPE_UP_APP,
    TWOFINGER_SWIPE_DOWN_APP,
    TWOFINGER_SWIPE_LEFT_APP,
    TWOFINGER_SWIPE_RIGHT_APP,
    PINCH_IN_APP,
    PINCH_OUT_APP,
}
