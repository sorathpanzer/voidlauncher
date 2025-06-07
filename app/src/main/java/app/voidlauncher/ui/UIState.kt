package app.voidlauncher.ui

import app.voidlauncher.data.AppModel

/**
 * App drawer UI state
 */
internal data class AppDrawerUiState(
    val apps: List<AppModel> = emptyList(),
    val filteredApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
