package com.openscan.scanner.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.io.image.ImageData
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {
    
    data class PdfSettings(
        val quality: Int = 90, // JPEG quality (0-100)
        val pageSize: PageSize = PageSize.A4,
        val compressionLevel: Int = 5, // PDF compression level (0-9)
        val fitToPage: Boolean = true
    )
    
    fun createPdf(
        imagePaths: List<String>,
        outputPath: String? = null,
        settings: PdfSettings = PdfSettings()
    ): Result<String> {
        return try {
            val fileName = outputPath ?: generateDefaultFileName()
            val pdfResult = createPdfInDownloads(fileName, imagePaths, settings)
            
            when {
                pdfResult.isSuccess -> Result.success(pdfResult.getOrThrow())
                else -> Result.failure(pdfResult.exceptionOrNull() ?: Exception("PDF creation failed"))
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun addImageToDocument(
        document: Document,
        imagePath: String,
        settings: PdfSettings
    ) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            
            // Compress bitmap if needed
            val compressedImageData = compressBitmap(bitmap, settings.quality)
            val imageData: ImageData = ImageDataFactory.create(compressedImageData)
            val image = Image(imageData)
            
            if (settings.fitToPage) {
                // Calculate scaling to fit page while maintaining aspect ratio
                val pageWidth = settings.pageSize.width - 40f // Account for margins
                val pageHeight = settings.pageSize.height - 40f // Account for margins
                
                val imageWidth = image.imageWidth
                val imageHeight = image.imageHeight
                
                val scaleX: Float = pageWidth / imageWidth
                val scaleY: Float = pageHeight / imageHeight
                val scale: Float = kotlin.math.min(scaleX, scaleY)
                
                image.scale(scale, scale)
                
                // Center the image on the page
                val x: Float = (pageWidth - imageWidth * scale) / 2f
                val y: Float = (pageHeight - imageHeight * scale) / 2f
                image.setFixedPosition(x, y)
            }
            
            document.add(image)
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue with other images even if one fails
        }
    }
    
    private fun compressBitmap(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
    
    private fun generateDefaultFileName(): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            .format(Date())
        return "OpenScan_$timestamp.pdf"
    }
    
    private fun createPdfInDownloads(
        fileName: String,
        imagePaths: List<String>,
        settings: PdfSettings
    ): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use MediaStore API
                createPdfWithMediaStore(fileName, imagePaths, settings)
            } else {
                // Older Android versions - Use legacy external storage
                createPdfLegacy(fileName, imagePaths, settings)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    @SuppressLint("NewApi")
    private fun createPdfWithMediaStore(
        fileName: String,
        imagePaths: List<String>,
        settings: PdfSettings
    ): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return Result.failure(Exception("MediaStore API not available"))
            }
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OpenScan")
            }
            
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(Exception("Failed to create PDF file"))
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                createPdfToStream(outputStream, imagePaths, settings)
            } ?: return Result.failure(Exception("Failed to open output stream"))
            
            // Get the actual file path for display
            val displayPath = "Downloads/OpenScan/$fileName"
            Result.success(displayPath)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun createPdfLegacy(
        fileName: String,
        imagePaths: List<String>,
        settings: PdfSettings
    ): Result<String> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val openScanDir = File(downloadsDir, "OpenScan")
            
            if (!openScanDir.exists()) {
                openScanDir.mkdirs()
            }
            
            val pdfFile = File(openScanDir, fileName)
            FileOutputStream(pdfFile).use { outputStream ->
                createPdfToStream(outputStream, imagePaths, settings)
            }
            
            Result.success(pdfFile.absolutePath)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun createPdfToStream(
        outputStream: OutputStream,
        imagePaths: List<String>,
        settings: PdfSettings
    ) {
        // Create PDF writer with compression
        val pdfWriter = PdfWriter(outputStream)
        pdfWriter.setCompressionLevel(settings.compressionLevel)
        
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument, settings.pageSize)
        
        // Set margins
        document.setMargins(20f, 20f, 20f, 20f)
        
        imagePaths.forEach { imagePath ->
            addImageToDocument(document, imagePath, settings)
        }
        
        document.close()
    }
    
    fun createSinglePagePdf(
        imagePath: String,
        outputPath: String? = null,
        settings: PdfSettings = PdfSettings()
    ): Result<String> {
        return createPdf(listOf(imagePath), outputPath, settings)
    }
    
    fun mergePdfs(
        pdfPaths: List<String>,
        outputPath: String? = null
    ): Result<String> {
        return try {
            // For merge operations, we'll create a temporary merged file first
            // then save it to Downloads. This is more complex due to PDF merging requirements.
            val fileName = outputPath ?: generateDefaultFileName()
            
            // Create temporary file for merging
            val tempFile = File.createTempFile("merge_", ".pdf", context.cacheDir)
            
            val pdfWriter = PdfWriter(tempFile.absolutePath)
            val pdfDocument = PdfDocument(pdfWriter)
            
            pdfPaths.forEach { pdfPath ->
                val sourcePdf = PdfDocument(com.itextpdf.kernel.pdf.PdfReader(pdfPath))
                sourcePdf.copyPagesTo(1, sourcePdf.numberOfPages, pdfDocument)
                sourcePdf.close()
            }
            
            pdfDocument.close()
            
            // Now save the merged PDF to Downloads
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveTempFileToDownloadsMediaStore(tempFile, fileName)
            } else {
                saveTempFileToDownloadsLegacy(tempFile, fileName)
            }
            
            // Clean up temp file
            tempFile.delete()
            
            result
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    fun addImageToPdf(
        existingPdfPath: String,
        imagePath: String,
        outputPath: String? = null,
        settings: PdfSettings = PdfSettings()
    ): Result<String> {
        return try {
            val fileName = outputPath ?: generateDefaultFileName()
            
            // Create temporary file for combining
            val tempFile = File.createTempFile("combine_", ".pdf", context.cacheDir)
            
            // Read existing PDF
            val sourcePdf = PdfDocument(com.itextpdf.kernel.pdf.PdfReader(existingPdfPath))
            
            // Create new PDF with additional page
            val pdfWriter = PdfWriter(tempFile.absolutePath)
            val pdfDocument = PdfDocument(pdfWriter)
            
            // Copy existing pages
            sourcePdf.copyPagesTo(1, sourcePdf.numberOfPages, pdfDocument)
            sourcePdf.close()
            
            // Add new image as a new page
            val document = Document(pdfDocument, settings.pageSize)
            document.setMargins(20f, 20f, 20f, 20f)
            addImageToDocument(document, imagePath, settings)
            document.close()
            
            // Save to Downloads
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveTempFileToDownloadsMediaStore(tempFile, fileName)
            } else {
                saveTempFileToDownloadsLegacy(tempFile, fileName)
            }
            
            // Clean up temp file
            tempFile.delete()
            
            result
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    fun getPdfInfo(pdfPath: String): Result<PdfInfo> {
        return try {
            val pdfDocument = PdfDocument(com.itextpdf.kernel.pdf.PdfReader(pdfPath))
            val info = PdfInfo(
                pageCount = pdfDocument.numberOfPages,
                fileSize = File(pdfPath).length(),
                creationDate = Date() // Could extract from PDF metadata
            )
            pdfDocument.close()
            
            Result.success(info)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    data class PdfInfo(
        val pageCount: Int,
        val fileSize: Long,
        val creationDate: Date
    )
    
    fun optimizePdf(
        inputPath: String,
        outputPath: String? = null,
        compressionLevel: Int = 9
    ): Result<String> {
        return try {
            val fileName = outputPath ?: generateDefaultFileName()
            
            // Create temporary file for optimization
            val tempFile = File.createTempFile("optimize_", ".pdf", context.cacheDir)
            
            val sourcePdf = PdfDocument(com.itextpdf.kernel.pdf.PdfReader(inputPath))
            val pdfWriter = PdfWriter(tempFile.absolutePath)
            pdfWriter.setCompressionLevel(compressionLevel)
            val optimizedPdf = PdfDocument(pdfWriter)
            
            sourcePdf.copyPagesTo(1, sourcePdf.numberOfPages, optimizedPdf)
            
            sourcePdf.close()
            optimizedPdf.close()
            
            // Save to Downloads
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveTempFileToDownloadsMediaStore(tempFile, fileName)
            } else {
                saveTempFileToDownloadsLegacy(tempFile, fileName)
            }
            
            // Clean up temp file
            tempFile.delete()
            
            result
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    @SuppressLint("NewApi")
    private fun saveTempFileToDownloadsMediaStore(tempFile: File, fileName: String): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return Result.failure(Exception("MediaStore API not available"))
            }
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OpenScan")
            }
            
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(Exception("Failed to create PDF file"))
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(Exception("Failed to open output stream"))
            
            val displayPath = "Downloads/OpenScan/$fileName"
            Result.success(displayPath)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun saveTempFileToDownloadsLegacy(tempFile: File, fileName: String): Result<String> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val openScanDir = File(downloadsDir, "OpenScan")
            
            if (!openScanDir.exists()) {
                openScanDir.mkdirs()
            }
            
            val targetFile = File(openScanDir, fileName)
            tempFile.copyTo(targetFile, overwrite = true)
            
            Result.success(targetFile.absolutePath)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
} 