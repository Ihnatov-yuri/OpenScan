package com.openscan.scanner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openscan.scanner.data.DataExtractor
import com.openscan.scanner.data.DataType
import com.openscan.scanner.ui.components.Eyebrow
import com.openscan.scanner.ui.components.Hairline
import com.openscan.scanner.ui.components.InkRule
import com.openscan.scanner.ui.components.editorialBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OcrResultScreen(
    title: String,
    text: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val content = text.ifBlank { "No text was detected in this document." }
    val detected = remember(text) { DataExtractor.grouped(text) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = editorialBarColors(),
                title = { Text(text = "TEXT · ${title.uppercase()}", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { copyToClipboard(context, content) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = { shareText(context, title, content) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            InkRule()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (detected.isNotEmpty()) {
                    Eyebrow("Detected")
                    Spacer(Modifier.height(12.dp))
                    detected.forEach { (type, values) ->
                        Text(
                            type.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            values.forEach { value ->
                                AssistChip(
                                    onClick = { actionFor(context, type, value) },
                                    label = { Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Hairline(modifier = Modifier.padding(vertical = 8.dp))
                }

                Eyebrow("Full text")
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(text = content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Tapping a detected chip does the obvious thing; falls back to copying. */
private fun actionFor(context: Context, type: DataType, value: String) {
    val intent = when (type) {
        DataType.EMAIL -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$value"))
        DataType.PHONE -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${value.filter { it.isDigit() || it == '+' }}"))
        DataType.LINK -> {
            val url = if (value.startsWith("http", true)) value else "https://$value"
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        else -> null
    }
    if (intent != null && runCatching { context.startActivity(intent) }.isSuccess) return
    copyToClipboard(context, value)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("OpenScan", text))
    // Android 13+ shows its own copy confirmation, so avoid a duplicate toast.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }
}

private fun shareText(context: Context, title: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share text"))
}
