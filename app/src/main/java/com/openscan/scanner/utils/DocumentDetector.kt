package com.openscan.scanner.utils

import android.graphics.Bitmap
import android.graphics.PointF
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

class DocumentDetector {
    
    data class DocumentBounds(
        val topLeft: PointF,
        val topRight: PointF,
        val bottomLeft: PointF,
        val bottomRight: PointF
    ) {
        fun toArray(): FloatArray {
            return floatArrayOf(
                topLeft.x, topLeft.y,
                topRight.x, topRight.y,
                bottomRight.x, bottomRight.y,
                bottomLeft.x, bottomLeft.y
            )
        }
        
        fun scale(scaleX: Float, scaleY: Float): DocumentBounds {
            return DocumentBounds(
                topLeft = PointF(topLeft.x * scaleX, topLeft.y * scaleY),
                topRight = PointF(topRight.x * scaleX, topRight.y * scaleY),
                bottomLeft = PointF(bottomLeft.x * scaleX, bottomLeft.y * scaleY),
                bottomRight = PointF(bottomRight.x * scaleX, bottomRight.y * scaleY)
            )
        }
        
        fun isValid(imageWidth: Int, imageHeight: Int): Boolean {
            val points = listOf(topLeft, topRight, bottomLeft, bottomRight)
            return points.all { point ->
                point.x >= 0 && point.x <= imageWidth &&
                point.y >= 0 && point.y <= imageHeight
            }
        }
    }
    
    init {
        // OpenCV initialization is handled by the dependency
    }
    
    /**
     * Detect document boundaries in a bitmap for high-quality processing
     */
    fun detectDocument(bitmap: Bitmap): DocumentBounds? {
        return detectDocumentInternal(bitmap, highQuality = true)
    }
    
    /**
     * Detect document boundaries optimized for real-time camera preview
     */
    fun detectDocumentRealTime(bitmap: Bitmap): DocumentBounds? {
        return detectDocumentInternal(bitmap, highQuality = false)
    }
    
    /**
     * Create default document bounds covering the entire image
     * Used as fallback when automatic detection fails
     */
    fun createDefaultBounds(width: Int, height: Int): DocumentBounds {
        val margin = 0.05f // 5% margin from edges
        val marginX = width * margin
        val marginY = height * margin
        
        return DocumentBounds(
            topLeft = PointF(marginX, marginY),
            topRight = PointF(width - marginX, marginY),
            bottomLeft = PointF(marginX, height - marginY),
            bottomRight = PointF(width - marginX, height - marginY)
        )
    }
    
    private fun detectDocumentInternal(bitmap: Bitmap, highQuality: Boolean): DocumentBounds? {
        var mat: Mat? = null
        var grayMat: Mat? = null
        var blurredMat: Mat? = null
        var edgesMat: Mat? = null
        var hierarchy: Mat? = null
        
        try {
            mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            
            // Resize for performance if this is real-time detection
            val workingMat = if (!highQuality && (mat.width() > 800 || mat.height() > 800)) {
                val scale = 800.0 / kotlin.math.max(mat.width(), mat.height())
                val resizedMat = Mat()
                Imgproc.resize(mat, resizedMat, Size(mat.width() * scale, mat.height() * scale))
                mat.release()
                resizedMat
            } else {
                mat
            }
            
            // Convert to grayscale
            grayMat = Mat()
            Imgproc.cvtColor(workingMat, grayMat, Imgproc.COLOR_RGB2GRAY)
            
            // Apply Gaussian blur to reduce noise
            blurredMat = Mat()
            val kernelSize = if (highQuality) 5.0 else 3.0
            Imgproc.GaussianBlur(grayMat, blurredMat, Size(kernelSize, kernelSize), 0.0)
            
            // Edge detection using Canny with improved parameters
            edgesMat = Mat()
            val lowThreshold = if (highQuality) 30.0 else 50.0
            val highThreshold = if (highQuality) 100.0 else 150.0
            Imgproc.Canny(blurredMat, edgesMat, lowThreshold, highThreshold)
            
            // Apply morphological operations to improve edge detection
            val kernelSize2 = if (highQuality) 5.0 else 3.0
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(kernelSize2, kernelSize2))
            Imgproc.morphologyEx(edgesMat, edgesMat, Imgproc.MORPH_CLOSE, kernel)
            
            // Find contours
            val contours = mutableListOf<MatOfPoint>()
            hierarchy = Mat()
            Imgproc.findContours(
                edgesMat,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            
            // Find the largest contour that could be a document
            val documentContour = findDocumentContour(contours, workingMat.size(), highQuality)
            
            val result = documentContour?.let { contour ->
                val corners = getDocumentCorners(contour, highQuality)
                if (corners.size == 4) {
                    var bounds = DocumentBounds(
                        topLeft = PointF(corners[0].x.toFloat(), corners[0].y.toFloat()),
                        topRight = PointF(corners[1].x.toFloat(), corners[1].y.toFloat()),
                        bottomRight = PointF(corners[2].x.toFloat(), corners[2].y.toFloat()),
                        bottomLeft = PointF(corners[3].x.toFloat(), corners[3].y.toFloat())
                    )
                    
                    // Scale back to original size if we resized
                    if (workingMat != mat) {
                        val scaleBack = kotlin.math.max(bitmap.width, bitmap.height).toFloat() / 800f
                        bounds = bounds.scale(scaleBack, scaleBack)
                    }
                    
                    bounds
                } else null
            }
            
            // Clean up contours
            contours.forEach { it.release() }
            
            return result
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            // Clean up OpenCV resources to prevent memory leaks
            hierarchy?.release()
            edgesMat?.release()
            blurredMat?.release()
            grayMat?.release()
            mat?.release()
        }
    }
    
    private fun findDocumentContour(contours: List<MatOfPoint>, imageSize: Size, highQuality: Boolean): MatOfPoint? {
        val minAreaRatio = if (highQuality) 0.01 else 0.02 // More sensitive for high quality
        val maxAreaRatio = if (highQuality) 0.98 else 0.95
        val minPerimeter = if (highQuality) 200 else 100
        
        val minArea = imageSize.area() * minAreaRatio
        val maxArea = imageSize.area() * maxAreaRatio
        
        return contours
            .filter { contour ->
                val area = Imgproc.contourArea(contour)
                val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                area > minArea && area < maxArea && perimeter > minPerimeter
            }
            .maxByOrNull { contour ->
                // Score based on area and aspect ratio
                val area = Imgproc.contourArea(contour)
                val rect = Imgproc.boundingRect(contour)
                val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
                val aspectScore = when {
                    aspectRatio > 0.5 && aspectRatio < 2.0 -> 1.0 // Good aspect ratio
                    aspectRatio > 0.3 && aspectRatio < 3.0 -> 0.7 // Acceptable
                    else -> 0.3 // Poor aspect ratio
                }
                area * aspectScore
            }
    }
    
    private fun getDocumentCorners(contour: MatOfPoint, highQuality: Boolean): List<Point> {
        var contour2f: MatOfPoint2f? = null
        var approx: MatOfPoint2f? = null
        
        return try {
            // Approximate contour to polygon
            contour2f = MatOfPoint2f()
            contour.convertTo(contour2f, CvType.CV_32FC2)
            
            approx = MatOfPoint2f()
            val epsilonFactors = if (highQuality) 
                doubleArrayOf(0.008, 0.015, 0.025, 0.04) 
            else 
                doubleArrayOf(0.01, 0.02, 0.03, 0.05)
            
            var bestApprox: Array<Point>? = null
            
            // Try different epsilon values for better approximation
            for (epsilonFactor in epsilonFactors) {
                val epsilon = epsilonFactor * Imgproc.arcLength(contour2f, true)
                Imgproc.approxPolyDP(contour2f, approx, epsilon, true)
                
                val points = approx.toArray()
                if (points.size == 4) {
                    bestApprox = points
                    break
                } else if (points.size > 4 && bestApprox == null) {
                    // Keep the best approximation so far
                    bestApprox = points
                }
            }
            
            val finalPoints = bestApprox ?: approx.toArray()
            
            if (finalPoints.size >= 4) {
                // If we have exactly 4 points, use them
                if (finalPoints.size == 4) {
                    sortPoints(finalPoints.toList())
                } else {
                    // If we have more than 4 points, try to find the best 4 corner points
                    findBestCorners(finalPoints.toList())
                }
            } else {
                // If we don't get enough points, use bounding rectangle
                val boundingRect = Imgproc.boundingRect(contour)
                listOf(
                    Point(boundingRect.x.toDouble(), boundingRect.y.toDouble()),
                    Point((boundingRect.x + boundingRect.width).toDouble(), boundingRect.y.toDouble()),
                    Point((boundingRect.x + boundingRect.width).toDouble(), (boundingRect.y + boundingRect.height).toDouble()),
                    Point(boundingRect.x.toDouble(), (boundingRect.y + boundingRect.height).toDouble())
                )
            }
        } finally {
            // Clean up OpenCV resources
            approx?.release()
            contour2f?.release()
        }
    }
    
    private fun findBestCorners(points: List<Point>): List<Point> {
        if (points.size <= 4) return sortPoints(points)
        
        // Find the convex hull to get the outer boundary
        val pointsArray = points.toTypedArray()
        val hull = mutableListOf<Int>()
        val pointsMat = MatOfPoint(*pointsArray)
        val hullMat = MatOfInt()
        
        try {
            Imgproc.convexHull(pointsMat, hullMat)
            val hullIndices = hullMat.toArray()
            
            val hullPoints = hullIndices.map { points[it] }
            
            if (hullPoints.size >= 4) {
                // Find the 4 corner points from the convex hull
                val corners = findExtremePoints(hullPoints)
                return sortPoints(corners)
            }
            
        } finally {
            pointsMat.release()
            hullMat.release()
        }
        
        return sortPoints(points.take(4))
    }
    
    private fun findExtremePoints(points: List<Point>): List<Point> {
        if (points.size <= 4) return points
        
        // Find extreme points in different directions
        val leftMost = points.minByOrNull { it.x }!!
        val rightMost = points.maxByOrNull { it.x }!!
        val topMost = points.minByOrNull { it.y }!!
        val bottomMost = points.maxByOrNull { it.y }!!
        
        // Remove duplicates and return up to 4 unique points
        val extremePoints = listOf(leftMost, rightMost, topMost, bottomMost).distinct()
        
        return if (extremePoints.size >= 4) {
            extremePoints.take(4)
        } else {
            // Add more points if needed
            val remaining = points.filter { it !in extremePoints }
            val center = Point(
                points.map { it.x }.average(),
                points.map { it.y }.average()
            )
            val additionalPoints = remaining.sortedBy { 
                val dx = it.x - center.x
                val dy = it.y - center.y
                dx * dx + dy * dy
            }
            
            (extremePoints + additionalPoints).take(4)
        }
    }
    
    private fun sortPoints(points: List<Point>): List<Point> {
        // Find center point
        val centerX = points.map { it.x }.average()
        val centerY = points.map { it.y }.average()
        
        // Classify points based on their position relative to center
        var topLeft: Point? = null
        var topRight: Point? = null
        var bottomLeft: Point? = null
        var bottomRight: Point? = null
        
        for (point in points) {
            if (point.x < centerX && point.y < centerY) {
                if (topLeft == null || (point.x + point.y) < (topLeft.x + topLeft.y)) {
                    topLeft = point
                }
            } else if (point.x > centerX && point.y < centerY) {
                if (topRight == null || (point.x - point.y) > (topRight.x - topRight.y)) {
                    topRight = point
                }
            } else if (point.x < centerX && point.y > centerY) {
                if (bottomLeft == null || (point.y - point.x) > (bottomLeft.y - bottomLeft.x)) {
                    bottomLeft = point
                }
            } else if (point.x > centerX && point.y > centerY) {
                if (bottomRight == null || (point.x + point.y) > (bottomRight.x + bottomRight.y)) {
                    bottomRight = point
                }
            }
        }
        
        // Fallback to sorted order if classification fails
        val result = listOfNotNull(topLeft, topRight, bottomRight, bottomLeft)
        return if (result.size == 4) {
            result
        } else {
            // Fallback: sort by angle from center
            points.sortedBy { point ->
                atan2(point.y - centerY, point.x - centerX)
            }.take(4)
        }
    }
    
    fun calculateDocumentArea(bounds: DocumentBounds): Float {
        val width1 = distance(bounds.topLeft, bounds.topRight)
        val width2 = distance(bounds.bottomLeft, bounds.bottomRight)
        val height1 = distance(bounds.topLeft, bounds.bottomLeft)
        val height2 = distance(bounds.topRight, bounds.bottomRight)
        
        val avgWidth = (width1 + width2) / 2
        val avgHeight = (height1 + height2) / 2
        
        return avgWidth * avgHeight
    }
    
    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
} 