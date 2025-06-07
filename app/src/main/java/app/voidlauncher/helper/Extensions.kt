package app.voidlauncher.helper

import android.app.Activity
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
fun Activity.showLauncherSelector(requestCode: Int) {
    val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
    if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        startActivityForResult(intent, requestCode)
    }
}

fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query ?: "")
    startActivity(intent)
}
