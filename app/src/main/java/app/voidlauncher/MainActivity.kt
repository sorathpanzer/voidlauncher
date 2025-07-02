package app.voidlauncher

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.voidlauncher.data.Navigation
import app.voidlauncher.data.repository.SettingsRepository
import app.voidlauncher.helper.setPlainWallpaper
import app.voidlauncher.ui.UiEvent
import app.voidlauncher.ui.viewmodels.SettingsViewModel
import app.voidlauncher.ui.voidlauncherNavigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val APP_WIDGETHOST_ID = 1024

internal class MainActivity : ComponentActivity() {
    private val viewModel by lazy { ViewModelProvider(this)[MainViewModel::class.java] }
    private val settingsViewModel by lazy { ViewModelProvider(this)[SettingsViewModel::class.java] }
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appWidgetHost: AppWidgetHost

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        )
        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)
        appWidgetHost = AppWidgetHost(applicationContext, APP_WIDGETHOST_ID)
        Log.d("MainActivity", "AppWidgetHost created with ID: $APP_WIDGETHOST_ID")

        handleFirstOpen()

        setContent {
            var currentScreen by remember { mutableStateOf(Navigation.HOME) }

            voidLauncherTheme {
                voidlauncherNavigation(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it },
                )
            }
        }

        initObservers()
        viewModel.loadApps()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    lifecycleScope.launch { viewModel.emitEvent(UiEvent.NavigateBack) }
                }
            },
        )
    }

    private fun handleFirstOpen() {
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.firstOpen) {
                viewModel.firstOpen(false)
                settingsRepository.setFirstOpen(false)
                settingsRepository.updateSetting {
                    it.copy(firstOpenTime = System.currentTimeMillis())
                }
            }
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                viewModel.launcherResetFailed.collect { resetFailed ->
                    if (resetFailed) {
                        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            AppCompatDelegate.setDefaultNightMode(settings.appTheme)

            if (settings.plainWallpaper &&
                AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ) {
                setPlainWallpaper(this@MainActivity, android.R.color.black)
                recreate()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch { viewModel.emitEvent(UiEvent.NavigateBack) }
        }
    }
}
