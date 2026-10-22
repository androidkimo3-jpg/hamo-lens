package com.example.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

enum class ImageFilter(val id: String, val stringKey: String) {
    ORIGINAL("none", "f_none"),
    MONO("mono", "f_mono"),
    SEPIA("sepia", "f_sepia"),
    VIVID("vivid", "f_vivid"),
    COOL("cool", "f_cool"),
    WARM("warm", "f_warm"),
    FADE("fade", "f_fade");

    val androidMatrix: ColorMatrix by lazy {
        when (this) {
            ORIGINAL -> ColorMatrix()
            MONO -> {
                // grayscale(1) contrast(1.1)
                val m = ColorMatrix().apply { setSaturation(0f) }
                m.postConcat(contrastMatrix(1.1f))
                m
            }
            SEPIA -> {
                // sepia(0.8) contrast(1.05)
                val m = sepiaMatrix(0.8f)
                m.postConcat(contrastMatrix(1.05f))
                m
            }
            VIVID -> {
                // saturate(1.7) contrast(1.1)
                val m = ColorMatrix().apply { setSaturation(1.7f) }
                m.postConcat(contrastMatrix(1.1f))
                m
            }
            COOL -> {
                // hue-rotate(-12deg) saturate(1.1) brightness(1.03)
                val m = hueRotateMatrix(-12f)
                m.postConcat(ColorMatrix().apply { setSaturation(1.1f) })
                m.postConcat(brightnessMatrix(1.03f))
                m
            }
            WARM -> {
                // sepia(0.3) saturate(1.3) hue-rotate(-8deg)
                val m = sepiaMatrix(0.3f)
                m.postConcat(ColorMatrix().apply { setSaturation(1.3f) })
                m.postConcat(hueRotateMatrix(-8f))
                m
            }
            FADE -> {
                // contrast(0.85) brightness(1.1) saturate(0.8)
                val m = contrastMatrix(0.85f)
                m.postConcat(brightnessMatrix(1.1f))
                m.postConcat(ColorMatrix().apply { setSaturation(0.8f) })
                m
            }
        }
    }

    val composeMatrix: androidx.compose.ui.graphics.ColorMatrix by lazy {
        androidx.compose.ui.graphics.ColorMatrix(androidMatrix.array)
    }

    companion object {
        fun fromId(id: String): ImageFilter = entries.firstOrNull { it.id == id } ?: ORIGINAL

        private fun contrastMatrix(contrast: Float): ColorMatrix {
            val scale = contrast
            val translate = 128f * (1f - contrast)
            return ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        private fun brightnessMatrix(brightness: Float): ColorMatrix {
            return ColorMatrix(
                floatArrayOf(
                    brightness, 0f, 0f, 0f, 0f,
                    0f, brightness, 0f, 0f, 0f,
                    0f, 0f, brightness, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        private fun sepiaMatrix(amount: Float): ColorMatrix {
            val i = 1f - amount
            return ColorMatrix(
                floatArrayOf(
                    i + 0.393f * amount, 0.769f * amount, 0.189f * amount, 0f, 0f,
                    0.349f * amount, i + 0.686f * amount, 0.168f * amount, 0f, 0f,
                    0.272f * amount, 0.534f * amount, i + 0.131f * amount, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        private fun hueRotateMatrix(degrees: Float): ColorMatrix {
            val rad = Math.toRadians(degrees.toDouble()).toFloat()
            val cosVal = cos(rad.toDouble()).toFloat()
            val sinVal = sin(rad.toDouble()).toFloat()

            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f

            return ColorMatrix(
                floatArrayOf(
                    lumR + cosVal * (1f - lumR) + sinVal * (-lumR),
                    lumG + cosVal * (-lumG) + sinVal * (-lumG),
                    lumB + cosVal * (-lumB) + sinVal * (1f - lumB),
                    0f, 0f,

                    lumR + cosVal * (-lumR) + sinVal * (0.143f),
                    lumG + cosVal * (1f - lumG) + sinVal * (0.140f),
                    lumB + cosVal * (-lumB) + sinVal * (-0.283f),
                    0f, 0f,

                    lumR + cosVal * (-lumR) + sinVal * (-(1f - lumR)),
                    lumG + cosVal * (-lumG) + sinVal * (lumG),
                    lumB + cosVal * (1f - lumB) + sinVal * (lumB),
                    0f, 0f,

                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        fun applyFilter(source: Bitmap, filter: ImageFilter): Bitmap {
            if (filter == ORIGINAL) return source
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(filter.androidMatrix)
            }
            canvas.drawBitmap(source, 0f, 0f, paint)
            return output
        }
    }
}
