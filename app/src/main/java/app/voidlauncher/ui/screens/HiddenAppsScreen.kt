package app.voidlauncher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.voidlauncher.MainViewModel
import app.voidlauncher.ui.components.appItem

private const val EFFECT_TWEEN_DELAY = 300

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun hiddenAppsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
) {
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val loading by remember { mutableStateOf(false) }
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Load hidden apps when screen is shown
    LaunchedEffect(Unit) {
        viewModel.getHiddenApps()
    }

    // Handle errors
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Show error toast or message
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .animateContentSize(
                        animationSpec = tween(EFFECT_TWEEN_DELAY),
                    ),
        ) {
            if (loading) {
                // Show loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (hiddenApps.isEmpty()) {
                // Show empty state with animation
                AnimatedVisibility(
                    visible = true,
                    enter =
                        fadeIn(animationSpec = tween(EFFECT_TWEEN_DELAY)) +
                            expandVertically(animationSpec = tween(EFFECT_TWEEN_DELAY)),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No hidden apps",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Long-press on any app in the app drawer to hide it",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = hiddenApps,
                        key = { app -> "${app.appPackage}/${app.activityClassName ?: ""}/${app.user.hashCode()}" },
                    ) { app ->
                        appItem(
                            app = app,
                            onClick = {
                                viewModel.launchApp(app)
                            },
                            onLongClick = {
                                // Unhide app and refresh list
                                viewModel.toggleAppHidden(app)
                            },
                            modifier =
                                Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = tween(durationMillis = 300),
                                ),
                        )
                    }
                }
            }
        }
    }
}
