package app.voidlauncher

import android.app.Application
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
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
    private lateinit var viewModel: MainViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appWidgetHost: AppWidgetHost

    protected override fun onCreate(savedInstanceState: Bundle?) {
        // Use hardware acceleration
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        )

        // Initialize settings repository
        settingsRepository = SettingsRepository(applicationContext)

        appWidgetHost = AppWidgetHost(applicationContext, APP_WIDGETHOST_ID)
        Log.d("MainActivity", "AppWidgetHost created with ID: $APP_WIDGETHOST_ID")

        viewModel =
            ViewModelProvider(
                this,
                MainViewModelFactory(application, appWidgetHost),
            )[MainViewModel::class.java]

        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        super.onCreate(savedInstanceState)

        // Handle first open
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.firstOpen) {
                viewModel.firstOpen(false)
                settingsRepository.setFirstOpen(false)
                settingsRepository.updateSetting { it.copy(firstOpenTime = System.currentTimeMillis()) }
            }
        }

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        setContent {
            voidLauncherTheme {
                var currentScreen by remember { mutableStateOf(Navigation.HOME) }

                voidlauncherNavigation(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    currentScreen = currentScreen,
                    onScreenChange = { screen ->
                        currentScreen = screen
                    },
                )
            }
        }

        initObservers()
        viewModel.loadApps()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                public override fun handleOnBackPressed() {
                    lifecycleScope.launch {
                        viewModel.emitEvent(UiEvent.NavigateBack)
                    }
                }
            },
        )
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.launcherResetFailed.collect { resetFailed ->
                    openLauncherChooser(resetFailed)
                }
            }
        }
    }

    private fun openLauncherChooser(resetFailed: Boolean) {
        if (resetFailed) {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
        }
    }

    public override fun onConfigurationChanged(newConfig: Configuration) {
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

    protected override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            lifecycleScope.launch {
                viewModel.emitEvent(UiEvent.NavigateBack)
            }
        }
    }
}

private class MainViewModelFactory(
    private val application: Application,
    private val appWidgetHost: AppWidgetHost,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
