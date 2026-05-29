package com.openscan.scanner.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openscan.scanner.data.DocumentStore
import com.openscan.scanner.data.ScannedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val loading: Boolean = true,
    val documents: List<ScannedDocument> = emptyList()
) {
    val isEmpty: Boolean get() = !loading && documents.isEmpty()
}

/**
 * Owns the saved-document list and all storage side effects so the UI stays
 * declarative. Storage I/O is moved off the main thread.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    val store = DocumentStore(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val docs = withContext(Dispatchers.IO) { store.list() }
            _uiState.value = HomeUiState(loading = false, documents = docs)
        }
    }

    fun saveScan(pdfUri: Uri, firstPageUri: Uri?, pageCount: Int, onSaved: (ScannedDocument) -> Unit) {
        viewModelScope.launch {
            val doc = withContext(Dispatchers.IO) {
                store.save(pdfUri, firstPageUri, pageCount)
            }
            refresh()
            onSaved(doc)
        }
    }

    fun rename(doc: ScannedDocument, newName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.rename(doc, newName) }
            refresh()
        }
    }

    fun delete(doc: ScannedDocument) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.delete(doc) }
            refresh()
        }
    }

    fun exportToDownloads(doc: ScannedDocument, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { store.exportToDownloads(doc) }
            onResult(name)
        }
    }
}
