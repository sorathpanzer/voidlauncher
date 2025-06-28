package app.voidlauncher.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.voidlauncher.data.Constants
import app.voidlauncher.data.settings.AppPreference
import app.voidlauncher.data.settings.AppSettings
import app.voidlauncher.data.settings.Setting
import app.voidlauncher.data.settings.SettingCategory
import app.voidlauncher.data.settings.SettingType
import app.voidlauncher.data.settings.SettingsManager
import app.voidlauncher.helper.isClauncherDefault
import app.voidlauncher.helper.setPlainWallpaperByTheme
import app.voidlauncher.ui.AppSelectionType
import app.voidlauncher.ui.UiEvent
import app.voidlauncher.ui.backHandler
import app.voidlauncher.ui.dialogs.settingsLockDialog
import app.voidlauncher.ui.util.updateStatusBarVisibility
import app.voidlauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.reflect.KProperty1

private const val MAX_SIZE_FILL = 0.8f
private const val ALPHA = 0.5f

// Sealed class for dialog types
private sealed class SettingsDialog {
    data class Slider(
        val property: KProperty1<AppSettings, *>,
        val annotation: Setting,
    ) : SettingsDialog()

    data class Dropdown(
        val property: KProperty1<AppSettings, *>,
        val annotation: Setting,
    ) : SettingsDialog()

    data class AppPicker(
        val property: KProperty1<AppSettings, *>,
    ) : SettingsDialog()

    data class ButtonAction(
        val property: KProperty1<AppSettings, *>,
    ) : SettingsDialog()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun settingsTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
    )
}

@Composable
internal fun settingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHiddenApps: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager() }

    // Dialog state
    var currentDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUnlockState()
        }
    }

    val effectiveLockState by viewModel.effectiveLockState.collectAsState()
    val showLockDialog by viewModel.showLockDialog.collectAsState()
    val settingPin by viewModel.settingPin.collectAsState()

    backHandler(onBack = {
        viewModel.resetUnlockState()
        onNavigateBack()
    })

    handleLockDialog(
        showLockDialog = showLockDialog,
        settingPin = settingPin,
        viewModel = viewModel,
    )

    handleCurrentDialog(
        currentDialog = currentDialog,
        uiState = uiState,
        viewModel = viewModel,
        coroutineScope = coroutineScope,
        context = context,
        onDismiss = { currentDialog = null },
    )

    Scaffold(
        topBar = { settingsTopBar(onNavigateBack) },
    ) { paddingValues ->
        if (viewModel.loading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (effectiveLockState) {
            lockedSettingsView(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                onUnlock = { viewModel.setShowLockDialog(true, false) },
            )
            return@Scaffold
        }

        settingsContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            uiState = uiState,
            settingsManager = settingsManager,
            onSettingClick = { property, annotation ->
                currentDialog =
                    when (annotation.type) {
                        SettingType.SLIDER -> SettingsDialog.Slider(property, annotation)
                        SettingType.DROPDOWN -> SettingsDialog.Dropdown(property, annotation)
                        SettingType.BUTTON -> SettingsDialog.ButtonAction(property)
                        SettingType.APP_PICKER -> SettingsDialog.AppPicker(property)
                        else -> null
                    }
            },
            viewModel = viewModel,
            context = context,
            coroutineScope = coroutineScope,
            onNavigateToHiddenApps = onNavigateToHiddenApps,
        )
    }
}

@Composable
private fun handleLockDialog(
    showLockDialog: Boolean,
    settingPin: Boolean,
    viewModel: SettingsViewModel,
) {
    if (showLockDialog) {
        settingsLockDialog(
            settingPin = settingPin,
            onDismiss = { viewModel.setShowLockDialog(false) },
            onConfirm = { pin ->
                if (settingPin) {
                    viewModel.setPin(pin)
                    viewModel.toggleLockSettings(true)
                    viewModel.setShowLockDialog(false)
                } else {
                    if (viewModel.validatePin(pin)) {
                        viewModel.setShowLockDialog(false)
                    }
                }
            },
        )
    }
}

@Composable
private fun handleCurrentDialog(
    currentDialog: SettingsDialog?,
    uiState: AppSettings,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
    context: Context,
    onDismiss: () -> Unit,
) {
    currentDialog?.let { dialog ->
        when (dialog) {
            is SettingsDialog.Slider -> {
                sliderSettingDialog(
                    title = dialog.annotation.title,
                    currentValue = getCurrentValue(dialog.property, uiState),
                    min = dialog.annotation.min,
                    max = dialog.annotation.max,
                    step = dialog.annotation.step,
                    onDismiss = onDismiss,
                    onValueSelected = { newValue ->
                        coroutineScope.launch {
                            val propertyName = dialog.property.name
                            when (dialog.property.returnType.classifier) {
                                Int::class -> viewModel.updateSetting(propertyName, newValue.toInt())
                                Float::class -> viewModel.updateSetting(propertyName, newValue)
                            }
                            onDismiss()
                        }
                    },
                )
            }

            is SettingsDialog.Dropdown -> {
                dropdownSettingDialog(
                    title = dialog.annotation.title,
                    options = dialog.annotation.options.toList(),
                    selectedIndex = dialog.property.get(uiState) as Int,
                    onDismiss = onDismiss,
                    onOptionSelected = { index ->
                        coroutineScope.launch {
                            viewModel.updateSetting(dialog.property.name, index)

                            if (dialog.property.name.endsWith("Action") &&
                                index == Constants.SwipeAction.APP
                            ) {
                                val appPropertyName = dialog.property.name.replace("Action", "App")
                                AppSettings::class
                                    .members
                                    .filterIsInstance<KProperty1<AppSettings, *>>()
                                    .firstOrNull { it.name == appPropertyName }
                                    ?.let { appProperty ->
                                        // You'll need to handle this state in the parent
                                    }
                            }
                            onDismiss()
                        }
                    },
                )
            }

            is SettingsDialog.AppPicker -> {
                LaunchedEffect(dialog) {
                    val selectionType =
                        when (dialog.property.name) {
                            "swipeLeftApp" -> AppSelectionType.SWIPE_LEFT_APP
                            "swipeRightApp" -> AppSelectionType.SWIPE_RIGHT_APP
                            "oneTapApp" -> AppSelectionType.ONE_TAP_APP
                            "doubleTapApp" -> AppSelectionType.DOUBLE_TAP_APP
                            "swipeUpApp" -> AppSelectionType.SWIPE_UP_APP
                            "swipeDownApp" -> AppSelectionType.SWIPE_DOWN_APP
                            "twoFingerSwipeUpApp" -> AppSelectionType.TWOFINGER_SWIPE_UP_APP
                            "twoFingerSwipeDownApp" -> AppSelectionType.TWOFINGER_SWIPE_DOWN_APP
                            "twoFingerSwipeLeftApp" -> AppSelectionType.TWOFINGER_SWIPE_LEFT_APP
                            "twoFingerSwipeRightApp" -> AppSelectionType.TWOFINGER_SWIPE_RIGHT_APP
                            "pinchInApp" -> AppSelectionType.PINCH_IN_APP
                            "pinchOutApp" -> AppSelectionType.PINCH_OUT_APP
                            else -> null
                        }

                    selectionType?.let {
                        viewModel.emitEvent(UiEvent.NavigateToAppSelection(it))
                        onDismiss()
                    }
                }
            }

            is SettingsDialog.ButtonAction -> {
                when (dialog.property.name) {
                    "plainWallpaper" -> {
                        setPlainWallpaperByTheme(context, appTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun lockedSettingsView(
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(MAX_SIZE_FILL),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Settings Locked",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Settings are locked",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your PIN to access settings",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onUnlock,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Unlock Settings")
                }
            }
        }
    }
}

@Composable
private fun settingsContent(
    modifier: Modifier = Modifier,
    uiState: AppSettings,
    settingsManager: SettingsManager,
    onSettingClick: (KProperty1<AppSettings, *>, Setting) -> Unit,
    viewModel: SettingsViewModel,
    context: android.content.Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onNavigateToHiddenApps: () -> Unit,
) {
    LazyColumn(modifier = modifier) {
        // Group settings by category
        val settingsByCategory = settingsManager.getSettingsByCategory()

        // Display each category
        for (category in SettingCategory.entries) {
            val categorySettings = settingsByCategory[category] ?: continue

            item {
                settingsSection(title = category.displayName()) {
                    categorySettings.forEach { (property, annotation) ->
                        settingItem(
                            property = property,
                            annotation = annotation,
                            uiState = uiState,
                            settingsManager = settingsManager,
                            onSettingClick = onSettingClick,
                            viewModel = viewModel,
                            context = context,
                            coroutineScope = coroutineScope,
                        )
                    }
                }
            }
        }

        item {
            settingsSection(title = "System") {
                systemSettings(
                    context = context,
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToHiddenApps = onNavigateToHiddenApps,
                )
            }
        }
    }
}

@Composable
private fun settingItem(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    settingsManager: SettingsManager,
    onSettingClick: (KProperty1<AppSettings, *>, Setting) -> Unit,
    viewModel: SettingsViewModel,
    context: Context,
    coroutineScope: CoroutineScope,
) {
    val isEnabled = settingsManager.isSettingEnabled(uiState, annotation)

    when (annotation.type) {
        SettingType.TOGGLE ->
            handleToggleSetting(
                property,
                annotation,
                uiState,
                isEnabled,
                viewModel,
                context,
                coroutineScope,
            )

        SettingType.SLIDER ->
            sliderSettingItem(
                property = property,
                annotation = annotation,
                uiState = uiState,
                enabled = isEnabled,
                onClick = { onSettingClick(property, annotation) },
            )

        SettingType.DROPDOWN ->
            dropdownSettingItem(
                property = property,
                annotation = annotation,
                uiState = uiState,
                enabled = isEnabled,
                onClick = { onSettingClick(property, annotation) },
            )

        SettingType.BUTTON ->
            settingsAction(
                title = annotation.title,
                description = annotation.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onSettingClick(property, annotation) },
            )

        SettingType.APP_PICKER ->
            appPickerSettingItem(
                property = property,
                annotation = annotation,
                uiState = uiState,
                enabled = isEnabled,
                onClick = { onSettingClick(property, annotation) },
            )
    }
}

@Composable
private fun handleToggleSetting(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    isEnabled: Boolean,
    viewModel: SettingsViewModel,
    context: Context,
    coroutineScope: CoroutineScope,
) {
    if (property.returnType.classifier == Boolean::class) {
        toggleSettingItem(
            title = annotation.title,
            description = annotation.description,
            isChecked = property.get(uiState) as Boolean,
            enabled = isEnabled,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    viewModel.updateSetting(property.name, checked)
                    if (property.name == "statusBar") {
                        (context as? Activity)?.let { updateStatusBarVisibility(it, checked) }
                    }
                }
            },
        )
    }
}

@Composable
private fun toggleSettingItem(
    title: String,
    description: String?,
    isChecked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
                .padding(16.dp)
                .alpha(if (enabled) 1f else ALPHA),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun sliderSettingItem(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    settingsItem(
        title = annotation.title,
        subtitle =
            when (property.returnType.classifier) {
                Int::class -> "${property.get(uiState) as Int}"
                Float::class -> String.format(Locale.getDefault(), "%.1f", property.get(uiState) as Float)
                else -> ""
            },
        description = annotation.description.takeIf { it.isNotEmpty() },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun dropdownSettingItem(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val options = annotation.options
    val value = property.get(uiState) as Int
    val displayText = options.getOrNull(value) ?: "Unknown"

    if (property.name.endsWith("Action")) {
        val appPropertyName = property.name.replace("Action", "App")
        val appProperty =
            AppSettings::class
                .members
                .filterIsInstance<KProperty1<AppSettings, *>>()
                .firstOrNull { it.name == appPropertyName }

        val appName =
            appProperty?.let {
                when (val appPref = it.get(uiState)) {
                    is AppPreference -> appPref.label
                    else -> "Select app"
                }
            } ?: "Select app"

        settingsItem(
            title = annotation.title,
            subtitle = if (value == Constants.SwipeAction.APP) "$displayText: $appName" else displayText,
            description = annotation.description.takeIf { it.isNotEmpty() },
            enabled = enabled,
            onClick = onClick,
        )
    } else {
        settingsItem(
            title = annotation.title,
            subtitle = displayText,
            description = annotation.description.takeIf { it.isNotEmpty() },
            enabled = enabled,
            onClick = onClick,
        )
    }
}

@Composable
private fun appPickerSettingItem(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val appPreference = property.get(uiState)
    val appName =
        when (appPreference) {
            is AppPreference -> appPreference.label
            else -> "Not set"
        }
    settingsItem(
        title = annotation.title,
        subtitle = appName,
        description = annotation.description.takeIf { it.isNotEmpty() },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun systemSettings(
    context: android.content.Context,
    uiState: AppSettings,
    viewModel: SettingsViewModel,
    onNavigateToHiddenApps: () -> Unit,
) {
    settingsItem(
        title = "Set as Default Launcher",
        subtitle = if (isClauncherDefault(context)) "VoidLauncher is default" else "VoidLauncher is not default",
        onClick = {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            context.startActivity(intent)
        },
        transparency = if (isClauncherDefault(context)) 0.7f else 1.0f,
    )

    settingsToggle(
        title = "Lock Settings",
        description = "Prevent changes to settings without a PIN",
        isChecked = uiState.lockSettings,
        onCheckedChange = { locked ->
            if (locked) {
                viewModel.setShowLockDialog(true, true)
            } else {
                viewModel.toggleLockSettings(false)
            }
        },
    )

    settingsItem(
        title = "Hidden Apps",
        onClick = onNavigateToHiddenApps,
    )

    settingsItem(
        title = "About VoidLauncher",
        subtitle = "Version ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
        onClick = {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(Constants.URL_ABOUT_VOIDLAUNCHER)
                }
            context.startActivity(intent)
        },
    )
}

// Helper functions
private fun getCurrentValue(
    property: KProperty1<AppSettings, *>,
    state: AppSettings,
): Float =
    when (property.returnType.classifier) {
        Int::class -> (property.get(state) as Int).toFloat()
        Float::class -> property.get(state) as Float
        else -> 0f
    }

private fun SettingCategory.displayName(): String =
    name.lowercase().replaceFirstChar {
        it.titlecase(Locale.getDefault())
    }

@Composable
private fun settingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 5.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Column {
                Text(
                    text = title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                content()
            }
        }
    }
}

@Composable
private fun settingsItem(
    title: String,
    subtitle: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    transparency: Float = 1.0f,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 8.dp)
                .alpha(if (enabled) transparency else ALPHA),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun settingsToggle(
    title: String,
    description: String? = null,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    var toggleState by remember { mutableStateOf(isChecked) }

    LaunchedEffect(isChecked) {
        toggleState = isChecked
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    toggleState = !toggleState
                    onCheckedChange(toggleState)
                }.padding(16.dp)
                .alpha(if (enabled) 1f else ALPHA),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Switch(
            checked = toggleState,
            onCheckedChange = {
                toggleState = it
                onCheckedChange(it)
            },
            enabled = enabled,
        )
    }
}

@Composable
private fun settingsAction(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .alpha(if (enabled) 1f else ALPHA),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f),
            )

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.5f),
                )
            }
        }

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.padding(start = 8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Text("Set")
        }
    }
}

@Composable
private fun sliderSettingDialog(
    title: String,
    currentValue: Float,
    min: Float,
    max: Float,
    step: Float,
    onDismiss: () -> Unit,
    onValueSelected: (Float) -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(String.format(Locale.getDefault(), "%.1f", sliderValue))
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        val steps = ((it - min) / step).toInt()
                        sliderValue = min + (steps * step)
                    },
                    valueRange = min..max,
                    steps = ((max - min) / step).toInt() - 1,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValueSelected(sliderValue)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun dropdownSettingDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit,
) {
    var selected by remember { mutableIntStateOf(selectedIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.size) { index ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selected = index }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { selected = index },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(options[index])
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onOptionSelected(selected)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
