package com.openscan.scanner.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ordering options for the document library. */
enum class SortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME("Name (A–Z)"),
    SIZE("Largest first")
}

/**
 * Stores scans as one folder per document under getExternalFilesDir("documents"):
 *
 *   <id>/
 *     page_000.jpg, page_001.jpg, ...   the corrected page images
 *     document.pdf                      generated multi-page PDF
 *     name.txt                          display name
 *
 * Keeping the page images lets us append pages, regenerate the PDF, and run OCR.
 */
class DocumentStore(private val context: Context, private val prefs: AppPrefs) {

    private val root: File by lazy {
        File(context.getExternalFilesDir(null), DIR_NAME).apply { mkdirs() }
    }

    private val authority: String get() = "${context.packageName}.fileprovider"

    fun list(sort: SortOrder = SortOrder.NEWEST): List<ScannedDocument> {
        val folders = root.listFiles { f -> f.isDirectory } ?: return emptyList()
        val docs = folders.mapNotNull { read(it) }
        return when (sort) {
            SortOrder.NEWEST -> docs.sortedByDescending { it.createdAt }
            SortOrder.OLDEST -> docs.sortedBy { it.createdAt }
            SortOrder.NAME -> docs.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SortOrder.SIZE -> docs.sortedByDescending { it.sizeBytes }
        }
    }

    /** Create a new document from freshly scanned page images. */
    fun save(pageUris: List<Uri>, displayName: String? = null): ScannedDocument {
        val timestamp = System.currentTimeMillis()
        val id = "scan_" + FILE_STAMP.format(Date(timestamp))
        val dir = File(root, id).apply { mkdirs() }

        copyPages(pageUris, dir, startIndex = 0)
        writeName(dir, displayName ?: ("Scan " + DISPLAY_STAMP.format(Date(timestamp))))
        regeneratePdf(dir)

        return requireNotNull(read(dir)) { "Failed to save document" }
    }

    /** Append more scanned pages to an existing document and rebuild its PDF. */
    fun append(doc: ScannedDocument, pageUris: List<Uri>): ScannedDocument {
        val dir = doc.pdfFile.parentFile ?: error("Missing document folder")
        copyPages(pageUris, dir, startIndex = doc.pageImages.size)
        regeneratePdf(dir)
        return requireNotNull(read(dir)) { "Failed to append pages" }
    }

    /** Reorder the document's pages (moving the page at [from] to [to]) and rebuild its PDF. */
    fun reorderPage(doc: ScannedDocument, from: Int, to: Int): ScannedDocument {
        val dir = doc.pdfFile.parentFile ?: error("Missing document folder")
        val pages = doc.pageImages.toMutableList()
        if (from !in pages.indices || to !in pages.indices) return doc
        pages.add(to, pages.removeAt(from))
        applyPageOrder(dir, pages)
        regeneratePdf(dir)
        return requireNotNull(read(dir)) { "Failed to reorder pages" }
    }

    /**
     * Delete a single page and rebuild the PDF. Returns the updated document, or
     * null if that was the last page (in which case the whole document is removed).
     */
    fun deletePage(doc: ScannedDocument, index: Int): ScannedDocument? {
        val dir = doc.pdfFile.parentFile ?: return doc
        val pages = doc.pageImages.toMutableList()
        if (index !in pages.indices) return doc
        pages.removeAt(index)
        if (pages.isEmpty()) {
            delete(doc)
            return null
        }
        applyPageOrder(dir, pages)
        regeneratePdf(dir)
        return read(dir)
    }

    fun rename(doc: ScannedDocument, newName: String) {
        val dir = doc.pdfFile.parentFile ?: return
        writeName(dir, newName.trim().ifEmpty { doc.name })
    }

    fun delete(doc: ScannedDocument) {
        doc.pdfFile.parentFile?.deleteRecursively()
    }

    fun contentUri(file: File): Uri = FileProvider.getUriForFile(context, authority, file)

    /** Total bytes used by all saved documents. */
    fun totalBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun deleteAll() {
        root.deleteRecursively()
        root.mkdirs()
    }

    /** Copy the document's PDF into the public Downloads collection. */
    fun exportToDownloads(doc: ScannedDocument): String? {
        val fileName = sanitize(doc.name) + ".pdf"
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)!!.use { out ->
                    doc.pdfFile.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val downloads =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloads.mkdirs()
                doc.pdfFile.inputStream().use { input ->
                    File(downloads, fileName).outputStream().use { input.copyTo(it) }
                }
            }
            fileName
        }.getOrNull()
    }

    // ---- internals ----

    private fun read(dir: File): ScannedDocument? {
        val pages = dir.listFiles { f -> f.isFile && f.name.startsWith(PAGE_PREFIX) }
            ?.sortedBy { it.name }
            ?: emptyList()
        val pdf = File(dir, PDF_NAME)
        if (!pdf.exists() || pages.isEmpty()) return null
        val id = dir.name
        return ScannedDocument(
            id = id,
            name = readName(dir, fallback = id),
            createdAt = parseTimestamp(id) ?: dir.lastModified(),
            sizeBytes = pdf.length(),
            pdfFile = pdf,
            pageImages = pages
        )
    }

    private fun copyPages(pageUris: List<Uri>, dir: File, startIndex: Int) {
        pageUris.forEachIndexed { offset, uri ->
            val dest = File(dir, "%s%03d.jpg".format(PAGE_PREFIX, startIndex + offset))
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open $uri" }
                dest.outputStream().use { input.copyTo(it) }
            }
        }
    }

    private fun regeneratePdf(dir: File) {
        val pages = dir.listFiles { f -> f.isFile && f.name.startsWith(PAGE_PREFIX) }
            ?.sortedBy { it.name } ?: emptyList()
        PdfBuilder.build(pages, File(dir, PDF_NAME), prefs.pdfQuality.maxEdgePx)
    }

    /**
     * Rewrite the page image files so that [kept] becomes page_000, page_001, …
     * (in the given order). Any existing page file not in [kept] is deleted.
     * Renames go through temp names first to avoid clobbering.
     */
    private fun applyPageOrder(dir: File, kept: List<File>) {
        val keptPaths = kept.map { it.absolutePath }.toSet()
        dir.listFiles { f -> f.isFile && f.name.startsWith(PAGE_PREFIX) }?.forEach { existing ->
            if (existing.absolutePath !in keptPaths) existing.delete()
        }
        val temps = kept.mapIndexed { i, file ->
            val tmp = File(dir, "tmp_%03d.jpg".format(i))
            file.renameTo(tmp)
            tmp
        }
        temps.forEachIndexed { i, tmp ->
            tmp.renameTo(File(dir, "%s%03d.jpg".format(PAGE_PREFIX, i)))
        }
    }

    private fun writeName(dir: File, name: String) = File(dir, NAME_FILE).writeText(name)

    private fun readName(dir: File, fallback: String): String {
        val f = File(dir, NAME_FILE)
        return if (f.exists()) f.readText().trim().ifEmpty { fallback } else fallback
    }

    private fun parseTimestamp(id: String): Long? =
        runCatching { FILE_STAMP.parse(id.removePrefix("scan_"))?.time }.getOrNull()

    private fun sanitize(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9-_ ]"), "_").trim().ifEmpty { "document" }

    companion object {
        private const val DIR_NAME = "documents"
        private const val PAGE_PREFIX = "page_"
        private const val PDF_NAME = "document.pdf"
        private const val NAME_FILE = "name.txt"
        private val FILE_STAMP = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        private val DISPLAY_STAMP = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    }
}
