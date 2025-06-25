package app.voidlauncher.helper

import android.app.SearchManager
import android.content.Context
import android.content.Intent

internal fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query ?: "")
    startActivity(intent)
}
