package com.project.bachao.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs

class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShake: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "BACHAO_SHAKE"

        private const val SHAKE_THRESHOLD = 2.5f
        private const val REQUIRED_MOVEMENTS = 3

        // After detecting a shake, ignore further shakes
        // for this amount of time.
        private const val COOLDOWN_MS = 5000L

        // The 3 movements must occur within this period.
        private const val SHAKE_WINDOW_MS = 1000L
    }

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    private var initialized = false

    private var movementCount = 0
    private var firstMovementTime = 0L
    private var lastShakeTime = 0L

    private var running = false

    fun start() {

        if (running) {
            Log.d(TAG, "Detector already running")
            return
        }

        val accelerometer =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )

        if (accelerometer == null) {
            Log.e(TAG, "Accelerometer not available")
            return
        }

        running = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        initialized = false
        movementCount = 0
        firstMovementTime = 0L

        Log.d(TAG, "================================")
        Log.d(TAG, "SHAKE DETECTOR STARTED")
        Log.d(TAG, "Registered = $running")
        Log.d(TAG, "================================")
    }

    fun stop() {

        if (!running) {
            return
        }

        sensorManager.unregisterListener(this)

        running = false
        initialized = false
        movementCount = 0
        firstMovementTime = 0L

        Log.d(TAG, "SHAKE DETECTOR STOPPED")
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event == null) {
            return
        }

        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!initialized) {

            lastX = x
            lastY = y
            lastZ = z

            initialized = true

            return
        }

        val dx = abs(x - lastX)
        val dy = abs(y - lastY)
        val dz = abs(z - lastZ)

        lastX = x
        lastY = y
        lastZ = z

        val movement = maxOf(dx, dy, dz)

        val now = System.currentTimeMillis()

        /*
         * Cooldown after a successful shake.
         */
        if (now - lastShakeTime < COOLDOWN_MS) {
            return
        }

        /*
         * Only count strong movements.
         */
        if (movement < SHAKE_THRESHOLD) {
            return
        }

        /*
         * Start a new shake window.
         */
        if (movementCount == 0) {
            firstMovementTime = now
        }

        /*
         * Movements must happen close together.
         */
        if (now - firstMovementTime > SHAKE_WINDOW_MS) {

            movementCount = 1
            firstMovementTime = now

        } else {

            movementCount++
        }

        Log.d(
            TAG,
            "Strong movement=$movement " +
                    "count=$movementCount"
        )

        /*
         * Three strong movements = one shake.
         */
        if (movementCount >= REQUIRED_MOVEMENTS) {

            /*
             * IMPORTANT:
             * Set cooldown BEFORE calling callback.
             */
            lastShakeTime = now

            movementCount = 0
            firstMovementTime = 0L

            Log.d(TAG, "================================")
            Log.d(TAG, "SHAKE DETECTED!")
            Log.d(TAG, "Calling emergency callback")
            Log.d(TAG, "================================")

            onShake()
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }
}