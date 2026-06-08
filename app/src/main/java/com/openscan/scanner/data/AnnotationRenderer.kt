package com.openscan.scanner.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import java.io.File
import java.io.FileOutputStream

/**
 * Burns freehand annotation strokes (signatures, notes) permanently into a page
 * image. Stroke coordinates are normalized to [0,1] relative to the page, so the
 * drawing scales correctly regardless of the on-screen size it was drawn at.
 */
object AnnotationRenderer {

    private const val MAX_EDGE_PX = 3000
    private const val JPEG_QUALITY = 92

    /** A single freehand stroke in normalized page coordinates. */
    class Stroke(
        val xs: FloatArray,
        val ys: FloatArray,
        val color: Int,
        val widthFrac: Float
    )

    fun burnIn(pageFile: File, strokes: List<Stroke>) {
        if (strokes.isEmpty()) return
        val src = decodeBounded(pageFile) ?: return
        try {
            val w = src.width
            val h = src.height
            val canvas = Canvas(src)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            strokes.forEach { stroke ->
                if (stroke.xs.size < 1) return@forEach
                paint.color = stroke.color
                paint.strokeWidth = (stroke.widthFrac * w).coerceAtLeast(1f)
                val path = Path()
                path.moveTo(stroke.xs[0] * w, stroke.ys[0] * h)
                for (i in 1 until stroke.xs.size) {
                    path.lineTo(stroke.xs[i] * w, stroke.ys[i] * h)
                }
                if (stroke.xs.size == 1) {
                    // a single dot
                    canvas.drawPoint(stroke.xs[0] * w, stroke.ys[0] * h, paint.apply { style = Paint.Style.FILL })
                    paint.style = Paint.Style.STROKE
                } else {
                    canvas.drawPath(path, paint)
                }
            }
            FileOutputStream(pageFile).use { src.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
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
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.path, opts)
    }
}
