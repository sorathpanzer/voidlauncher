package app.voidlauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import app.voidlauncher.data.AppModel
import app.voidlauncher.helper.getAppsList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * Repository for app-related operations
 */
internal class AppRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val _appList = MutableStateFlow<List<AppModel>>(emptyList())
    val appList: StateFlow<List<AppModel>> = _appList.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenApps: StateFlow<List<AppModel>> = _hiddenApps.asStateFlow()

    /**
     * Load all visible apps
     */
    internal suspend fun loadApps() {
        withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.first()
                if (settings.showHiddenAppsOnSearch) {
                    val apps = getAppsList(context, settingsRepository, includeRegularApps = true, includeHiddenApps = true)
                    _appList.value = apps
                } else {
                    val apps = getAppsList(context, settingsRepository, includeRegularApps = true, includeHiddenApps = false)
                    _appList.value = apps
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    internal suspend fun loadHiddenApps() {
        withContext(Dispatchers.IO) {
            try {
                val hiddenApps = getAppsList(context, settingsRepository, includeRegularApps = false, includeHiddenApps = true)
                _hiddenApps.value = hiddenApps
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Toggle app hidden state
     */
    internal suspend fun toggleAppHidden(app: AppModel) {
        withContext(Dispatchers.IO) {
            try {
                val appKey = "${app.appPackage}/${app.user.hashCode()}"

                settingsRepository.toggleAppHidden(appKey)

                loadApps()
                loadHiddenApps()
            } catch (e: Exception) {
                println("Error toggling app hidden state: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    /**
     * Launch an app
     */
    internal suspend fun launchApp(appModel: AppModel) {
        withContext(Dispatchers.Main) {
            try {
                val component =
                    ComponentName(
                        appModel.appPackage,
                        appModel.activityClassName ?: "",
                    )
                launcherApps.startMainActivity(component, appModel.user, null, null)
            } catch (e: SecurityException) {
                throw AppLaunchException("Security error launching ${appModel.appLabel}", e)
            } catch (e: NullPointerException) {
                throw AppLaunchException("App component not found for ${appModel.appLabel}", e)
            } catch (e: Exception) {
                throw AppLaunchException("Failed to launch ${appModel.appLabel}", e)
            }
        }
    }

    /**
     * Exception for app launch failures
     */
    class AppLaunchException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)
}
