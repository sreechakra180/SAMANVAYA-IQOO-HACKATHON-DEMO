package com.sukshma.samanvaya.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

private const val TAG = "SensorContextProvider"

// ─── Domain models ────────────────────────────────────────────────────────────

enum class MotionIntensity { STILL, WALKING, FAST, SUDDEN_STOP }

data class MotionContext(
    val intensity: MotionIntensity,
    val accelerationMagnitude: Float,    // m/s² — raw
    val isMovingFast: Boolean,           // above FAST_THRESHOLD
    val isSuddenMotion: Boolean,         // spike detected
    val orientationDeg: Float,           // rough device tilt (gyroscope integral)
    val sensorAvailable: Boolean         // false if hardware missing
)

// ─── Provider ─────────────────────────────────────────────────────────────────

/**
 * SensorContextProvider — collects accelerometer + gyroscope data
 * and produces a MotionContext for use by the vision pipeline.
 *
 * IMPORTANT: We do NOT claim sensors can detect obstacle distance.
 * Motion signals augment vision — they do not replace it.
 *
 * Thresholds are tunable via [config].
 */
class SensorContextProvider(private val context: Context) : SensorEventListener {

    data class Config(
        val fastThreshold: Float = 12f,        // m/s² magnitude for FAST
        val suddenSpikeThreshold: Float = 20f, // m/s² for sudden motion
        val windowSizeMs: Long = 500           // rolling average window
    )

    var config = Config()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var lastMagnitude = 0f
    private var tiltDeg = 0f

    private val _motionState = MutableStateFlow(
        MotionContext(
            intensity = MotionIntensity.STILL,
            accelerationMagnitude = 0f,
            isMovingFast = false,
            isSuddenMotion = false,
            orientationDeg = 0f,
            sensorAvailable = accelSensor != null
        )
    )
    val motionState: StateFlow<MotionContext> = _motionState

    fun start() {
        if (accelSensor == null) {
            Log.w(TAG, "Accelerometer not available on this device")
            return
        }
        sensorManager.registerListener(
            this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL
        )
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        Log.d(TAG, "Sensor monitoring started")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Sensor monitoring stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event.values)
            Sensor.TYPE_GYROSCOPE     -> processGyroscope(event.values)
        }
    }

    private fun processAccelerometer(values: FloatArray) {
        val x = values[0]; val y = values[1]; val z = values[2]
        // Remove gravity component approximately (simple high-pass not applied here for hackathon)
        val magnitude = sqrt(x * x + y * y + z * z)

        val intensity = when {
            magnitude > config.suddenSpikeThreshold -> MotionIntensity.SUDDEN_STOP
            magnitude > config.fastThreshold        -> MotionIntensity.FAST
            magnitude > 4f                          -> MotionIntensity.WALKING
            else                                    -> MotionIntensity.STILL
        }

        _motionState.value = _motionState.value.copy(
            intensity = intensity,
            accelerationMagnitude = magnitude,
            isMovingFast = magnitude > config.fastThreshold,
            isSuddenMotion = magnitude > config.suddenSpikeThreshold,
            sensorAvailable = true
        )

        lastMagnitude = magnitude
    }

    private fun processGyroscope(values: FloatArray) {
        // Simple tilt angle estimate from gyro Z (yaw)
        tiltDeg = (tiltDeg + Math.toDegrees(values[2].toDouble()).toFloat()).coerceIn(-180f, 180f)
        _motionState.value = _motionState.value.copy(orientationDeg = tiltDeg)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} -> $accuracy")
    }

    fun currentMotionContext(): MotionContext = _motionState.value
}
