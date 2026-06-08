package com.openscan.scanner.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.openscan.scanner.data.ScannedDocument
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerScreen(
    doc: ScannedDocument,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onAnnotate: (Int) -> Unit,
    onBack: () -> Unit
) {
    var deleteIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(doc.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            formatPageCount(doc.pageCount),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(doc.pageImages) { index, file ->
                PageRow(
                    index = index,
                    file = file,
                    isFirst = index == 0,
                    isLast = index == doc.pageImages.lastIndex,
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) },
                    onAnnotate = { onAnnotate(index) },
                    onDelete = { deleteIndex = index }
                )
            }
        }
    }

    deleteIndex?.let { idx ->
        DeleteDialog(
            name = "page ${idx + 1}",
            onConfirm = { onDeletePage(idx); deleteIndex = null },
            onDismiss = { deleteIndex = null }
        )
    }
}

@Composable
private fun PageRow(
    index: Int,
    file: File,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAnnotate: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 74.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    // Page files are renamed on reorder, so include lastModified in the
                    // cache key to avoid showing a stale image at a reused path.
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .memoryCacheKey(file.path + file.lastModified())
                        .diskCacheKey(file.path + file.lastModified())
                        .build(),
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(14.dp))
            Text(
                "Page ${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
            }
            IconButton(onClick = onAnnotate) {
                Icon(Icons.Default.Edit, contentDescription = "Annotate page")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete page",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
