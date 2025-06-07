package app.voidlauncher.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val isLoading = mutableStateOf(true)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()


    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    private val _showLockDialog = MutableStateFlow(false)
    val showLockDialog: StateFlow<Boolean> = _showLockDialog

    private val _isSettingPin = MutableStateFlow(false)
    val isSettingPin: StateFlow<Boolean> = _isSettingPin

    private val _isTemporarilyUnlocked = MutableStateFlow(false)
    val isTemporarilyUnlocked: StateFlow<Boolean> = _isTemporarilyUnlocked

    val effectiveLockState: StateFlow<Boolean> = combine(_isLocked, _isTemporarilyUnlocked) { locked, tempUnlocked ->
        locked && !tempUnlocked
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)



    init {
        // Load settings from repository
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _settingsState.value = settings
                isLoading.value = false
                _isLocked.value = settings.lockSettings
            }
        }
    }

    /**
     * Update a setting by property name
     */
    internal suspend fun updateSetting(propertyName: String, value: Any) {
        settingsRepository.updateSetting(propertyName, value)
    }

    internal suspend fun updateGridSize(propertyName: String, newValue: Int) {
        val currentSettings = settingsState.value

        updateSetting(propertyName, newValue)

    }

    /**
     * Emit UI event
     */
    internal fun emitEvent(event: UiEvent) {
        viewModelScope.launch {
            _eventsFlow.emit(event)
        }
    }

    internal fun setShowLockDialog(show: Boolean, isSettingPin: Boolean = false) {
        _showLockDialog.value = show
        _isSettingPin.value = isSettingPin
    }

    internal fun validatePin(pin: String): Boolean {
        var isValid = false
        viewModelScope.launch {
            isValid = settingsRepository.validateSettingsPin(pin)
            if (isValid) {
//                _isLocked.value = false
                _isTemporarilyUnlocked.value = true
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
        _isTemporarilyUnlocked.value = false
    }
}
