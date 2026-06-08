package com.openscan.scanner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.openscan.scanner.ui.formatPageCount
import com.openscan.scanner.ui.formatSize
import com.openscan.scanner.ui.formatTimestamp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentRow(
    doc: ScannedDocument,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onAppend: () -> Unit,
    onManagePages: () -> Unit,
    onHarmonize: () -> Unit,
    onExtractText: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentThumbnail(doc, Modifier.size(width = 52.dp, height = 68.dp))

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatPageCount(doc.pageCount) + " • " + formatSize(doc.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(doc.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectionMode) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                    }
                    DocumentActionsMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        onShare = onShare,
                        onExport = onExport,
                        onAppend = onAppend,
                        onManagePages = onManagePages,
                        onHarmonize = onHarmonize,
                        onExtractText = onExtractText,
                        onRename = onRename,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentThumbnail(doc: ScannedDocument, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val thumb = doc.thumbnailFile
        if (thumb != null) {
            val context = LocalContext.current
            AsyncImage(
                // Page files can be rewritten in place (reorder/delete/harmonize),
                // so key the cache on lastModified to avoid stale thumbnails.
                model = ImageRequest.Builder(context)
                    .data(thumb)
                    .memoryCacheKey(thumb.path + thumb.lastModified())
                    .diskCacheKey(thumb.path + thumb.lastModified())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DocumentActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onAppend: () -> Unit,
    onManagePages: () -> Unit,
    onHarmonize: () -> Unit,
    onExtractText: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Default.Share, null) },
            onClick = { onDismiss(); onShare() }
        )
        DropdownMenuItem(
            text = { Text("Save to Downloads") },
            leadingIcon = { Icon(Icons.Default.Download, null) },
            onClick = { onDismiss(); onExport() }
        )
        DropdownMenuItem(
            text = { Text("Add pages") },
            leadingIcon = { Icon(Icons.Default.PostAdd, null) },
            onClick = { onDismiss(); onAppend() }
        )
        DropdownMenuItem(
            text = { Text("Manage pages") },
            leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
            onClick = { onDismiss(); onManagePages() }
        )
        DropdownMenuItem(
            text = { Text("Harmonize lighting") },
            leadingIcon = { Icon(Icons.Default.AutoFixHigh, null) },
            onClick = { onDismiss(); onHarmonize() }
        )
        DropdownMenuItem(
            text = { Text("Extract text") },
            leadingIcon = { Icon(Icons.Default.TextFields, null) },
            onClick = { onDismiss(); onExtractText() }
        )
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            onClick = { onDismiss(); onRename() }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}
