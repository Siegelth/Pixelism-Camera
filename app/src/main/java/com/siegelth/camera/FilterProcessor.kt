package com.siegelth.camera

import android.graphics.*

class FilterProcessor {

    companion object {

        // 创建带滤镜效果的Bitmap
        fun applyFilterToBitmap(bitmap: Bitmap, filterType: FilterType): Bitmap {
            val paint = Paint().apply {
                colorFilter = getColorFilter(filterType)
            }

            val filteredBitmap = Bitmap.createBitmap(
                bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(filteredBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            return filteredBitmap
        }

        // 获取对应滤镜的ColorFilter
        fun getColorFilter(filterType: FilterType): ColorFilter? {
            return when (filterType) {
                FilterType.NONE -> null
                FilterType.VINTAGE -> createVintageFilter()
                FilterType.SEPIA -> createSepiaFilter()
                FilterType.FILM -> createFilmFilter()
                FilterType.BLACK_WHITE -> createBlackWhiteFilter()
                FilterType.WARM -> createWarmFilter()
            }
        }

        private fun createVintageFilter(): ColorFilter {
            val colorMatrix = ColorMatrix().apply {
                setSaturation(0.8f)
            }
            val vintageMatrix = ColorMatrix(floatArrayOf(
                0.7f, 0.2f, 0.1f, 0f, 30f,
                0.1f, 0.8f, 0.1f, 0f, 15f,
                0.2f, 0.1f, 0.6f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(vintageMatrix)
            return ColorMatrixColorFilter(colorMatrix)
        }

        private fun createSepiaFilter(): ColorFilter {
            val sepiaMatrix = ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            return ColorMatrixColorFilter(sepiaMatrix)
        }

        private fun createFilmFilter(): ColorFilter {
            val filmMatrix = ColorMatrix(floatArrayOf(
                1.0f, 0.1f, 0.1f, 0f, -5f,
                0.1f, 0.9f, 0.1f, 0f, -3f,
                0.1f, 0.1f, 0.8f, 0f, 3f,
                0f, 0f, 0f, 1f, 0f
            ))
            return ColorMatrixColorFilter(filmMatrix)
        }

        private fun createBlackWhiteFilter(): ColorFilter {
            val bwMatrix = ColorMatrix().apply {
                setSaturation(0f)
            }
            val contrastMatrix = ColorMatrix(floatArrayOf(
                1.3f, 0f, 0f, 0f, -25f,
                0f, 1.3f, 0f, 0f, -25f,
                0f, 0f, 1.3f, 0f, -25f,
                0f, 0f, 0f, 1f, 0f
            ))
            bwMatrix.postConcat(contrastMatrix)
            return ColorMatrixColorFilter(bwMatrix)
        }

        private fun createWarmFilter(): ColorFilter {
            val warmMatrix = ColorMatrix(floatArrayOf(
                1.3f, 0.1f, 0f, 0f, 25f,
                0.1f, 1.2f, 0f, 0f, 15f,
                0f, 0.1f, 0.7f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            ))
            return ColorMatrixColorFilter(warmMatrix)
        }
    }
}
