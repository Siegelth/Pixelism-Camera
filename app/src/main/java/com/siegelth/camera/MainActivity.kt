package com.siegelth.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.siegelth.camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var orientationSensorManager: OrientationSensorManager
    private var deviceOrientation = 0

    // Binary filter parameters
    private var currentThreshold = 128
    private var currentBrightColor = Color.WHITE
    private var currentDarkColor = Color.BLACK
    private lateinit var currentImageAnalyzer: ImageAnalyzer

    // RGB values for colors
    private var brightRed = 255
    private var brightGreen = 255
    private var brightBlue = 255
    private var darkRed = 0
    private var darkGreen = 0
    private var darkBlue = 0

    private lateinit var vibrator: Vibrator

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "需要相机权限才能使用应用", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 隐藏ActionBar和状态栏
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        setupParameterControls()

        orientationSensorManager = OrientationSensorManager(this) { orientation ->
            deviceOrientation = orientation
        }
    }

    private fun setupParameterControls() {
        // 阈值滑块
        binding.seekBarThreshold.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                currentThreshold = progress
                binding.textThresholdValue.text = progress.toString()
                updateFilterParameters()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // 亮色折叠/展开功能
        binding.brightColorHeader.setOnClickListener {
            toggleBrightColorControls()
        }

        // 暗色折叠/展开功能
        binding.darkColorHeader.setOnClickListener {
            toggleDarkColorControls()
        }

        // 亮色RGB滑条
        binding.seekBarBrightRed.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                brightRed = progress
                binding.textBrightRedValue.text = progress.toString()
                updateBrightColor()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.seekBarBrightGreen.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                brightGreen = progress
                binding.textBrightGreenValue.text = progress.toString()
                updateBrightColor()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.seekBarBrightBlue.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                brightBlue = progress
                binding.textBrightBlueValue.text = progress.toString()
                updateBrightColor()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // 暗色RGB滑条
        binding.seekBarDarkRed.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                darkRed = progress
                binding.textDarkRedValue.text = progress.toString()
                updateDarkColor()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.seekBarDarkGreen.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                darkGreen = progress
                binding.textDarkGreenValue.text = progress.toString()
                updateDarkColor()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.seekBarDarkBlue.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                darkBlue = progress
                binding.textDarkBlueValue.text = progress.toString()
                updateDarkColor()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // 初始化颜色
        updateBrightColor()
        updateDarkColor()

        // 设置默认折叠状态
        binding.brightColorControls.visibility = View.GONE
        binding.brightColorArrow.text = "▶"
        binding.darkColorControls.visibility = View.GONE
        binding.darkColorArrow.text = "▶"
    }

    private fun toggleBrightColorControls() {
        val controls = binding.brightColorControls
        val arrow = binding.brightColorArrow

        if (controls.visibility == View.VISIBLE) {
            // 收起
            controls.visibility = View.GONE
            arrow.text = "▶"
        } else {
            // 展开
            controls.visibility = View.VISIBLE
            arrow.text = "▼"
        }
    }

    private fun toggleDarkColorControls() {
        val controls = binding.darkColorControls
        val arrow = binding.darkColorArrow

        if (controls.visibility == View.VISIBLE) {
            // 收起
            controls.visibility = View.GONE
            arrow.text = "▶"
        } else {
            // 展开
            controls.visibility = View.VISIBLE
            arrow.text = "▼"
        }
    }

    private fun updateBrightColor() {
        currentBrightColor = Color.rgb(brightRed, brightGreen, brightBlue)
        updateFilterParameters()
    }

    private fun updateDarkColor() {
        currentDarkColor = Color.rgb(darkRed, darkGreen, darkBlue)
        updateFilterParameters()
    }

    private fun updateFilterParameters() {
        if (::currentImageAnalyzer.isInitialized) {
            currentImageAnalyzer.updateParameters(currentThreshold, currentBrightColor, currentDarkColor)
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (exc: Exception) {
                Log.e(TAG, "相机初始化失败", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        // 创建带参数的ImageAnalyzer
        currentImageAnalyzer = ImageAnalyzer(
            onAnalyzed = { bitmap ->
                bitmap?.let { bmp ->
                    runOnUiThread {
                        binding.filterOverlay.updatePreview(bmp)
                    }
                }
            },
            threshold = currentThreshold,
            brightColor = currentBrightColor,
            darkColor = currentDarkColor
        )

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, currentImageAnalyzer)
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
        } catch (exc: Exception) {
            Log.e(TAG, "相机绑定失败", exc)
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // 快门效果：震动和短黑屏
        performShutterEffect()

        // 创建临时文件来保存原始照片
        val tempFile = File.createTempFile("temp_photo", ".jpg", cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        val exif = ExifInterface(tempFile.absolutePath)

                        // 获取EXIF中的方向信息
                        val exifOrientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )

                        // 根据EXIF和设备方向计算最终旋转角度
                        val rotationDegrees = getRotationDegrees(exifOrientation, deviceOrientation)

                        // 应用旋转修正
                        if (rotationDegrees != 0) {
                            val matrix = Matrix().apply {
                                postRotate(rotationDegrees.toFloat())
                            }
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        }

                        // 缩放到1080P而不裁切
                        val scaledBitmap = scaleToFit1080P(bitmap)

                        // 应用二分法滤镜效果到保存的照片
                        val analyzer = ImageAnalyzer({ }, currentThreshold, currentBrightColor, currentDarkColor)
                        val filteredBitmap = analyzer.applyBinaryFilter(scaledBitmap, currentThreshold, currentBrightColor, currentDarkColor)
                        saveToGallery(filteredBitmap)
                        tempFile.delete()

                    } catch (e: Exception) {
                        Log.e(TAG, "Image processing failed", e)
                        tempFile.delete()
                    }
                }
            }
        )
    }

    private fun performShutterEffect() {
        // 震动反馈
        vibrateDevice()

        // 短黑屏效果
        val blackOverlay = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 1.0f
        }

        // 添加黑屏覆盖层到根布局
        val rootLayout = binding.root
        rootLayout.addView(blackOverlay,
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // 150毫秒后移除黑屏
        blackOverlay.postDelayed({
            rootLayout.removeView(blackOverlay)
        }, 150)
    }

    private fun vibrateDevice() {
        try {
            // 震动反馈
            if (::vibrator.isInitialized && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration function error: ${e.message}")
        }
    }

    private fun getRotationDegrees(exifOrientation: Int, deviceOrientation: Int): Int {
        // 首先根据EXIF信息获取基础旋转角度
        val exifRotation = when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        // 结合设备方向进行最终调整
        return when (deviceOrientation) {
            0 -> exifRotation // Portrait
            90 -> (exifRotation + 270) % 360 // Landscape right
            180 -> (exifRotation + 180) % 360 // Portrait upside down
            270 -> (exifRotation + 90) % 360 // Landscape left
            else -> exifRotation
        }
    }

    private fun scaleToFit1080P(originalBitmap: Bitmap): Bitmap {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // 计算短边，确保短边缩放到1080
        val minDimension = kotlin.math.min(originalWidth, originalHeight)
        val scale = 1080f / minDimension

        // 计算缩放后的尺寸
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()

        // 直接缩放，不添加黑边
        return Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
    }

    private fun saveToGallery(bitmap: Bitmap) {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            takePhoto()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        orientationSensorManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        orientationSensorManager.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationSensorManager.stopListening()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "VintageCamera"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }
}
