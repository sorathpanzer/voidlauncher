package app.voidlauncher.helper

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Manager for handling permission-related operations
 */
internal class PermissionManager(
    private val context: Context,
) {
    /**
     * Check if the app has usage stats permission
     */
    private fun hasUsageStatsPermission(): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats =
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 1000 * 60,
                now,
            )
        return stats != null && stats.isNotEmpty()
    }

    /**
     * Check if the app has accessibility service permission
     */
    private fun hasAccessibilityPermission(): Boolean = isAccessServiceEnabled(context)

    /**
     * Open the usage access settings screen
     */
    private fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback if specific settings page not available
            openAppSettings()
        }
    }

    /**
     * Open the accessibility settings screen
     */
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    /**
     * Open the app info settings screen
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
