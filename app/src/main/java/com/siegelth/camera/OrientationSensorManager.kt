package com.siegelth.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

class OrientationSensorManager(
    private val context: Context,
    private val onOrientationChanged: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var lastAccelerometerValues = FloatArray(3)
    private var lastMagnetometerValues = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var currentOrientation = 0

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometerValues, 0, event.values.size)
                lastAccelerometerSet = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagnetometerValues, 0, event.values.size)
                lastMagnetometerSet = true
            }
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, null, lastAccelerometerValues, lastMagnetometerValues)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)

                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                val newOrientation = calculateOrientation(pitch, roll)
                if (newOrientation != currentOrientation) {
                    currentOrientation = newOrientation
                    onOrientationChanged(currentOrientation)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不需要处理精度变化
    }

    private fun calculateOrientation(pitch: Float, roll: Float): Int {
        return when {
            abs(roll) < 45 -> 0  // Portrait
            roll > 45 -> 270     // Landscape left
            roll < -45 -> 90     // Landscape right
            else -> 180          // Portrait upside down
        }
    }

    fun getCurrentOrientation(): Int = currentOrientation
}
