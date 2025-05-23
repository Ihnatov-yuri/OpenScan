package com.openscan.scanner.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openscan.scanner.utils.ImageProcessor
import com.openscan.scanner.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentReviewScreen(
    documentPath: String,
    projectId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageProcessor = remember { ImageProcessor(context) }
    val pdfGenerator = remember { PdfGenerator(context) }
    val projectRepository = remember { com.openscan.scanner.data.repository.ProjectRepository(context) }
    
    var selectedColorMode by remember { mutableStateOf(ImageProcessor.ColorMode.COLOR) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var processedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var currentProject by remember { mutableStateOf(projectId?.let { projectRepository.getProject(it) }) }
    var showProjectDialog by remember { mutableStateOf(false) }
    
    // Load and process the image
    LaunchedEffect(documentPath, selectedColorMode) {
        try {
            val originalBitmap = BitmapFactory.decodeFile(documentPath)
            if (originalBitmap != null) {
                val newProcessedBitmap = imageProcessor.processDocument(
                    bitmap = originalBitmap,
                    bounds = null, // Already processed in camera
                    colorMode = selectedColorMode,
                    enhanceContrast = true
                )
                
                // Recycle old bitmap if it exists
                processedBitmap?.takeIf { !it.isRecycled }?.recycle()
                processedBitmap = newProcessedBitmap
                
                // Recycle original bitmap if different from processed
                if (originalBitmap != newProcessedBitmap && !originalBitmap.isRecycled) {
                    originalBitmap.recycle()
                }
            } else {
                android.util.Log.e("DocumentReview", "Failed to load bitmap from: $documentPath")
                errorMessage = "Failed to load image from: $documentPath"
                showErrorDialog = true
            }
        } catch (e: Exception) {
            android.util.Log.e("DocumentReview", "Error processing image", e)
            errorMessage = "Error processing image: ${e.message}"
            showErrorDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Document") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSaveDialog = true },
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Document preview
            processedBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Document preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // Color mode selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Color Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorModeButton(
                            label = "Color",
                            icon = Icons.Default.Palette,
                            isSelected = selectedColorMode == ImageProcessor.ColorMode.COLOR,
                            onClick = { selectedColorMode = ImageProcessor.ColorMode.COLOR }
                        )
                        
                        ColorModeButton(
                            label = "Grayscale",
                            icon = Icons.Default.FilterBAndW,
                            isSelected = selectedColorMode == ImageProcessor.ColorMode.GRAYSCALE,
                            onClick = { selectedColorMode = ImageProcessor.ColorMode.GRAYSCALE }
                        )
                        
                        ColorModeButton(
                            label = "B&W",
                            icon = Icons.Default.Contrast,
                            isSelected = selectedColorMode == ImageProcessor.ColorMode.BLACK_AND_WHITE,
                            onClick = { selectedColorMode = ImageProcessor.ColorMode.BLACK_AND_WHITE }
                        )
                    }
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentProject == null) {
                            // Create new project first
                            showProjectDialog = true
                        } else {
                            // Add current image to existing project and go to camera
                            scope.launch {
                                try {
                                    val bitmap = processedBitmap
                                    if (bitmap != null && !bitmap.isRecycled) {
                                        // Save current processed image
                                        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
                                        val saveSuccess = imageProcessor.saveBitmap(bitmap, tempFile.absolutePath, 90)
                                        
                                        if (saveSuccess) {
                                            projectRepository.addPageToProject(currentProject!!.id, tempFile.absolutePath)
                                            onNavigateToCamera()
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Failed to add page: ${e.message}"
                                    showErrorDialog = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentProject == null) "New Project" else "Add Page")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = { showSaveDialog = true },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save PDF")
                    }
                }
            }
        }
    }
    
    // Save dialog
    if (showSaveDialog) {
        SaveDocumentDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { quality ->
                scope.launch {
                    isProcessing = true
                    
                    try {
                        val bitmap = processedBitmap
                        if (bitmap == null || bitmap.isRecycled) {
                            android.util.Log.e("DocumentReview", "No valid bitmap to save")
                            isProcessing = false
                            showSaveDialog = false
                            return@launch
                        }
                        
                        // Save processed image first
                        val tempFile = File(context.cacheDir, "temp_processed_${System.currentTimeMillis()}.jpg")
                        val saveSuccess = imageProcessor.saveBitmap(bitmap, tempFile.absolutePath, quality)
                        
                        if (!saveSuccess) {
                            android.util.Log.e("DocumentReview", "Failed to save temporary image")
                            isProcessing = false
                            showSaveDialog = false
                            return@launch
                        }
                        
                        // Create PDF
                        val pdfSettings = PdfGenerator.PdfSettings(quality = quality)
                        val result = if (currentProject != null && currentProject!!.imagePaths.isNotEmpty()) {
                            // Create multi-page PDF from project
                            val allImages = currentProject!!.imagePaths + tempFile.absolutePath
                            pdfGenerator.createPdf(allImages, settings = pdfSettings)
                        } else {
                            // Create single-page PDF
                            pdfGenerator.createSinglePagePdf(
                                imagePath = tempFile.absolutePath,
                                settings = pdfSettings
                            )
                        }
                        
                        if (result.isSuccess) {
                            val pdfPath = result.getOrNull()!!
                            val fileName = File(pdfPath).name
                            android.util.Log.d("DocumentReview", "PDF created successfully: $pdfPath")
                            
                            // Show success message on main thread
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                isProcessing = false
                                showSaveDialog = false
                                val locationMessage = if (pdfPath.startsWith("Downloads/OpenScan/")) {
                                    "Location: $pdfPath"
                                } else {
                                    "Location: Downloads/OpenScan/$fileName"
                                }
                                successMessage = "PDF saved successfully!\n\nFile: $fileName\n$locationMessage"
                                showSuccessDialog = true
                            }
                        } else {
                            android.util.Log.e("DocumentReview", "Failed to create PDF", result.exceptionOrNull())
                            
                            // Show error on main thread
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                isProcessing = false
                                showSaveDialog = false
                                errorMessage = "Failed to create PDF: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                showErrorDialog = true
                            }
                        }
                        
                        // Clean up temp file
                        tempFile.delete()
                        
                    } catch (e: Exception) {
                        android.util.Log.e("DocumentReview", "Error during PDF creation", e)
                        isProcessing = false
                        showSaveDialog = false
                    }
                }
            }
        )
    }
    
    // Error dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                onNavigateToHome()
            },
            title = { Text("Success") },
            text = { Text(successMessage) },
            confirmButton = {
                Button(onClick = { 
                    showSuccessDialog = false
                    onNavigateToHome()
                }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Project creation dialog
    if (showProjectDialog) {
        var projectName by remember { mutableStateOf(projectRepository.generateProjectName()) }
        
        AlertDialog(
            onDismissRequest = { showProjectDialog = false },
            title = { Text("Create New Project") },
            text = {
                Column {
                    Text("Create a project to combine multiple scans into one PDF:")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val project = projectRepository.createProject(projectName)
                        currentProject = project
                        showProjectDialog = false
                        
                        // Add current image to project and go to camera
                        scope.launch {
                            try {
                                val bitmap = processedBitmap
                                if (bitmap != null && !bitmap.isRecycled) {
                                    val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
                                    val saveSuccess = imageProcessor.saveBitmap(bitmap, tempFile.absolutePath, 90)
                                    
                                    if (saveSuccess) {
                                        projectRepository.addPageToProject(project.id, tempFile.absolutePath)
                                        onNavigateToCamera()
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = "Failed to create project: ${e.message}"
                                showErrorDialog = true
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilterChip(
            onClick = onClick,
            label = { Text(label) },
            selected = isSelected,
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun SaveDocumentDialog(
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var quality by remember { mutableStateOf(90) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Document") },
        text = {
            Column {
                Text("Choose PDF quality:")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Quality: ${quality}%")
                Slider(
                    value = quality.toFloat(),
                    onValueChange = { quality = it.toInt() },
                    valueRange = 10f..100f,
                    steps = 8
                )
                
                Text(
                    text = when {
                        quality >= 90 -> "High quality, larger file size"
                        quality >= 70 -> "Good quality, medium file size"
                        else -> "Lower quality, smaller file size"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(quality) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 