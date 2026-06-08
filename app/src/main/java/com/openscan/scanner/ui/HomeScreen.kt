package com.openscan.scanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
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
import com.openscan.scanner.data.LibraryView
import com.openscan.scanner.data.ScannedDocument
import com.openscan.scanner.data.SortOrder
import com.openscan.scanner.ui.components.DocumentGridCell
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
    onToggleView: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpen: (ScannedDocument) -> Unit,
    onShare: (ScannedDocument) -> Unit,
    onExport: (ScannedDocument, (String?) -> Unit) -> Unit,
    onAppend: (ScannedDocument) -> Unit,
    onManagePages: (ScannedDocument) -> Unit,
    onHarmonize: (ScannedDocument) -> Unit,
    onExtractText: (ScannedDocument) -> Unit,
    onRename: (ScannedDocument, String) -> Unit,
    onDelete: (ScannedDocument) -> Unit,
    onMerge: (List<ScannedDocument>) -> Unit,
    onDeleteMany: (List<ScannedDocument>) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var renameTarget by remember { mutableStateOf<ScannedDocument?>(null) }
    var deleteTarget by remember { mutableStateOf<ScannedDocument?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var batchDeleteConfirm by remember { mutableStateOf(false) }

    val selectionMode = selectedIds.isNotEmpty()
    val selectedDocs = state.documents.filter { it.id in selectedIds }

    fun clearSelection() { selectedIds = emptySet() }
    fun toggleSelect(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    BackHandler(enabled = selectionMode) { clearSelection() }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionTopBar(
                    count = selectedIds.size,
                    canMerge = selectedIds.size >= 2,
                    onClose = { clearSelection() },
                    onMerge = { onMerge(selectedDocs); clearSelection() },
                    onDelete = { batchDeleteConfirm = true }
                )
            } else {
                TopAppBar(
                    title = { Text("OpenScan") },
                    actions = {
                        IconButton(onClick = onToggleView) {
                            if (state.viewMode == LibraryView.LIST) {
                                Icon(Icons.Default.GridView, contentDescription = "Grid view")
                            } else {
                                Icon(Icons.Default.ViewList, contentDescription = "List view")
                            }
                        }
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
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                ExtendedFloatingActionButton(
                    text = { Text("Scan") },
                    icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) },
                    onClick = onScan
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.totalDocs > 0 && !selectionMode) {
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
                    placeholder = { Text("Search name or text inside") },
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
                    state.viewMode == LibraryView.GRID -> DocumentGrid(
                        state = state,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onItemClick = { doc -> if (selectionMode) toggleSelect(doc.id) else onOpen(doc) },
                        onItemLong = { doc -> toggleSelect(doc.id) },
                        onShare = onShare,
                        onExport = { doc -> exportWithSnackbar(doc, onExport, scope, snackbarHostState) },
                        onAppend = onAppend,
                        onManagePages = onManagePages,
                        onHarmonize = onHarmonize,
                        onExtractText = onExtractText,
                        onRename = { renameTarget = it },
                        onDelete = { deleteTarget = it }
                    )
                    else -> DocumentList(
                        state = state,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onItemClick = { doc -> if (selectionMode) toggleSelect(doc.id) else onOpen(doc) },
                        onItemLong = { doc -> toggleSelect(doc.id) },
                        onShare = onShare,
                        onExport = { doc -> exportWithSnackbar(doc, onExport, scope, snackbarHostState) },
                        onAppend = onAppend,
                        onManagePages = onManagePages,
                        onHarmonize = onHarmonize,
                        onExtractText = onExtractText,
                        onRename = { renameTarget = it },
                        onDelete = { deleteTarget = it }
                    )
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

    if (batchDeleteConfirm) {
        DeleteDialog(
            name = "${selectedIds.size} documents",
            onConfirm = {
                onDeleteMany(selectedDocs)
                batchDeleteConfirm = false
                clearSelection()
            },
            onDismiss = { batchDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    canMerge: Boolean,
    onClose: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text("$count selected") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cancel selection")
            }
        },
        actions = {
            IconButton(onClick = onMerge, enabled = canMerge) {
                Icon(Icons.Default.CallMerge, contentDescription = "Merge into one document")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected")
            }
        }
    )
}

@Composable
private fun DocumentList(
    state: HomeUiState,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onItemClick: (ScannedDocument) -> Unit,
    onItemLong: (ScannedDocument) -> Unit,
    onShare: (ScannedDocument) -> Unit,
    onExport: (ScannedDocument) -> Unit,
    onAppend: (ScannedDocument) -> Unit,
    onManagePages: (ScannedDocument) -> Unit,
    onHarmonize: (ScannedDocument) -> Unit,
    onExtractText: (ScannedDocument) -> Unit,
    onRename: (ScannedDocument) -> Unit,
    onDelete: (ScannedDocument) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.documents, key = { it.id }) { doc ->
            DocumentRow(
                doc = doc,
                selectionMode = selectionMode,
                selected = doc.id in selectedIds,
                onClick = { onItemClick(doc) },
                onLongClick = { onItemLong(doc) },
                onShare = { onShare(doc) },
                onExport = { onExport(doc) },
                onAppend = { onAppend(doc) },
                onManagePages = { onManagePages(doc) },
                onHarmonize = { onHarmonize(doc) },
                onExtractText = { onExtractText(doc) },
                onRename = { onRename(doc) },
                onDelete = { onDelete(doc) }
            )
        }
    }
}

@Composable
private fun DocumentGrid(
    state: HomeUiState,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onItemClick: (ScannedDocument) -> Unit,
    onItemLong: (ScannedDocument) -> Unit,
    onShare: (ScannedDocument) -> Unit,
    onExport: (ScannedDocument) -> Unit,
    onAppend: (ScannedDocument) -> Unit,
    onManagePages: (ScannedDocument) -> Unit,
    onHarmonize: (ScannedDocument) -> Unit,
    onExtractText: (ScannedDocument) -> Unit,
    onRename: (ScannedDocument) -> Unit,
    onDelete: (ScannedDocument) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(state.documents, key = { it.id }) { doc ->
            DocumentGridCell(
                doc = doc,
                selectionMode = selectionMode,
                selected = doc.id in selectedIds,
                onClick = { onItemClick(doc) },
                onLongClick = { onItemLong(doc) },
                onShare = { onShare(doc) },
                onExport = { onExport(doc) },
                onAppend = { onAppend(doc) },
                onManagePages = { onManagePages(doc) },
                onHarmonize = { onHarmonize(doc) },
                onExtractText = { onExtractText(doc) },
                onRename = { onRename(doc) },
                onDelete = { onDelete(doc) }
            )
        }
    }
}

private fun exportWithSnackbar(
    doc: ScannedDocument,
    onExport: (ScannedDocument, (String?) -> Unit) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    onExport(doc) { savedName ->
        scope.launch {
            snackbarHostState.showSnackbar(
                if (savedName != null) "Saved \"$savedName\" to Downloads"
                else "Couldn't save to Downloads"
            )
        }
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
