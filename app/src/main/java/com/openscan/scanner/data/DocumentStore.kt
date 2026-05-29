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

/**
 * Stores finished scans in the app's external files directory and exposes
 * simple list / save / rename / delete / share / export operations.
 *
 * Layout (in getExternalFilesDir("documents")):
 *   <id>.pdf   - the document
 *   <id>.jpg   - first-page thumbnail (optional)
 *   <id>.meta  - "displayName\npageCount" sidecar
 */
class DocumentStore(private val context: Context) {

    private val dir: File by lazy {
        File(context.getExternalFilesDir(null), DIR_NAME).apply { mkdirs() }
    }

    private val authority: String get() = "${context.packageName}.fileprovider"

    /** All saved documents, newest first. */
    fun list(): List<ScannedDocument> {
        val pdfs = dir.listFiles { f -> f.isFile && f.extension.equals("pdf", true) }
            ?: return emptyList()
        return pdfs
            .sortedByDescending { it.lastModified() }
            .map { pdf ->
                val id = pdf.nameWithoutExtension
                val thumb = File(dir, "$id.jpg").takeIf { it.exists() }
                val (name, pages) = readMeta(id, fallbackName = id)
                ScannedDocument(
                    id = id,
                    name = name,
                    pageCount = pages,
                    createdAt = pdf.lastModified(),
                    sizeBytes = pdf.length(),
                    pdfFile = pdf,
                    thumbnailFile = thumb
                )
            }
    }

    /**
     * Persist a scan result. [pdfUri] is required; [firstPageUri] is used to
     * build a thumbnail if present.
     */
    fun save(pdfUri: Uri, firstPageUri: Uri?, pageCount: Int, displayName: String? = null): ScannedDocument {
        val timestamp = System.currentTimeMillis()
        val id = "scan_" + FILE_STAMP.format(Date(timestamp))
        val pdfFile = File(dir, "$id.pdf")
        copyUriToFile(pdfUri, pdfFile)

        var thumb: File? = null
        if (firstPageUri != null) {
            val t = File(dir, "$id.jpg")
            runCatching { copyUriToFile(firstPageUri, t) }.onSuccess { thumb = t }
        }

        val name = displayName ?: ("Scan " + DISPLAY_STAMP.format(Date(timestamp)))
        writeMeta(id, name, pageCount)

        return ScannedDocument(
            id = id,
            name = name,
            pageCount = pageCount,
            createdAt = timestamp,
            sizeBytes = pdfFile.length(),
            pdfFile = pdfFile,
            thumbnailFile = thumb
        )
    }

    fun rename(doc: ScannedDocument, newName: String) {
        val trimmed = newName.trim().ifEmpty { doc.name }
        writeMeta(doc.id, trimmed, doc.pageCount)
    }

    fun delete(doc: ScannedDocument) {
        doc.pdfFile.delete()
        doc.thumbnailFile?.delete()
        File(dir, "${doc.id}.meta").delete()
    }

    /** A content:// uri suitable for sharing/viewing the given file. */
    fun contentUri(file: File): Uri =
        FileProvider.getUriForFile(context, authority, file)

    /**
     * Copy the document's PDF into the public Downloads collection.
     * Returns the saved file name on success, or null on failure.
     */
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
                val dest = File(downloads, fileName)
                doc.pdfFile.inputStream().use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
            }
            fileName
        }.getOrNull()
    }

    // ---- internals ----

    private fun copyUriToFile(uri: Uri, dest: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open input stream for $uri" }
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun writeMeta(id: String, name: String, pages: Int) {
        File(dir, "$id.meta").writeText("$name\n$pages")
    }

    private fun readMeta(id: String, fallbackName: String): Pair<String, Int> {
        val meta = File(dir, "$id.meta")
        if (!meta.exists()) return fallbackName to 0
        val lines = runCatching { meta.readLines() }.getOrDefault(emptyList())
        val name = lines.getOrNull(0)?.takeIf { it.isNotBlank() } ?: fallbackName
        val pages = lines.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return name to pages
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9-_ ]"), "_").trim().ifEmpty { "document" }

    companion object {
        private const val DIR_NAME = "documents"
        private val FILE_STAMP = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        private val DISPLAY_STAMP = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    }
}
