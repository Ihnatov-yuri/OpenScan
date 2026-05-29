package com.openscan.scanner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openscan.scanner.data.ScannedDocument
import com.openscan.scanner.ui.components.DocumentRow
import com.openscan.scanner.ui.components.EmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onScan: () -> Unit,
    onOpen: (ScannedDocument) -> Unit,
    onShare: (ScannedDocument) -> Unit,
    onExport: (ScannedDocument, (String?) -> Unit) -> Unit,
    onRename: (ScannedDocument, String) -> Unit,
    onDelete: (ScannedDocument) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var renameTarget by remember { mutableStateOf<ScannedDocument?>(null) }
    var deleteTarget by remember { mutableStateOf<ScannedDocument?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenScan") },
                colors = TopAppBarDefaults.topAppBarColors()
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.isEmpty -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
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
            onConfirm = { newName ->
                onRename(doc, newName)
                renameTarget = null
            },
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
