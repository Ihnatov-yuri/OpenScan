package com.openscan.scanner.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Builds a multi-page PDF from a list of page images using the platform
 * [PdfDocument] API (no third-party PDF library required). Each image becomes
 * one page sized to the (optionally downscaled) image pixels.
 */
object PdfBuilder {

    /** Default cap for the longest page edge when none is supplied. */
    private const val DEFAULT_MAX_EDGE_PX = 2480

    fun build(imageFiles: List<File>, out: File, maxEdgePx: Int = DEFAULT_MAX_EDGE_PX) {
        val doc = PdfDocument()
        try {
            var pageNumber = 1
            for (file in imageFiles) {
                val bitmap = decodeBounded(file, maxEdgePx) ?: continue
                val info = PdfDocument.PageInfo
                    .Builder(bitmap.width, bitmap.height, pageNumber)
                    .create()
                val page = doc.startPage(info)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                doc.finishPage(page)
                bitmap.recycle()
                pageNumber++
            }
            out.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    private fun decodeBounded(file: File, maxEdgePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > maxEdgePx) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.path, opts)
    }
}
