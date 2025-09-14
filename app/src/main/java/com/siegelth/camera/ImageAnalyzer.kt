package com.siegelth.camera

import android.graphics.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ImageAnalyzer(
    private val onAnalyzed: (Bitmap?) -> Unit,
    private var threshold: Int = 128,
    private var brightColor: Int = Color.WHITE,
    private var darkColor: Int = Color.BLACK
) : ImageAnalysis.Analyzer {

    fun updateParameters(newThreshold: Int, newBrightColor: Int, newDarkColor: Int) {
        threshold = newThreshold
        brightColor = newBrightColor
        darkColor = newDarkColor
    }

    override fun analyze(image: ImageProxy) {
        try {
            val bitmap = imageProxyToBitmap(image)
            val filteredBitmap = bitmap?.let { applyBinaryFilter(it, threshold, brightColor, darkColor) }
            onAnalyzed(filteredBitmap)
        } catch (e: Exception) {
            onAnalyzed(null)
        } finally {
            image.close()
        }
    }

    fun applyBinaryFilter(bitmap: Bitmap, threshold: Int = 128, brightColor: Int = Color.WHITE, darkColor: Int = Color.BLACK): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // 使用像素数组批量处理，比逐个像素处理快很多
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]

            // 计算灰度值
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val gray = (red * 0.299 + green * 0.587 + blue * 0.114).toInt()

            // 二分法：大于阈值显示明亮颜色，小于显示暗色
            pixels[i] = if (gray > threshold) brightColor else darkColor
        }

        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    private fun pixelateBitmap(bitmap: Bitmap, pixelWidth: Int = 108, pixelHeight: Int = 144): Bitmap {
        // Scale down to create pixelation effect
        val smallBitmap = Bitmap.createScaledBitmap(bitmap, pixelWidth, pixelHeight, false)
        // Scale back up to original size
        return Bitmap.createScaledBitmap(smallBitmap, bitmap.width, bitmap.height, false)
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            // 处理YUV_420_888格式
            if (image.format == ImageFormat.YUV_420_888) {
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)

                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
                val imageBytes = out.toByteArray()

                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                // 根据图像旋转角度调整bitmap
                val rotationDegrees = image.imageInfo.rotationDegrees
                if (rotationDegrees != 0) {
                    val matrix = Matrix().apply {
                        postRotate(rotationDegrees.toFloat())
                    }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }
            } else {
                // 处理其他格式
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }
}
