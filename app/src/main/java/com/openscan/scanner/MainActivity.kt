package com.openscan.scanner

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openscan.scanner.data.ScannedDocument
import com.openscan.scanner.scanner.DocumentScanner
import com.openscan.scanner.ui.AboutScreen
import com.openscan.scanner.ui.HomeScreen
import com.openscan.scanner.ui.HomeViewModel
import com.openscan.scanner.ui.OcrResultScreen
import com.openscan.scanner.ui.PageManagerScreen
import com.openscan.scanner.ui.ProgressDialog
import com.openscan.scanner.ui.openPdf
import com.openscan.scanner.ui.sharePdf
import com.openscan.scanner.ui.theme.OpenScanTheme

private sealed interface Screen {
    object Home : Screen
    object About : Screen
    data class Ocr(val title: String, val text: String) : Screen
    data class Pages(val docId: String) : Screen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpenScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: HomeViewModel = viewModel()
                    val state by viewModel.uiState.collectAsState()
                    val context = LocalContext.current
                    val activity = this@MainActivity

                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                    var pendingAppend by remember { mutableStateOf<ScannedDocument?>(null) }
                    var ocrRunning by remember { mutableStateOf(false) }
                    var busyMessage by remember { mutableStateOf<String?>(null) }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartIntentSenderForResult()
                    ) { activityResult ->
                        if (activityResult.resultCode == Activity.RESULT_OK) {
                            val result = DocumentScanner.resultFrom(activityResult.data)
                            val pages = result?.pages?.map { it.imageUri } ?: emptyList()
                            val target = pendingAppend
                            when {
                                pages.isEmpty() ->
                                    Toast.makeText(context, "Nothing was scanned.", Toast.LENGTH_SHORT).show()
                                target != null -> viewModel.appendScan(target, pages)
                                else -> viewModel.saveScan(pages)
                            }
                        }
                        pendingAppend = null
                    }

                    fun launchScanner(appendTo: ScannedDocument?) {
                        pendingAppend = appendTo
                        DocumentScanner.client()
                            .getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                            .addOnFailureListener { e ->
                                pendingAppend = null
                                Toast.makeText(
                                    context,
                                    "Scanner unavailable: ${e.localizedMessage ?: "Google Play Services required"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }

                    fun runOcr(doc: ScannedDocument) {
                        ocrRunning = true
                        viewModel.extractText(doc) { result ->
                            ocrRunning = false
                            result
                                .onSuccess { screen = Screen.Ocr(doc.name, it) }
                                .onFailure {
                                    Toast.makeText(context, "Couldn't extract text.", Toast.LENGTH_LONG).show()
                                }
                        }
                    }

                    when (val current = screen) {
                        Screen.Home -> HomeScreen(
                            state = state,
                            onScan = { launchScanner(null) },
                            onQueryChange = viewModel::setQuery,
                            onSortChange = viewModel::setSort,
                            onOpenAbout = { screen = Screen.About },
                            onOpen = { doc -> openPdf(context, viewModel.store.contentUri(doc.pdfFile)) },
                            onShare = { doc -> sharePdf(context, viewModel.store.contentUri(doc.pdfFile), doc.name) },
                            onExport = { doc, cb -> viewModel.exportToDownloads(doc, cb) },
                            onToggleView = { viewModel.toggleView() },
                            onAppend = { doc -> launchScanner(doc) },
                            onManagePages = { doc -> screen = Screen.Pages(doc.id) },
                            onHarmonize = { doc ->
                                busyMessage = "Harmonizing lighting…"
                                viewModel.harmonizeLighting(doc) { busyMessage = null }
                            },
                            onExtractText = { doc -> runOcr(doc) },
                            onRename = { doc, newName -> viewModel.rename(doc, newName) },
                            onDelete = { doc -> viewModel.delete(doc) },
                            onMerge = { docs ->
                                busyMessage = "Merging documents…"
                                viewModel.merge(docs) { busyMessage = null }
                            },
                            onDeleteMany = { docs -> viewModel.deleteMany(docs) }
                        )

                        Screen.About -> {
                            BackHandler { screen = Screen.Home }
                            AboutScreen(
                                storageBytes = state.storageBytes,
                                documentCount = state.totalDocs,
                                quality = viewModel.pdfQuality,
                                onQualityChange = { viewModel.pdfQuality = it },
                                onClearAll = { viewModel.deleteAll() },
                                onBack = { screen = Screen.Home }
                            )
                        }

                        is Screen.Pages -> {
                            BackHandler { screen = Screen.Home }
                            val doc = state.documents.firstOrNull { it.id == current.docId }
                            if (doc == null) {
                                screen = Screen.Home
                            } else {
                                PageManagerScreen(
                                    doc = doc,
                                    onMoveUp = { index -> viewModel.reorderPage(doc, index, index - 1) {} },
                                    onMoveDown = { index -> viewModel.reorderPage(doc, index, index + 1) {} },
                                    onDeletePage = { index ->
                                        viewModel.deletePage(doc, index) { updated ->
                                            if (updated == null) screen = Screen.Home
                                        }
                                    },
                                    onBack = { screen = Screen.Home }
                                )
                            }
                        }

                        is Screen.Ocr -> {
                            BackHandler { screen = Screen.Home }
                            OcrResultScreen(
                                title = current.title,
                                text = current.text,
                                onBack = { screen = Screen.Home }
                            )
                        }
                    }

                    if (ocrRunning) {
                        ProgressDialog(message = "Extracting text…")
                    }
                    busyMessage?.let { ProgressDialog(message = it) }
                }
            }
        }
    }
}
