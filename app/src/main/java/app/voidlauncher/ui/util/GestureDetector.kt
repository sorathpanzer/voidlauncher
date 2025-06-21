package app.voidlauncher.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Adds swipe gesture detection to a composable
 */
internal fun Modifier.detectSwipeGestures(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val (x, y) = dragAmount

                // Use a minimum threshold to avoid accidental swipes
                val minSwipeDistance = 50f

                // Determine which direction has the highest magnitude
                when {
                    abs(x) > abs(y) && abs(x) > minSwipeDistance -> {
                        // Horizontal swipe
                        if (x > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                    abs(y) > abs(x) && abs(y) > minSwipeDistance -> {
                        // Vertical swipe
                        if (y > 0) {
                            onSwipeDown()
                        } else {
                            onSwipeUp()
                        }
                    }
                }
            }
        },
    )

internal fun Modifier.detectTwoFingerSwipes(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val pointersY = mutableMapOf<PointerId, Float>()
                val pointersX = mutableMapOf<PointerId, Float>()

                // Wait for two fingers down
                while (pointersY.size < 2) {
                    val event = awaitPointerEvent()
                    event.changes.filter { it.pressed }.forEach {
                        pointersY[it.id] = it.position.y
                        pointersX[it.id] = it.position.x
                    }
                }

                val startYs = pointersY.toMap()
                val startXs = pointersX.toMap()

                var endYs = startYs
                var endXs = startXs

                while (true) {
                    val event = awaitPointerEvent()
                    val current = event.changes.filter { it.pressed }
                    if (current.size < 2) break

                    endYs = current.associate { it.id to it.position.y }
                    endXs = current.associate { it.id to it.position.x }

                    current.forEach { it.consume() }
                }

                val dy = endYs.values.zip(startYs.values).map { (end, start) -> end - start }
                val dx = endXs.values.zip(startXs.values).map { (end, start) -> end - start }

                val minSwipe = 50f

                if (dy.size == 2 && dx.size == 2) {
                    if (dy.all { abs(it) > abs(dx.first()) } && dy.all { it > minSwipe }) {
                        onSwipeDown()
                    } else if (dy.all { abs(it) > abs(dx.first()) } && dy.all { it < -minSwipe }) {
                        onSwipeUp()
                    } else if (dx.all { abs(it) > abs(dy.first()) } && dx.all { it > minSwipe }) {
                        onSwipeRight()
                    } else if (dx.all { abs(it) > abs(dy.first()) } && dx.all { it < -minSwipe }) {
                        onSwipeLeft()
                    }
                }
            }
        },
    )

internal fun Modifier.detectPinchGestures(onPinch: (zoomDelta: Float) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            var pointer1: PointerInputChange? = null
            var pointer2: PointerInputChange? = null

            while (pointer1 == null || pointer2 == null) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    pointer1 = pressed[0]
                    pointer2 = pressed[1]
                }
            }

            var previousDistance = distanceBetween(pointer1.position, pointer2.position)

            while (true) {
                val event = awaitPointerEvent()
                val changes = event.changes.filter { it.pressed }
                if (changes.size < 2) break

                val pos1 = changes[0].position
                val pos2 = changes[1].position

                val currentDistance = distanceBetween(pos1, pos2)
                val zoomDelta = currentDistance - previousDistance

                if (zoomDelta != 0f) {
                    onPinch(zoomDelta)
                }

                previousDistance = currentDistance
                changes.forEach { it.consume() }
            }
        }
    }

private fun distanceBetween(
    p1: Offset,
    p2: Offset,
): Float = hypot(p2.x - p1.x, p2.y - p1.y)
