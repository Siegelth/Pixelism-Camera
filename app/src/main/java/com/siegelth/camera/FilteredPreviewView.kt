package com.siegelth.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FilteredPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentFilter: FilterType = FilterType.NONE
    private var previewBitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastUpdateTime = 0L
    private val updateInterval = 100L // 限制更新频率

    fun setFilter(filterType: FilterType) {
        if (currentFilter != filterType) {
            currentFilter = filterType
            paint.colorFilter = FilterProcessor.getColorFilter(filterType)
            invalidate()
        }
    }

    fun updatePreview(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime > updateInterval) {
            previewBitmap?.recycle() // 释放之前的bitmap
            previewBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            lastUpdateTime = currentTime
            post { invalidate() } // 在UI线程中刷新
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        previewBitmap?.let { bitmap ->
            // 计算缩放比例以适应视图
            val scaleX = width.toFloat() / bitmap.width
            val scaleY = height.toFloat() / bitmap.height
            val scale = minOf(scaleX, scaleY)

            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale

            val left = (width - scaledWidth) / 2
            val top = (height - scaledHeight) / 2

            val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
            canvas.drawBitmap(bitmap, null, destRect, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        previewBitmap?.recycle()
        previewBitmap = null
    }
}
