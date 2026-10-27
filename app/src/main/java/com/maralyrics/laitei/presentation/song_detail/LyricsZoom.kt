package com.maralyrics.laitei.presentation.song_detail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * How much the lyrics are scaled relative to the font size chosen in Settings: 1.0 is that size
 * exactly ("default"). Held for the whole app session rather than per song, so Previous/Next and
 * opening the next song from the list keep the size the reader settled on; a restart goes back to
 * the Settings size. It is a multiplier, not a new setting, so Settings stays the one place the
 * default size is chosen.
 */
@Singleton
class LyricsZoom @Inject constructor() {

    private val _factor = MutableStateFlow(DEFAULT)
    val factor: StateFlow<Float> = _factor.asStateFlow()

    fun zoomIn() = zoomBy(STEP)

    fun zoomOut() = zoomBy(1f / STEP)

    /** Scales by [ratio] (a pinch reports a small ratio per frame); stays within [MIN]..[MAX]. */
    fun zoomBy(ratio: Float) {
        if (ratio <= 0f || ratio.isNaN()) return
        _factor.value = (_factor.value * ratio).coerceIn(MIN, MAX)
    }

    fun reset() {
        _factor.value = DEFAULT
    }

    companion object {
        const val DEFAULT = 1f
        const val MIN = 0.6f
        const val MAX = 3f

        /** Each press of the zoom buttons: a fifth bigger or smaller. */
        const val STEP = 1.2f

        fun canZoomIn(factor: Float) = factor < MAX - EPSILON
        fun canZoomOut(factor: Float) = factor > MIN + EPSILON
        fun isDefault(factor: Float) = kotlin.math.abs(factor - DEFAULT) < EPSILON

        /** For display: 1.2 -> 120. */
        fun percent(factor: Float): Int = (factor * 100f).roundToInt()

        // Pinching and repeated stepping accumulate float error; "at the limit" and "at default"
        // must not depend on the last bit.
        private const val EPSILON = 0.005f
    }
}
