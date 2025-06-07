package app.voidlauncher

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.voidlauncher.data.Navigation
import app.voidlauncher.data.repository.SettingsRepository
import app.voidlauncher.helper.isDarkThemeOn
import app.voidlauncher.helper.isTablet
import app.voidlauncher.helper.setPlainWallpaper
import app.voidlauncher.ui.CLauncherNavigation
import app.voidlauncher.ui.UiEvent
import app.voidlauncher.ui.util.updateStatusBarVisibility
import app.voidlauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.appwidget.AppWidgetHost
import androidx.lifecycle.ViewModel

internal class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var appWidgetHost: AppWidgetHost
    private val APPWIDGET_HOST_ID = 1024
    private val REQUEST_CONFIGURE_WIDGET = 1001

    private val appWidgetManagerInstance: AppWidgetManager by lazy {
        AppWidgetManager.getInstance(applicationContext)
    }

    val widgetRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "Widget request result: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            Log.d("MainActivity", "Widget ID from result: $appWidgetId")
            if (appWidgetId != -1) {
            }
        }
    }

    private val configureWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val requestCode = REQUEST_CONFIGURE_WIDGET
        val resultCode = result.resultCode
        val data = result.data

        viewModel.handleActivityResult(requestCode, resultCode, data)
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {

        // Use hardware acceleration
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        // Initialize settings repository
        settingsRepository = SettingsRepository(applicationContext)

        appWidgetHost = AppWidgetHost(applicationContext, APPWIDGET_HOST_ID)
        Log.d("MainActivity", "AppWidgetHost created with ID: $APPWIDGET_HOST_ID")

        viewModel = ViewModelProvider(this, MainViewModelFactory(application, appWidgetHost))[MainViewModel::class.java] // Use factory
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]


        // Initialize theme based on settings
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
        }

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

        // Update status bar visibility
        lifecycleScope.launch {
            // Ensure window is ready
            delay(500)
            settingsRepository.settings.first().let { settings ->
                try {
                    updateStatusBarVisibility(this@MainActivity, settings.statusBar)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        setContent {
            CLauncherTheme {
                var currentScreen by remember { mutableStateOf(Navigation.HOME) }

                CLauncherNavigation(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    currentScreen = currentScreen,
                    onScreenChange = { screen ->
                        currentScreen = screen
                    },
                    appWidgetHost = appWidgetHost,
                )
            }
        }

        initObservers()
        viewModel.loadApps()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            public override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    viewModel.emitEvent(UiEvent.NavigateBack)
                }
            }
        })
    }

    protected override fun onStart() {
        super.onStart()
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting widget host listening", e)
        }
    }

    protected override fun onStop() {
        super.onStop()
        try {
            appWidgetHost.stopListening() // Stop listening to save resources
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping widget host listening", e)
        }
    }


    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    private fun setPlainWallpaper() {
        if (this.isDarkThemeOn())
            setPlainWallpaper(this, android.R.color.black)
        else setPlainWallpaper(this, android.R.color.white)
    }

    public override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            AppCompatDelegate.setDefaultNightMode(settings.appTheme)

            if (settings.plainWallpaper && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                setPlainWallpaper()
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

    protected override fun onResume() {
        super.onResume()
        // Force hardware acceleration
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
    }

    protected override fun onDestroy() {
        super.onDestroy()
    }

}

private class MainViewModelFactory(
    private val application: Application,
    private val appWidgetHost: AppWidgetHost
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, appWidgetHost) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
