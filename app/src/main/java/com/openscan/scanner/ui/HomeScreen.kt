package com.openscan.scanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openscan.scanner.data.ScannedDocument
import com.openscan.scanner.data.SortOrder
import com.openscan.scanner.ui.components.DocumentRow
import com.openscan.scanner.ui.components.EmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onScan: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onOpenAbout: () -> Unit,
    onOpen: (ScannedDocument) -> Unit,
    onShare: (ScannedDocument) -> Unit,
    onExport: (ScannedDocument, (String?) -> Unit) -> Unit,
    onAppend: (ScannedDocument) -> Unit,
    onExtractText: (ScannedDocument) -> Unit,
    onRename: (ScannedDocument, String) -> Unit,
    onDelete: (ScannedDocument) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var renameTarget by remember { mutableStateOf<ScannedDocument?>(null) }
    var deleteTarget by remember { mutableStateOf<ScannedDocument?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenScan") },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    trailingIcon = {
                                        if (order == state.sortOrder) Icon(Icons.Default.Check, null)
                                    },
                                    onClick = { sortMenuOpen = false; onSortChange(order) }
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(Icons.Default.Info, contentDescription = "About")
                        }
                        DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("About OpenScan") },
                                onClick = { overflowOpen = false; onOpenAbout() }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Scan") },
                icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) },
                onClick = onScan
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.totalDocs > 0) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    placeholder = { Text("Search documents") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.isEmpty -> EmptyState()
                    state.noResults -> NoResults(state.query)
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.documents, key = { it.id }) { doc ->
                            DocumentRow(
                                doc = doc,
                                onOpen = { onOpen(doc) },
                                onShare = { onShare(doc) },
                                onExport = {
                                    onExport(doc) { savedName ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (savedName != null) "Saved \"$savedName\" to Downloads"
                                                else "Couldn't save to Downloads"
                                            )
                                        }
                                    }
                                },
                                onAppend = { onAppend(doc) },
                                onExtractText = { onExtractText(doc) },
                                onRename = { renameTarget = doc },
                                onDelete = { deleteTarget = doc }
                            )
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { doc ->
        RenameDialog(
            currentName = doc.name,
            onConfirm = { newName -> onRename(doc, newName); renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { doc ->
        DeleteDialog(
            name = doc.name,
            onConfirm = {
                onDelete(doc)
                deleteTarget = null
                scope.launch { snackbarHostState.showSnackbar("Deleted \"${doc.name}\"") }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun NoResults(query: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "No documents match \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
