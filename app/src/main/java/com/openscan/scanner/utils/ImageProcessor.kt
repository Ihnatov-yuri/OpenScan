package com.openscan.scanner.utils

import android.content.Context
import android.graphics.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ImageProcessor(private val context: Context) {
    
    enum class ColorMode {
        COLOR,
        GRAYSCALE,
        BLACK_AND_WHITE
    }
    
    fun processDocument(
        bitmap: Bitmap,
        bounds: DocumentDetector.DocumentBounds?,
        colorMode: ColorMode = ColorMode.COLOR,
        enhanceContrast: Boolean = true
    ): Bitmap {
        var mat: Mat? = null
        var correctedMat: Mat? = null
        var processedMat: Mat? = null
        
        return try {
            mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            
            // Apply perspective correction if bounds are detected
            correctedMat = bounds?.let { applyPerspectiveCorrection(mat, it) } ?: mat
            
            // Apply color mode
            processedMat = when (colorMode) {
                ColorMode.COLOR -> {
                    if (enhanceContrast) enhanceContrast(correctedMat) else correctedMat
                }
                ColorMode.GRAYSCALE -> convertToGrayscale(correctedMat, enhanceContrast)
                ColorMode.BLACK_AND_WHITE -> convertToBlackAndWhite(correctedMat)
            }
            
            // Convert back to bitmap
            val resultBitmap = Bitmap.createBitmap(
                processedMat.cols(),
                processedMat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(processedMat, resultBitmap)
            
            resultBitmap
            
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap // Return original bitmap if processing fails
        } finally {
            // Clean up OpenCV resources to prevent memory leaks
            if (processedMat != null && processedMat != correctedMat) {
                processedMat.release()
            }
            if (correctedMat != null && correctedMat != mat) {
                correctedMat.release()
            }
            mat?.release()
        }
    }
    
    private fun applyPerspectiveCorrection(
        mat: Mat,
        bounds: DocumentDetector.DocumentBounds
    ): Mat {
        try {
            // Source points (detected corners)
            val srcPoints = MatOfPoint2f(
                Point(bounds.topLeft.x.toDouble(), bounds.topLeft.y.toDouble()),
                Point(bounds.topRight.x.toDouble(), bounds.topRight.y.toDouble()),
                Point(bounds.bottomRight.x.toDouble(), bounds.bottomRight.y.toDouble()),
                Point(bounds.bottomLeft.x.toDouble(), bounds.bottomLeft.y.toDouble())
            )
            
            // Calculate the dimensions of the corrected document
            val width1 = distance(bounds.topLeft, bounds.topRight)
            val width2 = distance(bounds.bottomLeft, bounds.bottomRight)
            val height1 = distance(bounds.topLeft, bounds.bottomLeft)
            val height2 = distance(bounds.topRight, bounds.bottomRight)
            
            val maxWidth = max(width1, width2).toInt()
            val maxHeight = max(height1, height2).toInt()
            
            // Destination points (rectangle)
            val dstPoints = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(maxWidth.toDouble(), 0.0),
                Point(maxWidth.toDouble(), maxHeight.toDouble()),
                Point(0.0, maxHeight.toDouble())
            )
            
            // Get perspective transformation matrix
            val transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
            
            // Apply transformation
            val correctedMat = Mat()
            Imgproc.warpPerspective(
                mat,
                correctedMat,
                transformMatrix,
                Size(maxWidth.toDouble(), maxHeight.toDouble())
            )
            
            return correctedMat
            
        } catch (e: Exception) {
            e.printStackTrace()
            return mat
        }
    }
    
    private fun enhanceContrast(mat: Mat): Mat {
        val enhanced = Mat()
        
        // Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        
        if (mat.channels() == 1) {
            // Grayscale image
            clahe.apply(mat, enhanced)
        } else {
            // Color image - convert to LAB color space
            val labMat = Mat()
            Imgproc.cvtColor(mat, labMat, Imgproc.COLOR_RGB2Lab)
            
            val labChannels = mutableListOf<Mat>()
            Core.split(labMat, labChannels)
            
            // Apply CLAHE to L channel only
            clahe.apply(labChannels[0], labChannels[0])
            
            Core.merge(labChannels, labMat)
            Imgproc.cvtColor(labMat, enhanced, Imgproc.COLOR_Lab2RGB)
        }
        
        return enhanced
    }
    
    private fun convertToGrayscale(mat: Mat, enhanceContrast: Boolean = true): Mat {
        val grayMat = if (mat.channels() > 1) {
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            gray
        } else {
            mat.clone()
        }
        
        return if (enhanceContrast) {
            enhanceContrast(grayMat)
        } else {
            grayMat
        }
    }
    
    private fun convertToBlackAndWhite(mat: Mat): Mat {
        val grayMat = if (mat.channels() > 1) {
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
            gray
        } else {
            mat.clone()
        }
        
        // Apply adaptive threshold for better results
        val bwMat = Mat()
        Imgproc.adaptiveThreshold(
            grayMat,
            bwMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )
        
        return bwMat
    }
    
    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
    
    fun saveBitmap(bitmap: Bitmap, filePath: String, quality: Int = 90): Boolean {
        return try {
            FileOutputStream(filePath).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun cropBitmap(bitmap: Bitmap, bounds: DocumentDetector.DocumentBounds): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Create a path from the bounds
        val path = Path().apply {
            moveTo(bounds.topLeft.x, bounds.topLeft.y)
            lineTo(bounds.topRight.x, bounds.topRight.y)
            lineTo(bounds.bottomRight.x, bounds.bottomRight.y)
            lineTo(bounds.bottomLeft.x, bounds.bottomLeft.y)
            close()
        }
        
        // Create a bitmap with the path
        val croppedBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(croppedBitmap)
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return croppedBitmap
    }
    
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    
    fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)
        
        if (scale >= 1.0f) return bitmap
        
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            width,
            height,
            matrix,
            true
        )
    }
} 