package com.openscan.scanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openscan.scanner.data.repository.DocumentRepository
import com.openscan.scanner.ui.components.DocumentCard
import com.openscan.scanner.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDocumentReview: (String) -> Unit = { }
) {
    val context = LocalContext.current
    val documentRepository = remember { DocumentRepository(context) }
    var documents by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) {
        documents = documentRepository.getAllDocuments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OpenScan",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCamera,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Scan Document"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Recent Scans",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (documents.isEmpty()) {
                EmptyState(
                    title = "No documents yet",
                    subtitle = "Start scanning by tapping the + button",
                    onActionClick = onNavigateToCamera,
                    actionText = "Start Scanning"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documents) { document ->
                        DocumentCard(
                            documentPath = document,
                            onDocumentClick = { documentPath ->
                                onNavigateToDocumentReview(documentPath)
                            },
                            onDocumentShare = { documentPath ->
                                documentRepository.shareDocument(documentPath)?.let { intent ->
                                    context.startActivity(intent)
                                }
                            },
                            onDocumentDelete = { documentPath ->
                                if (documentRepository.deleteDocument(documentPath)) {
                                    documents = documents.filter { it != documentPath }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
} 