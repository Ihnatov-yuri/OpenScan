package com.openscan.scanner.scanner

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Thin wrapper around the ML Kit Document Scanner client configuration.
 *
 * SCANNER_MODE_FULL enables the complete editing experience: automatic edge
 * detection, manual corner adjustment, perspective correction, rotation, and
 * image filters (color / grayscale / auto-enhance). This is what lets users
 * "fix" a page that was scanned incorrectly before saving.
 */
object DocumentScanner {

    const val MAX_PAGES = 30

    fun client() = GmsDocumentScanning.getClient(
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(MAX_PAGES)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    )

    /** Convenience re-export so callers don't need to import the ML Kit type directly. */
    fun resultFrom(intent: android.content.Intent?): GmsDocumentScanningResult? =
        GmsDocumentScanningResult.fromActivityResultIntent(intent)
}
