package app.voidlauncher.helper

import android.app.Activity
import android.app.SearchManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

internal fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query ?: "")
    startActivity(intent)
}
