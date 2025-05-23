package com.openscan.scanner.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.openscan.scanner.utils.DocumentDetector
import com.openscan.scanner.utils.ImageProcessor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReview: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCaptured by remember { mutableStateOf(false) }
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var detectedBounds by remember { mutableStateOf<DocumentDetector.DocumentBounds?>(null) }
    var manualMode by remember { mutableStateOf(false) }
    var showManualOverlay by remember { mutableStateOf(false) }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    val documentDetector = remember { DocumentDetector() }

    if (hasCameraPermission) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Scan Document") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Manual mode toggle
                        IconButton(
                            onClick = { 
                                manualMode = !manualMode
                                if (manualMode && detectedBounds != null) {
                                    showManualOverlay = true
                                } else {
                                    showManualOverlay = false
                                }
                            }
                        ) {
                            Icon(
                                if (manualMode) Icons.Default.TouchApp else Icons.Default.AutoFixHigh,
                                contentDescription = if (manualMode) "Auto Mode" else "Manual Mode",
                                tint = if (manualMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Capture button
                        IconButton(
                            onClick = {
                                if (!isCaptured) {
                                    captureImage(
                                        imageCapture = imageCapture,
                                        context = context,
                                        onImageCaptured = { path ->
                                            capturedImagePath = path
                                            isCaptured = true
                                        },
                                        onError = { /* Handle error */ }
                                    )
                                }
                            },
                            enabled = !isProcessing && imageCapture != null
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (hasCameraPermission) {
                    if (!isCaptured) {
                        // Camera preview with overlay
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { context ->
                                    PreviewView(context).apply {
                                        previewView = this
                                        startCamera(
                                            context = context,
                                            lifecycleOwner = lifecycleOwner,
                                            previewView = this,
                                            onImageCaptureReady = { capture ->
                                                imageCapture = capture
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Document detection overlay
                            if (!manualMode) {
                                DocumentDetectionOverlay(
                                    bounds = detectedBounds,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            // Manual adjustment overlay
                            if (showManualOverlay && detectedBounds != null) {
                                ManualBorderAdjustmentOverlay(
                                    initialBounds = detectedBounds!!,
                                    onBoundsChanged = { newBounds ->
                                        detectedBounds = newBounds
                                    },
                                    onDone = {
                                        showManualOverlay = false
                                        manualMode = false
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            
                            // Capture hint
                            if (!manualMode) {
                                Card(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    )
                                ) {
                                    Text(
                                        text = if (detectedBounds != null) 
                                            "Document detected! Tap capture or enable manual mode to adjust corners" 
                                        else 
                                            "Position document on contrasting background",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Show captured image with processing controls
                        CapturedImagePreview(
                            imagePath = capturedImagePath!!,
                            detectedBounds = detectedBounds,
                            isProcessing = isProcessing,
                            onRetake = {
                                isCaptured = false
                                capturedImagePath = null
                                detectedBounds = null
                                showManualOverlay = false
                            },
                            onProcess = { bounds ->
                                isProcessing = true
                                processDocument(
                                    context = context,
                                    imagePath = capturedImagePath!!,
                                    bounds = bounds,
                                    onSuccess = { processedPath ->
                                        isProcessing = false
                                        onNavigateToReview(processedPath)
                                    },
                                    onError = {
                                        isProcessing = false
                                        // Handle error
                                    }
                                )
                            },
                            onManualAdjust = {
                                showManualOverlay = true
                            }
                        )
                    }
                } else {
                    // Permission request UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera permission is required to scan documents",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentDetectionOverlay(
    bounds: DocumentDetector.DocumentBounds?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        bounds?.let { docBounds ->
            val path = Path().apply {
                moveTo(docBounds.topLeft.x, docBounds.topLeft.y)
                lineTo(docBounds.topRight.x, docBounds.topRight.y)
                lineTo(docBounds.bottomRight.x, docBounds.bottomRight.y)
                lineTo(docBounds.bottomLeft.x, docBounds.bottomLeft.y)
                close()
            }
            
            drawPath(
                path = path,
                color = Color.Green,
                style = Stroke(width = 8.dp.toPx())
            )
            
            // Draw corner points
            val radius = 12.dp.toPx()
            drawCircle(
                color = Color.Red,
                radius = radius,
                center = Offset(docBounds.topLeft.x, docBounds.topLeft.y)
            )
            drawCircle(
                color = Color.Red,
                radius = radius,
                center = Offset(docBounds.topRight.x, docBounds.topRight.y)
            )
            drawCircle(
                color = Color.Red,
                radius = radius,
                center = Offset(docBounds.bottomRight.x, docBounds.bottomRight.y)
            )
            drawCircle(
                color = Color.Red,
                radius = radius,
                center = Offset(docBounds.bottomLeft.x, docBounds.bottomLeft.y)
            )
        }
    }
}

@Composable
private fun ManualBorderAdjustmentOverlay(
    initialBounds: DocumentDetector.DocumentBounds,
    onBoundsChanged: (DocumentDetector.DocumentBounds) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bounds by remember { mutableStateOf(initialBounds) }
    var draggedCorner by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = modifier) {
        // Semi-transparent background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Determine which corner is being dragged
                            val threshold = 50.dp.toPx()
                            draggedCorner = when {
                                (offset - Offset(bounds.topLeft.x, bounds.topLeft.y)).getDistance() < threshold -> "topLeft"
                                (offset - Offset(bounds.topRight.x, bounds.topRight.y)).getDistance() < threshold -> "topRight"
                                (offset - Offset(bounds.bottomRight.x, bounds.bottomRight.y)).getDistance() < threshold -> "bottomRight"
                                (offset - Offset(bounds.bottomLeft.x, bounds.bottomLeft.y)).getDistance() < threshold -> "bottomLeft"
                                else -> null
                            }
                        },
                        onDrag = { _, dragAmount ->
                            draggedCorner?.let { corner ->
                                bounds = when (corner) {
                                    "topLeft" -> bounds.copy(
                                        topLeft = PointF(
                                            bounds.topLeft.x + dragAmount.x,
                                            bounds.topLeft.y + dragAmount.y
                                        )
                                    )
                                    "topRight" -> bounds.copy(
                                        topRight = PointF(
                                            bounds.topRight.x + dragAmount.x,
                                            bounds.topRight.y + dragAmount.y
                                        )
                                    )
                                    "bottomRight" -> bounds.copy(
                                        bottomRight = PointF(
                                            bounds.bottomRight.x + dragAmount.x,
                                            bounds.bottomRight.y + dragAmount.y
                                        )
                                    )
                                    "bottomLeft" -> bounds.copy(
                                        bottomLeft = PointF(
                                            bounds.bottomLeft.x + dragAmount.x,
                                            bounds.bottomLeft.y + dragAmount.y
                                        )
                                    )
                                    else -> bounds
                                }
                                onBoundsChanged(bounds)
                            }
                        },
                        onDragEnd = {
                            draggedCorner = null
                        }
                    )
                }
        ) {
            // Draw semi-transparent overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )
            
            // Draw document outline
            val path = Path().apply {
                moveTo(bounds.topLeft.x, bounds.topLeft.y)
                lineTo(bounds.topRight.x, bounds.topRight.y)
                lineTo(bounds.bottomRight.x, bounds.bottomRight.y)
                lineTo(bounds.bottomLeft.x, bounds.bottomLeft.y)
                close()
            }
            
            // Clear the document area
            drawPath(
                path = path,
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
            
            // Draw border
            drawPath(
                path = path,
                color = Color.Green,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Draw draggable corner points
            val cornerRadius = 20.dp.toPx()
            
            drawCircle(
                color = Color.Red,
                radius = cornerRadius,
                center = Offset(bounds.topLeft.x, bounds.topLeft.y)
            )
            drawCircle(
                color = Color.Red,
                radius = cornerRadius,
                center = Offset(bounds.topRight.x, bounds.topRight.y)
            )
            drawCircle(
                color = Color.Red,
                radius = cornerRadius,
                center = Offset(bounds.bottomRight.x, bounds.bottomRight.y)
            )
            drawCircle(
                color = Color.Red,
                radius = cornerRadius,
                center = Offset(bounds.bottomLeft.x, bounds.bottomLeft.y)
            )
        }
        
        // Control buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDone,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    onBoundsChanged(bounds)
                    onDone()
                }
            ) {
                Text("Done")
            }
        }
        
        // Instructions
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = "Drag the red circles to adjust document corners",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CapturedImagePreview(
    imagePath: String,
    detectedBounds: DocumentDetector.DocumentBounds?,
    isProcessing: Boolean,
    onRetake: () -> Unit,
    onProcess: (DocumentDetector.DocumentBounds?) -> Unit,
    onManualAdjust: () -> Unit
) {
    val bitmap = remember(imagePath) {
        BitmapFactory.decodeFile(imagePath)
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Captured image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Show detected bounds overlay
                detectedBounds?.let { bounds ->
                    DocumentDetectionOverlay(
                        bounds = bounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // Control buttons
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (detectedBounds != null) "Document corners detected" else "No document detected",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retake")
                    }
                    
                    if (detectedBounds != null) {
                        OutlinedButton(
                            onClick = onManualAdjust,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Adjust")
                        }
                    }
                    
                    Button(
                        onClick = { onProcess(detectedBounds) },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Process")
                        }
                    }
                }
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            
            preview.setSurfaceProvider(previewView.surfaceProvider)
            onImageCaptureReady(imageCapture)
            
        } catch (exc: Exception) {
            android.util.Log.e("CameraScreen", "Camera binding failed", exc)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun captureImage(
    imageCapture: ImageCapture?,
    context: Context,
    onImageCaptured: (String) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture ?: return
    
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())
    
    // Create output directory if it doesn't exist
    val imagesDir = File(context.getExternalFilesDir(null), "captured_images")
    if (!imagesDir.exists()) {
        imagesDir.mkdirs()
    }
    
    val outputFile = File(imagesDir, "capture_$name.jpg")
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                android.util.Log.e("CameraScreen", "Image capture failed", exception)
                onError("Failed to capture image: ${exception.message}")
            }
            
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                android.util.Log.d("CameraScreen", "Image captured: ${outputFile.absolutePath}")
                onImageCaptured(outputFile.absolutePath)
            }
        }
    )
}

private fun processDocument(
    context: Context,
    imagePath: String,
    bounds: DocumentDetector.DocumentBounds?,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    // Process the image in a background thread
    Thread {
        var bitmap: Bitmap? = null
        var processedBitmap: Bitmap? = null
        
        try {
            bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                android.util.Log.e("CameraScreen", "Failed to decode bitmap from: $imagePath")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError("Failed to load image")
                }
                return@Thread
            }
            
            val imageProcessor = ImageProcessor(context)
            
            // Apply perspective correction and enhance image
            processedBitmap = imageProcessor.processDocument(
                bitmap = bitmap,
                bounds = bounds,
                colorMode = ImageProcessor.ColorMode.COLOR,
                enhanceContrast = true
            )
            
            // Create output directory if it doesn't exist
            val documentsDir = File(context.getExternalFilesDir(null), "processed_documents")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            
            // Save processed image
            val outputFile = File(documentsDir, "processed_${System.currentTimeMillis()}.jpg")
            val success = imageProcessor.saveBitmap(processedBitmap, outputFile.absolutePath, 90)
            
            if (success) {
                android.util.Log.d("CameraScreen", "Document processed: ${outputFile.absolutePath}")
                // Switch to main thread for UI update
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onSuccess(outputFile.absolutePath)
                }
            } else {
                android.util.Log.e("CameraScreen", "Failed to save processed image")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError("Failed to save processed document")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("CameraScreen", "Error processing document", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onError("Error processing document: ${e.message}")
            }
        } finally {
            // Clean up bitmaps to prevent memory leaks
            processedBitmap?.takeIf { it != bitmap && !it.isRecycled }?.recycle()
            bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }.start()
} 