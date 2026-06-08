package com.openscan.scanner.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openscan.scanner.BuildConfig
import com.openscan.scanner.data.PdfQuality
import com.openscan.scanner.ui.components.Eyebrow
import com.openscan.scanner.ui.components.Hairline
import com.openscan.scanner.ui.components.InkRule
import com.openscan.scanner.ui.components.OrangeDot
import com.openscan.scanner.ui.components.editorialBarColors

private const val CREATOR_NAME = "Yuri Ihnatov"
private const val CREATOR_URL = "https://ihnatov.nl"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    storageBytes: Long,
    documentCount: Int,
    quality: PdfQuality,
    onQualityChange: (PdfQuality) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf(quality) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = editorialBarColors(),
                title = { Text("ABOUT", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            InkRule()

            // Masthead block
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "OpenScan",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(4.dp))
                    OrangeDot(size = 8)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "VERSION ${BuildConfig.VERSION_NAME}".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "A document scanner powered by Google ML Kit. Capture, auto-crop, " +
                        "fix the perspective, extract text, and save tidy PDFs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Hairline()
            Section(index = "01", label = "Creator") {
                Text(CREATOR_NAME, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text(
                    CREATOR_URL,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CREATOR_URL)))
                    }
                )
            }

            Hairline()
            Section(index = "02", label = "PDF Quality") {
                Text(
                    "Applies to new scans and page edits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                PdfQuality.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selectedQuality,
                                onClick = { selectedQuality = option; onQualityChange(option) }
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Choice(option == selectedQuality)
                        Spacer(Modifier.width(12.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Hairline()
            Section(index = "03", label = "Storage") {
                Text(
                    "$documentCount DOCUMENT${if (documentCount == 1) "" else "S"} · ${formatSize(storageBytes)}".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { confirmClear = true },
                    enabled = documentCount > 0
                ) {
                    Text("CLEAR ALL DOCUMENTS", style = MaterialTheme.typography.labelLarge)
                }
            }

            Hairline()
            Text(
                "Scans are stored only on this device. The camera is operated by " +
                    "Google Play Services, so OpenScan requests no camera permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        }
    }

    if (confirmClear) {
        DeleteDialog(
            name = "all documents",
            onConfirm = { confirmClear = false; onClearAll() },
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun Section(index: String, label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Eyebrow("$index / $label")
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun Choice(selected: Boolean) {
    if (selected) {
        OrangeDot(size = 14)
    } else {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}
