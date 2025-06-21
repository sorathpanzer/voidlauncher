package app.voidlauncher.data

import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.text.CollationKey

@Serializable
@Immutable
internal data class AppModel(
    val appLabel: String,
    @Transient
    val key: CollationKey? = null,
    val appPackage: String,
    val activityClassName: String?,
    val New: Boolean = false,
    @Transient
    val user: UserHandle = android.os.Process.myUserHandle(),
    @Transient
    val appIcon: ImageBitmap? = null,
    val Hidden: Boolean = false,
    val userString: String = user.toString(),
) : Comparable<AppModel> {
    override fun compareTo(other: AppModel): Int =
        when {
            key != null && other.key != null -> key.compareTo(other.key)
            else -> appLabel.compareTo(other.appLabel, true)
        }

    internal fun getKey(): String = "$appPackage/$userString"
}
