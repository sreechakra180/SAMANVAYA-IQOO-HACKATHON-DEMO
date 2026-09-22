package com.sukshma.samanvaya.accessibility

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

private const val TAG = "HapticEngine"

enum class HapticLevel { INFO, WARNING, CRITICAL }

/**
 * HapticEngine — accessibility haptic feedback patterns.
 *
 * Patterns:
 *  INFO     → 1 short pulse  (80ms)
 *  WARNING  → 2 short pulses (80ms on, 80ms off, 80ms on)
 *  CRITICAL → 1 long pulse   (300ms) — hazard detected
 */
class HapticEngine(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate(level: HapticLevel) {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            Log.w(TAG, "Vibrator not available")
            return
        }

        val effect = when (level) {
            HapticLevel.INFO -> VibrationEffect.createOneShot(
                80L, VibrationEffect.DEFAULT_AMPLITUDE
            )
            HapticLevel.WARNING -> VibrationEffect.createWaveform(
                longArrayOf(0, 80, 80, 80),
                intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                -1  // no repeat
            )
            HapticLevel.CRITICAL -> VibrationEffect.createOneShot(
                300L, VibrationEffect.DEFAULT_AMPLITUDE
            )
        }

        vib.vibrate(effect)
        Log.d(TAG, "Haptic: $level")
    }
}
