package app.voidlauncher.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
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
import app.voidlauncher.ui.BackHandler
import app.voidlauncher.ui.UiEvent
import app.voidlauncher.ui.util.updateStatusBarVisibility
import app.voidlauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.reflect.KProperty1
import app.voidlauncher.ui.dialogs.SettingsLockDialog
import kotlinx.coroutines.CoroutineScope
import android.content.Context
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHiddenApps: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager() }

    var showingDialog by remember { mutableStateOf<SettingsDialogType?>(null) }
    var currentProperty by remember { mutableStateOf<KProperty1<AppSettings, *>?>(null) }
    var currentAnnotation by remember { mutableStateOf<Setting?>(null) }

    val effectiveLockState by viewModel.effectiveLockState.collectAsState()
    val showLockDialog by viewModel.showLockDialog.collectAsState()
    val SettingPin by viewModel.SettingPin.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetUnlockState() }
    }

    BackHandler {
        viewModel.resetUnlockState()
        onNavigateBack()
    }

    if (showLockDialog) {
        SettingsLockDialog(
            SettingPin = SettingPin,
            onDismiss = { viewModel.setShowLockDialog(false) },
            onConfirm = { pin ->
                if (SettingPin) {
                    viewModel.setPin(pin)
                    viewModel.toggleLockSettings(true)
                } else if (viewModel.validatePin(pin)) {
                    viewModel.setShowLockDialog(false)
                }
            }
        )
    }

    ShowSettingsDialog(
        dialogType = showingDialog,
        currentProperty = currentProperty,
        currentAnnotation = currentAnnotation,
        uiState = uiState,
        coroutineScope = coroutineScope,
        viewModel = viewModel,
        context = context,
        onDismiss = { showingDialog = null }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            viewModel.Loading.value -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            effectiveLockState -> {
                Box(
                    Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Card(Modifier.padding(16.dp).fillMaxWidth(0.8f)) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Settings are locked", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text("Enter your PIN to access settings", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.setShowLockDialog(true, false) }, Modifier.fillMaxWidth()) {
                                Text("Unlock Settings")
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize().padding(paddingValues)) {
            val settingsByCategory = settingsManager.getSettingsByCategory()
            for (category in SettingCategory.entries) {
                val entries = settingsByCategory[category] ?: continue
                item {
                    SettingsSection(title = category.toString().replaceFirstChar { it.titlecase(Locale.getDefault()) }) {
                        entries.forEach { (prop, ann) ->
                            val enabled = settingsManager.isSettingEnabled(uiState, prop, ann)
                            when (ann.type) {
                                SettingType.TOGGLE -> {
                                        SettingsToggle(
                                            title = ann.title,
                                            description = ann.description.ifBlank { null },
                                            isChecked = prop.get(uiState) as Boolean,
                                            enabled = enabled
                                        ) { checked ->
                                            coroutineScope.launch { viewModel.updateSetting(prop.name, checked) }
                                        }
                                }
                                SettingType.SLIDER -> {
                                    SettingsItem(
                                        title = ann.title,
                                        subtitle = when (prop.returnType.classifier) {
                                            Int::class -> "${prop.get(uiState)}"
                                            Float::class -> String.format(Locale.getDefault(), "%.1f", prop.get(uiState))
                                            else -> ""
                                        },
                                        description = ann.description.ifBlank { null },
                                        enabled = enabled,
                                        onClick = {
                                            currentProperty = prop
                                            currentAnnotation = ann
                                            showingDialog = SettingsDialogType.SLIDER
                                        }
                                    )
                                }
                                SettingType.DROPDOWN -> {
                                    if (ann.options.isNotEmpty() && prop.returnType.classifier == Int::class) {
                                        val value = prop.get(uiState) as Int
                                        val display = ann.options.getOrElse(value) { "Unknown" }
                                        SettingsItem(
                                            title = ann.title,
                                            subtitle = if (prop.name.endsWith("Action") && value == Constants.SwipeAction.APP) {
                                                val appName = (AppSettings::class.members
                                                    .firstOrNull { it.name == prop.name.replace("Action", "App") }
                                                    ?.call(uiState) as? AppPreference)?.label ?: "Select app"
                                                "$display: $appName"
                                            } else {
                                                display
                                            },
                                            description = ann.description.ifBlank { null },
                                            enabled = enabled,
                                            onClick = {
                                                currentProperty = prop
                                                currentAnnotation = ann
                                                showingDialog = SettingsDialogType.DROPDOWN
                                            }
                                        )
                                    }
                                }
                                SettingType.BUTTON -> {
                                    SettingsAction(
                                        title = ann.title,
                                        description = ann.description.ifBlank { null },
                                        enabled = enabled,
                                        onClick = {
                                            currentProperty = prop
                                            showingDialog = SettingsDialogType.BUTTON
                                        }
                                    )
                                }
                                SettingType.APP_PICKER -> {
                                    val appName = (prop.get(uiState) as? AppPreference)?.label ?: "Not set"
                                    SettingsItem(
                                        title = ann.title,
                                        subtitle = appName,
                                        description = ann.description.ifBlank { null },
                                        enabled = enabled,
                                        onClick = {
                                            currentProperty = prop
                                            showingDialog = SettingsDialogType.APP_PICKER
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "SYSTEM") {
                    SettingsToggle(
                        title = "Lock Settings",
                        description = "Prevent changes to settings without a PIN",
                        isChecked = uiState.lockSettings,
                        onCheckedChange = { locked ->
                            if (locked) viewModel.setShowLockDialog(true, true)
                            else viewModel.toggleLockSettings(false)
                        }
                    )

                    SettingsItem(title = "Hidden Apps", onClick = onNavigateToHiddenApps)

                    SettingsItem(
                        title = "About VoidLauncher",
                        subtitle = "Version ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_ABOUT_VOIDLAUNCHER)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

private enum class SettingsDialogType { SLIDER, DROPDOWN, APP_PICKER, BUTTON }

@Composable
private fun ShowSettingsDialog(
    dialogType: SettingsDialogType?,
    currentProperty: KProperty1<AppSettings, *>?,
    currentAnnotation: Setting?,
    uiState: AppSettings,
    coroutineScope: CoroutineScope,
    viewModel: SettingsViewModel,
    context: Context,
    onDismiss: () -> Unit
) {
    when (dialogType) {
        SettingsDialogType.SLIDER -> currentProperty?.takeIf { currentAnnotation != null }?.let { prop ->
            val ann = currentAnnotation!!
            val initialValue = when (prop.returnType.classifier) {
                Int::class -> (prop.get(uiState) as Int).toFloat()
                Float::class -> prop.get(uiState) as Float
                else -> 0f
            }
            SliderSettingDialog(
                title = ann.title,
                currentValue = initialValue,
                min = ann.min,
                max = ann.max,
                step = ann.step,
                onDismiss = onDismiss
            ) { newValue ->
                coroutineScope.launch {
                    val final = if (prop.returnType.classifier == Int::class) newValue.toInt() else newValue
                    viewModel.updateSetting(prop.name, final)
                    onDismiss()
                }
            }
        }

        SettingsDialogType.DROPDOWN -> currentProperty?.takeIf { currentAnnotation != null }?.let { prop ->
            val ann = currentAnnotation!!
            val index = prop.get(uiState) as? Int ?: 0
            DropdownSettingDialog(
                title = ann.title,
                options = ann.options.toList(),
                selectedIndex = index,
                onDismiss = onDismiss
            ) { newIndex ->
                coroutineScope.launch {
                    viewModel.updateSetting(prop.name, newIndex)
                    if (prop.name.endsWith("Action") && newIndex == Constants.SwipeAction.APP) {
                        AppSettings::class.members
                            .filterIsInstance<KProperty1<AppSettings, *>>()
                            .firstOrNull { it.name == prop.name.replace("Action", "App") }
                            ?.let { appProp ->
                                viewModel.emitEvent(UiEvent.NavigateToAppSelection(propNameToSelection(appProp.name)))
                            }
                    }
                    onDismiss()
                }
            }
        }

        SettingsDialogType.APP_PICKER -> currentProperty?.let { prop ->
            coroutineScope.launch {
                viewModel.emitEvent(UiEvent.NavigateToAppSelection(propNameToSelection(prop.name)))
                onDismiss()
            }
        }

        SettingsDialogType.BUTTON -> currentProperty?.let { prop ->
            if (prop.name == "plainWallpaper") {
                setPlainWallpaperByTheme(context, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            onDismiss()
        }

        null -> Unit
    }
}

private fun propNameToSelection(name: String) = when (name) {
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
    else -> error("Unknown app picker property")
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 5.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                    Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    transparency: Float = 1.0f
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(if (enabled) transparency else 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String? = null,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    var toggleState by remember { mutableStateOf(isChecked) }

    LaunchedEffect(isChecked) {
        toggleState = isChecked
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                toggleState = !toggleState
                onCheckedChange(toggleState)
            }
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Switch(
            checked = toggleState,
            onCheckedChange = {
                toggleState = it
                onCheckedChange(it)
            },
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsAction(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
            )

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.5f)
                )
            }
        }

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.padding(start = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Set")
        }
    }
}

@Composable
private fun SliderSettingDialog(
    title: String,
    currentValue: Float,
    min: Float,
    max: Float,
    step: Float,
    onDismiss: () -> Unit,
    onValueSelected: (Float) -> Unit
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
                    steps = ((max - min) / step).toInt() - 1
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
        }
    )
}

@Composable
private fun DropdownSettingDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(selectedIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = index }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { selected = index }
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
        }
    )
}
