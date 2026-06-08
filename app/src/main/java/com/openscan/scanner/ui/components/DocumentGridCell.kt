package com.openscan.scanner.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openscan.scanner.data.ScannedDocument
import com.openscan.scanner.ui.formatPageCount

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentGridCell(
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)) {
            DocumentThumbnail(doc, Modifier.fillMaxWidth().aspectRatio(0.75f))

            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RectangleShape)
                )
            }

            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    if (selected) {
                        OrangeDot(size = 16)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menuOpen = true }) {
                        Text(
                            "···",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
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

        Spacer(Modifier.height(8.dp))
        Text(
            text = doc.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatPageCount(doc.pageCount).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
