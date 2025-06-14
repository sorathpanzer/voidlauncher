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
// import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import app.voidlauncher.MainViewModel
import app.voidlauncher.data.Constants
import app.voidlauncher.helper.expandNotificationDrawer
import app.voidlauncher.helper.isAccessServiceEnabled
import app.voidlauncher.ui.util.detectSwipeGestures
import app.voidlauncher.ui.viewmodels.SettingsViewModel
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import androidx.compose.foundation.gestures.awaitEachGesture

internal fun Modifier.detectTwoFingerVerticalSwipes(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val pointers = mutableMapOf<PointerId, Float>() // store Y positions

            // Wait for two fingers down
            while (pointers.size < 2) {
                val event = awaitPointerEvent()
                event.changes.filter { it.pressed }.forEach {
                    pointers[it.id] = it.position.y
                }
            }

            val startYs = pointers.toMap()

            // Track movement
            var endYs = startYs
            while (true) {
                val event = awaitPointerEvent()
                val current = event.changes.filter { it.pressed }
                if (current.size < 2) break

                endYs = current.associate { it.id to it.position.y }
                current.forEach { it.consume() }
            }

            val dy = endYs.values.zip(startYs.values).map { (end, start) -> end - start }

            if (dy.size == 2) {
                val minSwipe = 50f
                if (dy.all { it > minSwipe }) {
                    onSwipeDown()
                } else if (dy.all { it < -minSwipe }) {
                    onSwipeUp()
                }
            }
        }
    }
)


private fun checkAccessibilityAndLock(context: Context, viewModel: MainViewModel) {
    if (!isAccessServiceEnabled(context)) {
        Toast.makeText(context, "Enable accessibility permission to lock screen.", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
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
        launchApp: () -> Unit = {}
    ) {
        when (action) {
            Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
            Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
            Constants.SwipeAction.APP -> launchApp()
            Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
            Constants.SwipeAction.NULL -> Unit
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
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
                }
            )
             // Multi-finger swipe support
            .detectTwoFingerVerticalSwipes(
                onSwipeUp = {
                    handleAction(context, viewModel, settings.twoFingerSwipeUpAction) {
                        viewModel.launchTwoFingerSwipeUpApp()
                    }
                },
                onSwipeDown = {
                    handleAction(context, viewModel, settings.twoFingerSwipeDownAction) {
                        viewModel.launchTwoFingerSwipeDownApp()
                    }
                }
            )
            .pointerInput(Unit) {
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
                    }
                )
            }
    ) {
        // TODO: Add your HomeScreen UI content here
    }
}
