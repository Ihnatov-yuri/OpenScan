package com.openscan.scanner.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

/**
 * Evens out the exposure across a multi-page document so all pages share a
 * consistent brightness — useful when scans of the same document were captured
 * under different lighting. Each page is scaled toward a common target mean
 * luminance and lightly contrast-stretched, then written back as JPEG.
 */
object LightingHarmonizer {

    private const val MAX_EDGE_PX = 3000
    private const val JPEG_QUALITY = 92

    fun harmonize(pageFiles: List<File>) {
        if (pageFiles.size < 1) return

        // 1) Measure each page's mean luminance from a small sample.
        val means = pageFiles.map { meanLuminance(it) }
        val valid = means.filter { it > 0 }
        if (valid.isEmpty()) return
        // Target: the average page brightness, nudged toward a clean document look.
        val target = (valid.average() * 0.5 + 200.0 * 0.5).coerceIn(120.0, 235.0)

        // 2) Re-tone each page toward the target.
        pageFiles.forEachIndexed { index, file ->
            val mean = means[index]
            if (mean <= 0) return@forEachIndexed
            val scale = (target / mean).toFloat().coerceIn(0.6f, 1.8f)
            retone(file, scale)
        }
    }

    /** Average luminance (0..255) of a downsampled copy of the image. */
    private fun meanLuminance(file: File): Double {
        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bmp = BitmapFactory.decodeFile(file.path, opts) ?: return 0.0
        try {
            val w = bmp.width
            val h = bmp.height
            if (w == 0 || h == 0) return 0.0
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            var sum = 0.0
            for (p in pixels) {
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                sum += 0.299 * r + 0.587 * g + 0.114 * b
            }
            return sum / pixels.size
        } finally {
            bmp.recycle()
        }
    }

    /** Apply a brightness [scale] (with a mild contrast lift) and overwrite the file. */
    private fun retone(file: File, scale: Float) {
        val src = decodeBounded(file) ?: return
        try {
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            // Brightness scale around 0, then a gentle contrast stretch (1.1x around mid-grey).
            val brightness = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, 0f,
                    0f, scale, 0f, 0f, 0f,
                    0f, 0f, scale, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val contrast = 1.1f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            brightness.postConcat(contrastMatrix)
            val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(brightness) }
            canvas.drawBitmap(src, 0f, 0f, paint)
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            out.recycle()
        } finally {
            src.recycle()
        }
    }

    private fun decodeBounded(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > MAX_EDGE_PX) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inMutable = false
        }
        return BitmapFactory.decodeFile(file.path, opts)
    }
}
