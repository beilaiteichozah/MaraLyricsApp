package com.maralyrics.laitei.presentation.song_detail

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

/**
 * Reports a two-finger pinch as a per-event zoom ratio (>1 spreading, <1 pinching in).
 *
 * Only reacts once a second finger is down, so a single finger keeps scrolling and tapping as
 * usual. While pinching it claims the touch in the Initial pass — before the scrollable below
 * sees it — so the two fingers don't also drag the list, and a pinch never ends as a tap.
 */
internal fun Modifier.pinchToZoom(onZoom: (zoomChange: Float) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) onZoom(zoom)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
