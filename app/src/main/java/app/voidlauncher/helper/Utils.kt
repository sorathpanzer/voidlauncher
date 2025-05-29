@file:Suppress("unused")

package app.voidlauncher.helper

import android.annotation.SuppressLint
import android.app.SearchManager
import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Point
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import app.voidlauncher.R
import app.voidlauncher.data.AppModel
import app.voidlauncher.data.Constants
import app.voidlauncher.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.Collator
import kotlin.math.pow
import kotlin.math.sqrt

fun Context.showToast(message: String?, duration: Int = Toast.LENGTH_SHORT) {
    if (message.isNullOrBlank()) return
    Toast.makeText(this, message, duration).show()
}

fun Context.showToast(stringResource: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(stringResource), duration).show()
}

suspend fun getAppsList(
    context: Context,
    settingsRepository: SettingsRepository,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
): MutableList<AppModel> {
    return withContext(Dispatchers.IO) {
        val appList: MutableList<AppModel> = mutableListOf()

        try {
            val settings = settingsRepository.settings.first()
            val hiddenApps = settings.hiddenApps
            val includeIcons = settings.showAppIcons
            val renamedApps = settings.renamedApps
            val selectedIconPack = settings.selectedIconPack

            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val collator = Collator.getInstance()

            val iconCache = IconCache(context)

            for (profile in userManager.userProfiles) {
                for (app in launcherApps.getActivityList(null, profile)) {
                    // Skip VoidLauncher itself
                    if (app.applicationInfo.packageName == context.packageName) continue

                    val tempAppModel = AppModel(
                        "temp",
                        null,
                        app.applicationInfo.packageName,
                        app.componentName.className,
                        false,
                        profile,
                        null,
                        false,
                        profile.toString()
                    )
                    val appKey = tempAppModel.getKey()

                    val defaultLabel = app.label.toString() +
                            if (profile != android.os.Process.myUserHandle()) " (Clone)" else ""

                    val appLabelShown = renamedApps[appKey] ?: defaultLabel

                    // Pass the selected icon pack to getIcon
                    val appIcon = if (includeIcons) {
                        iconCache.getIcon(
                            app.applicationInfo.packageName,
                            app.componentName.className,
                            app.user,
                            selectedIconPack
                        )
                    } else {
                        null
                    }

                    val appModel = AppModel(
                        appLabelShown,
                        collator.getCollationKey(app.label.toString()),
                        app.applicationInfo.packageName,
                        app.componentName.className,
                        (System.currentTimeMillis() - app.firstInstallTime) < Constants.ONE_HOUR_IN_MILLIS,
                        profile,
                        appIcon = appIcon
                    )

                    val dupAppKey = "${app.applicationInfo.packageName}/${profile.hashCode()}"

                    val isHidden = hiddenApps.contains(dupAppKey)

                    if (isHidden) {
                        if (includeHiddenApps) {
                            appList.add(appModel.copy(isHidden = true))
                        }
                    } else {
                        if (includeRegularApps) {
                            appList.add(appModel)
                        }
                    }
                }
            }
            appList.sortBy { it.appLabel.lowercase() }

        } catch (e: Exception) {
            println("Error loading apps: ${e.message}")
            e.printStackTrace()
        }
        appList
    }
}



// This is to ensure backward compatibility with older app versions
// which did not support multiple user profiles
//private fun upgradeHiddenApps(prefs: Prefs) {
//    val hiddenAppsSet = prefs.hiddenApps
//    val newHiddenAppsSet = mutableSetOf<String>()
//    for (hiddenPackage in hiddenAppsSet) {
//        if (hiddenPackage.contains("|")) newHiddenAppsSet.add(hiddenPackage)
//        else newHiddenAppsSet.add(hiddenPackage + android.os.Process.myUserHandle().toString())
//    }
//    prefs.hiddenApps = newHiddenAppsSet
//    prefs.hiddenAppsUpdated = true
//}

fun isPackageInstalled(context: Context, packageName: String, userString: String): Boolean {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(packageName, getUserHandleFromString(context, userString))
    return activityInfo.isNotEmpty()
}

fun getUserHandleFromString(context: Context, userHandleString: String): UserHandle {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    for (userHandle in userManager.userProfiles) {
        if (userHandle.toString() == userHandleString) {
            return userHandle
        }
    }
    return android.os.Process.myUserHandle()
}

fun isClauncherDefault(context: Context): Boolean {
    val launcherPackageName = getDefaultLauncherPackage(context)
    return context.packageName == launcherPackageName
}

fun getDefaultLauncherPackage(context: Context): String {
    val intent = Intent()
    intent.action = Intent.ACTION_MAIN
    intent.addCategory(Intent.CATEGORY_HOME)
    val packageManager = context.packageManager
    val result = packageManager.resolveActivity(intent, 0)
    return if (result?.activityInfo != null) {
        result.activityInfo.packageName
    } else "android"
}

fun setPlainWallpaperByTheme(context: Context, appTheme: Int) {
    when (appTheme) {
        AppCompatDelegate.MODE_NIGHT_YES -> setPlainWallpaper(context, android.R.color.black)
        AppCompatDelegate.MODE_NIGHT_NO -> setPlainWallpaper(context, android.R.color.white)
        else -> {
            if (context.isDarkThemeOn())
                setPlainWallpaper(context, android.R.color.black)
            else setPlainWallpaper(context, android.R.color.white)
        }
    }
}

fun setPlainWallpaper(context: Context, color: Int) {
    try {
        val bitmap = createBitmap(1000, 2000)
        bitmap.eraseColor(context.getColor(color))
        val manager = WallpaperManager.getInstance(context)
        manager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
        manager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getChangedAppTheme(context: Context, currentAppTheme: Int): Int {
    return when (currentAppTheme) {
        AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
        else -> {
            if (context.isDarkThemeOn())
                AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        }
    }
}

fun openAppInfo(context: Context, userHandle: UserHandle, packageName: String) {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val intent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)

    intent?.let {
        launcher.startAppDetailsActivity(intent.component, userHandle, null, null)
    } ?: context.showToast(context.getString(R.string.unable_to_open_app))
}

fun getScreenDimensions(context: Context): Pair<Int, Int> {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics = windowManager.currentWindowMetrics
        val bounds = metrics.bounds
        Pair(bounds.width(), bounds.height())
    } else {
        // Fallback for older versions
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        val point = Point()
        @Suppress("DEPRECATION")
        display.getRealSize(point)
        Pair(point.x, point.y)
    }
}


fun openSearch(context: Context) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, "")
    context.startActivity(intent)
}

@SuppressLint("WrongConstant")
fun expandNotificationDrawer(context: Context) {
    try {
        //  (Android 12+)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            val statusBarManager = context.getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
//            statusBarManager.expandNotificationsPanel()
//            return
//        }

        // Fall back -> reflection for older versions
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (_: Exception) {
        // If all else fails, try to use the notification intent
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}

fun openAlarmApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.d("TAG", e.toString())
    }
}

fun openCalendar(context: Context) {
    try {
        val calendarUri = CalendarContract.CONTENT_URI
            .buildUpon()
            .appendPath("time")
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, calendarUri))
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            val intent = Intent(Intent.ACTION_MAIN).setClassName(
                context,
                "app.voidlauncher.helper.FakeHomeActivity"
            )
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun isAccessServiceEnabled(context: Context): Boolean {
    val enabled = try {
        Settings.Secure.getInt(context.applicationContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
    if (enabled == 1) {
        val enabledServicesString: String? = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServicesString?.contains(context.packageName + "/" + MyAccessibilityService::class.java.name) ?: false
    }
    return false
}

fun isTablet(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = context.resources.displayMetrics

        val bounds = windowManager.currentWindowMetrics.bounds
        val widthPixels = bounds.width()
        val heightPixels = bounds.height()

        val widthInches = widthPixels / metrics.xdpi
        val heightInches = heightPixels / metrics.ydpi
        val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))

        return diagonalInches >= 7.0
    } else {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)

        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))

        return diagonalInches >= 7.0
    }
}


fun Context.isDarkThemeOn(): Boolean {
    return resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}

fun Context.copyToClipboard(text: String) {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(getString(R.string.app_name), text)
    clipboardManager.setPrimaryClip(clipData)
    showToast("")
}

fun Context.openUrl(url: String) {
    if (url.isEmpty()) return
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = url.toUri()
    startActivity(intent)
}

fun Context.isSystemApp(packageName: String): Boolean {
    if (packageName.isBlank()) return true
    return try {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                || (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0))
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.uninstall(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE)
    intent.data = "package:$packageName".toUri()
    startActivity(intent)
}

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true,
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun View.animateAlpha(alpha: Float = 1.0f) {
    this.animate().apply {
        interpolator = LinearInterpolator()
        duration = 200
        alpha(alpha)
        start()
    }
}

fun Context.shareApp() {
    val message = getString(R.string.are_you_using_your_phone_or_is_your_phone_using_you) +
            "\n" + Constants.URL_CCLAUNCHER_GITHUB
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

fun Context.starApp() {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Constants.URL_CCLAUNCHER_GITHUB.toUri()
    )
    var flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
    flags = flags or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
    intent.addFlags(flags)
    startActivity(intent)
}