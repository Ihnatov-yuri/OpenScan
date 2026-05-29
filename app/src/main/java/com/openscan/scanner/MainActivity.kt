package com.openscan.scanner

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openscan.scanner.scanner.DocumentScanner
import com.openscan.scanner.ui.HomeScreen
import com.openscan.scanner.ui.HomeViewModel
import com.openscan.scanner.ui.openPdf
import com.openscan.scanner.ui.sharePdf
import com.openscan.scanner.ui.theme.OpenScanTheme

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

                    // Receives the ML Kit scanner result and persists it.
                    val scannerLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartIntentSenderForResult()
                    ) { activityResult ->
                        if (activityResult.resultCode == Activity.RESULT_OK) {
                            val result = DocumentScanner.resultFrom(activityResult.data)
                            val pdf = result?.pdf
                            if (pdf != null) {
                                val firstPage = result.pages?.firstOrNull()?.imageUri
                                viewModel.saveScan(
                                    pdfUri = pdf.uri,
                                    firstPageUri = firstPage,
                                    pageCount = pdf.pageCount
                                ) { /* list auto-refreshes */ }
                            } else {
                                Toast.makeText(context, "Nothing was scanned.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    fun launchScanner() {
                        DocumentScanner.client()
                            .getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Scanner unavailable: ${e.localizedMessage ?: "Google Play Services required"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }

                    HomeScreen(
                        state = state,
                        onScan = { launchScanner() },
                        onOpen = { doc -> openPdf(context, viewModel.store.contentUri(doc.pdfFile)) },
                        onShare = { doc -> sharePdf(context, viewModel.store.contentUri(doc.pdfFile), doc.name) },
                        onExport = { doc, cb -> viewModel.exportToDownloads(doc, cb) },
                        onRename = { doc, newName -> viewModel.rename(doc, newName) },
                        onDelete = { doc -> viewModel.delete(doc) }
                    )
                }
            }
        }
    }
}
