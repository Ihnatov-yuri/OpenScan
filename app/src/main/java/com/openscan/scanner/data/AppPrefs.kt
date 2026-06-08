package com.openscan.scanner.data

import android.content.Context

/** PDF output quality, expressed as the longest page edge in pixels. */
enum class PdfQuality(val label: String, val maxEdgePx: Int) {
    HIGH("High — sharpest, largest file", 3000),
    MEDIUM("Medium — balanced", 2000),
    LOW("Low — smallest file", 1240)
}

/** How the document library is laid out. */
enum class LibraryView { LIST, GRID }

/** Tiny SharedPreferences-backed settings store. */
class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("openscan_prefs", Context.MODE_PRIVATE)

    var pdfQuality: PdfQuality
        get() = runCatching {
            PdfQuality.valueOf(prefs.getString(KEY_PDF_QUALITY, PdfQuality.HIGH.name)!!)
        }.getOrDefault(PdfQuality.HIGH)
        set(value) {
            prefs.edit().putString(KEY_PDF_QUALITY, value.name).apply()
        }

    var libraryView: LibraryView
        get() = runCatching {
            LibraryView.valueOf(prefs.getString(KEY_LIBRARY_VIEW, LibraryView.LIST.name)!!)
        }.getOrDefault(LibraryView.LIST)
        set(value) {
            prefs.edit().putString(KEY_LIBRARY_VIEW, value.name).apply()
        }

    private companion object {
        const val KEY_PDF_QUALITY = "pdf_quality"
        const val KEY_LIBRARY_VIEW = "library_view"
    }
}
