package com.openscan.scanner.data

import java.io.File

/**
 * A single saved scan: a folder holding one or more perspective-corrected page
 * images plus a generated multi-page PDF.
 */
data class ScannedDocument(
    val id: String,
    val name: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val pdfFile: File,
    val pageImages: List<File>
) {
    val pageCount: Int get() = pageImages.size
    val thumbnailFile: File? get() = pageImages.firstOrNull()
}
