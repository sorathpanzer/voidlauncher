package app.voidlauncher.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.voidlauncher.MainViewModel
import app.voidlauncher.data.repository.SettingsRepository
import app.voidlauncher.data.settings.AppSettings
import app.voidlauncher.ui.UiEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val settingsRepository = SettingsRepository(application.applicationContext)

    // UI state for settings
    private val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    // Loading state
    val Loading = mutableStateOf(true)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()


    private val _Locked = MutableStateFlow(false)
    val Locked: StateFlow<Boolean> = _Locked

    private val _showLockDialog = MutableStateFlow(false)
    val showLockDialog: StateFlow<Boolean> = _showLockDialog

    private val _SettingPin = MutableStateFlow(false)
    val SettingPin: StateFlow<Boolean> = _SettingPin

    private val _TemporarilyUnlocked = MutableStateFlow(false)
    val TemporarilyUnlocked: StateFlow<Boolean> = _TemporarilyUnlocked

    val effectiveLockState: StateFlow<Boolean> = combine(_Locked, _TemporarilyUnlocked) { locked, tempUnlocked ->
        locked && !tempUnlocked
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)



    init {
        // Load settings from repository
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _settingsState.value = settings
                Loading.value = false
                _Locked.value = settings.lockSettings
            }
        }
    }

    /**
     * Update a setting by property name
     */
    internal suspend fun updateSetting(propertyName: String, value: Any) {
        settingsRepository.updateSetting(propertyName, value)
    }

    /**
     * Emit UI event
     */
    internal fun emitEvent(event: UiEvent) {
        viewModelScope.launch {
            _eventsFlow.emit(event)
        }
    }

    internal fun setShowLockDialog(show: Boolean, SettingPin: Boolean = false) {
        _showLockDialog.value = show
        _SettingPin.value = SettingPin
    }

    internal fun validatePin(pin: String): Boolean {
        var isValid = false
        viewModelScope.launch {
            isValid = settingsRepository.validateSettingsPin(pin)
            if (isValid) {
                _TemporarilyUnlocked.value = true
                _showLockDialog.value = false
            }
        }
        return isValid
    }

    internal fun setPin(pin: String) {
        viewModelScope.launch {
            settingsRepository.setSettingsLockPin(pin)
        }
    }

    internal fun toggleLockSettings(locked: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSettingsLock(locked)
            if (!locked) {
                // When unlocking, reset the PIN to empty
                settingsRepository.setSettingsLockPin("")
            }
        }
    }

    internal fun resetUnlockState() {
        _TemporarilyUnlocked.value = false
    }
}
