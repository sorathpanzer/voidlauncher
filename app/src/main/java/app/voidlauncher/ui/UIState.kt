package app.voidlauncher.ui

import app.voidlauncher.data.AppModel

internal data class AppDrawerUiState(
    val apps: List<AppModel> = emptyList(),
    val filteredApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val Loading: Boolean = false,
    val error: String? = null,
    val calculatorResult: String = "",
    val showCalculatorResult: Boolean = false
)
