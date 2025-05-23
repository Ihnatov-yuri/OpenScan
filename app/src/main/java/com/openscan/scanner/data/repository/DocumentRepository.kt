package com.openscan.scanner.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DocumentRepository(private val context: Context) {
    
    private val documentsDir = File(context.getExternalFilesDir(null), "Documents")
    private val pdfsDir = File(context.getExternalFilesDir(null), "PDFs")
    
    init {
        // Create directories if they don't exist
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        if (!pdfsDir.exists()) {
            pdfsDir.mkdirs()
        }
    }
    
    fun getAllDocuments(): List<String> {
        return try {
            documentsDir.listFiles()
                ?.filter { it.isFile && (it.extension == "jpg" || it.extension == "jpeg" || it.extension == "png") }
                ?.map { it.absolutePath }
                ?.sortedByDescending { File(it).lastModified() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getAllPdfs(): List<String> {
        return try {
            pdfsDir.listFiles()
                ?.filter { it.isFile && it.extension == "pdf" }
                ?.map { it.absolutePath }
                ?.sortedByDescending { File(it).lastModified() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun deleteDocument(documentPath: String): Boolean {
        return try {
            File(documentPath).delete()
        } catch (e: Exception) {
            false
        }
    }
    
    fun shareDocument(documentPath: String): Intent? {
        return try {
            val file = File(documentPath)
            if (!file.exists()) return null
            
            val uri = FileProvider.getUriForFile(
                context,
                "com.openscan.scanner.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = when (file.extension.lowercase()) {
                    "pdf" -> "application/pdf"
                    else -> "image/*"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Intent.createChooser(shareIntent, "Share Document")
            
        } catch (e: Exception) {
            null
        }
    }
    
    fun getDocumentInfo(documentPath: String): DocumentInfo? {
        return try {
            val file = File(documentPath)
            if (!file.exists()) return null
            
            DocumentInfo(
                name = file.name,
                size = file.length(),
                lastModified = Date(file.lastModified()),
                type = when (file.extension.lowercase()) {
                    "pdf" -> DocumentType.PDF
                    else -> DocumentType.IMAGE
                }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun renameDocument(oldPath: String, newName: String): String? {
        return try {
            val oldFile = File(oldPath)
            if (!oldFile.exists()) return null
            
            val extension = oldFile.extension
            val newFile = File(oldFile.parent, "$newName.$extension")
            
            if (oldFile.renameTo(newFile)) {
                newFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun copyDocument(sourcePath: String, targetDir: File? = null): String? {
        return try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return null
            
            val targetDirectory = targetDir ?: documentsDir
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val targetFile = File(targetDirectory, "copy_${timestamp}_${sourceFile.name}")
            
            sourceFile.copyTo(targetFile, overwrite = true)
            targetFile.absolutePath
            
        } catch (e: Exception) {
            null
        }
    }
    
    fun getStorageInfo(): StorageInfo {
        val totalSpace = documentsDir.totalSpace
        val freeSpace = documentsDir.freeSpace
        val usedSpace = totalSpace - freeSpace
        
        val documentCount = getAllDocuments().size
        val pdfCount = getAllPdfs().size
        
        return StorageInfo(
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            usedSpace = usedSpace,
            documentCount = documentCount,
            pdfCount = pdfCount
        )
    }
    
    fun cleanupOldDocuments(maxAge: Long = 30 * 24 * 60 * 60 * 1000L): Int { // 30 days default
        val cutoffTime = System.currentTimeMillis() - maxAge
        var deletedCount = 0
        
        try {
            documentsDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            pdfsDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
        } catch (e: Exception) {
            // Handle cleanup errors
        }
        
        return deletedCount
    }
    
    data class DocumentInfo(
        val name: String,
        val size: Long,
        val lastModified: Date,
        val type: DocumentType
    )
    
    enum class DocumentType {
        IMAGE,
        PDF
    }
    
    data class StorageInfo(
        val totalSpace: Long,
        val freeSpace: Long,
        val usedSpace: Long,
        val documentCount: Int,
        val pdfCount: Int
    )
} 