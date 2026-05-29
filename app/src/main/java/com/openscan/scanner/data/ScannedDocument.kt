package com.openscan.scanner.data

import java.io.File

/**
 * A single saved scan: one PDF (possibly multi-page) plus an optional
 * thumbnail generated from its first page.
 */
data class ScannedDocument(
    val id: String,
    val name: String,
    val pageCount: Int,
    val createdAt: Long,
    val sizeBytes: Long,
    val pdfFile: File,
    val thumbnailFile: File?
)
