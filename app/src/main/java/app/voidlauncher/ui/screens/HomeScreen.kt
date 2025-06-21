package app.voidlauncher.ui.screens

import android.appwidget.AppWidgetHost
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import app.voidlauncher.MainViewModel
import app.voidlauncher.data.Constants
import app.voidlauncher.helper.expandNotificationDrawer
import app.voidlauncher.helper.isAccessServiceEnabled
import app.voidlauncher.ui.util.detectPinchGestures
import app.voidlauncher.ui.util.detectSwipeGestures
import app.voidlauncher.ui.util.detectTwoFingerSwipes
import app.voidlauncher.ui.viewmodels.SettingsViewModel

private fun checkAccessibilityAndLock(
    context: Context,
    viewModel: MainViewModel,
) {
    if (!isAccessServiceEnabled(context)) {
        Toast.makeText(context, "Enable accessibility permission to lock screen.", Toast.LENGTH_SHORT).show()
        val intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    } else {
        viewModel.lockScreen()
    }
}

@Composable
internal fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    appWidgetHost: AppWidgetHost,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()

    fun handleAction(
        context: Context,
        viewModel: MainViewModel,
        action: Int,
        launchApp: () -> Unit = {},
    ) {
        when (action) {
            Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
            Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
            Constants.SwipeAction.APP -> launchApp()
            Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
            Constants.SwipeAction.NULL -> Unit
            else -> {}
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .detectSwipeGestures(
                    onSwipeUp = {
                        handleAction(context, viewModel, settings.swipeUpAction) {
                            viewModel.launchSwipeUpApp()
                        }
                    },
                    onSwipeDown = {
                        handleAction(context, viewModel, settings.swipeDownAction) {
                            viewModel.launchSwipeDownApp()
                        }
                    },
                    onSwipeLeft = {
                        handleAction(context, viewModel, settings.swipeLeftAction) {
                            viewModel.launchSwipeLeftApp()
                        }
                    },
                    onSwipeRight = {
                        handleAction(context, viewModel, settings.swipeRightAction) {
                            viewModel.launchSwipeRightApp()
                        }
                    },
                )
                // Multi-finger swipe support
                .detectTwoFingerSwipes(
                    onSwipeUp = {
                        handleAction(context, viewModel, settings.twoFingerSwipeUpAction) {
                            viewModel.launchTwoFingerSwipeUpApp()
                        }
                    },
                    onSwipeDown = {
                        handleAction(context, viewModel, settings.twoFingerSwipeDownAction) {
                            viewModel.launchTwoFingerSwipeDownApp()
                        }
                    },
                    onSwipeRight = {
                        handleAction(context, viewModel, settings.twoFingerSwipeRightAction) {
                            viewModel.launchTwoFingerSwipeRightApp()
                        }
                    },
                    onSwipeLeft = {
                        handleAction(context, viewModel, settings.twoFingerSwipeLeftAction) {
                            viewModel.launchTwoFingerSwipeLeftApp()
                        }
                    },
                ).detectPinchGestures { zoomDelta ->
                    when {
                        zoomDelta > 150f -> {
                            // Pinch out (zoom in)
                            handleAction(context, viewModel, settings.pinchOutAction) {
                                viewModel.launchPinchOutApp()
                            }
                        }
                        zoomDelta < -150f -> {
                            // Pinch in (zoom out)
                            handleAction(context, viewModel, settings.pinchInAction) {
                                viewModel.launchPinchInApp()
                            }
                        }
                    }
                }.pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            handleAction(context, viewModel, settings.doubleTapAction) {
                                viewModel.launchDoubleTapApp()
                            }
                        },
                        onLongPress = {
                            onNavigateToSettings()
                        },
                        onTap = {
                            handleAction(context, viewModel, settings.oneTapAction) {
                                viewModel.launchOneTapApp()
                            }
                        },
                    )
                },
    ) {
        // TODO: Add your HomeScreen UI content here
    }
}
