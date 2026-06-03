package com.openscan.scanner.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openscan.scanner.data.DocumentStore
import com.openscan.scanner.data.ScannedDocument
import com.openscan.scanner.data.SortOrder
import com.openscan.scanner.data.TextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val loading: Boolean = true,
    val documents: List<ScannedDocument> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val totalDocs: Int = 0,
    val storageBytes: Long = 0L
) {
    val isEmpty: Boolean get() = !loading && totalDocs == 0
    val noResults: Boolean get() = !loading && totalDocs > 0 && documents.isEmpty()
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    val store = DocumentStore(app)

    private var allDocs: List<ScannedDocument> = emptyList()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val order = _uiState.value.sortOrder
            val docs = withContext(Dispatchers.IO) { store.list(order) }
            val bytes = withContext(Dispatchers.IO) { store.totalBytes() }
            allDocs = docs
            _uiState.update { it.copy(loading = false, totalDocs = docs.size, storageBytes = bytes) }
            applyFilter()
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        applyFilter()
    }

    fun setSort(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        refresh()
    }

    /** Recompute the displayed list from [allDocs] using the current query. */
    private fun applyFilter() {
        val query = _uiState.value.query.trim()
        val filtered = if (query.isEmpty()) {
            allDocs
        } else {
            allDocs.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.update { it.copy(documents = filtered) }
    }

    fun saveScan(pageUris: List<Uri>, onSaved: (ScannedDocument) -> Unit = {}) {
        if (pageUris.isEmpty()) return
        viewModelScope.launch {
            val doc = withContext(Dispatchers.IO) { store.save(pageUris) }
            refresh()
            onSaved(doc)
        }
    }

    fun appendScan(doc: ScannedDocument, pageUris: List<Uri>, onDone: (ScannedDocument) -> Unit = {}) {
        if (pageUris.isEmpty()) return
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) { store.append(doc, pageUris) }
            refresh()
            onDone(updated)
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

    fun deleteAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { store.deleteAll() }
            refresh()
        }
    }

    fun exportToDownloads(doc: ScannedDocument, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { store.exportToDownloads(doc) }
            onResult(name)
        }
    }

    fun extractText(doc: ScannedDocument, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                TextExtractor.extract(getApplication(), doc.pageImages)
            }
            onResult(result)
        }
    }
}
