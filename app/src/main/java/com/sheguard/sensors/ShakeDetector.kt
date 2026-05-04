package com.sheguard.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.sqrt

class ShakeDetector(
    private val onShakeThresholdReached: () -> Unit
) : SensorEventListener {

    private var lastUpdate = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var shakeCount = 0
    private var firstShakeTime = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < 100) {
            return
        }

        val diffTime = currentTime - lastUpdate
        lastUpdate = currentTime

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val speed = sqrt(
            ((x - lastX) * (x - lastX) +
                (y - lastY) * (y - lastY) +
                (z - lastZ) * (z - lastZ)).toDouble()
        ) / diffTime * 10000

        if (speed > 900) {
            if (firstShakeTime == 0L || currentTime - firstShakeTime > 2000) {
                firstShakeTime = currentTime
                shakeCount = 1
            } else {
                shakeCount += 1
            }

            if (shakeCount >= 3) {
                shakeCount = 0
                firstShakeTime = 0L
                onShakeThresholdReached.invoke()
            }
        }

        lastX = x
        lastY = y
        lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
