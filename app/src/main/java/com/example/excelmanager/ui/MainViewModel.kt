package com.example.excelmanager.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.excelmanager.data.db.FileEntity
import com.example.excelmanager.data.db.SheetEntity
import com.example.excelmanager.data.repo.ExcelRepository
import com.example.excelmanager.data.repo.SelectionStore
import com.example.excelmanager.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.OutputStream
import javax.inject.Inject

sealed interface ImportState {
    data object Idle : ImportState
    data class Running(val progress: Float) : ImportState
}

data class QueryState(val searchCol: Int? = null, val filters: List<Filter> = emptyList(), val sort: Sort? = null)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: ExcelRepository,
    private val sel: SelectionStore
) : ViewModel() {

    private val share = SharingStarted.WhileSubscribed(5_000)

    // ---- selection ----
    val files: StateFlow<List<FileEntity>> = repo.files().stateIn(viewModelScope, share, emptyList())
    val selectedFileId = sel.fileId
    val autoSync = sel.autoSync

    val sheets: StateFlow<List<SheetEntity>> = sel.fileId
        .flatMapLatest { if (it == 0L) flowOf(emptyList()) else repo.sheets(it) }
        .stateIn(viewModelScope, share, emptyList())

    /** The saved sheet if it still belongs to the file, otherwise the first sheet. */
    val selectedSheetId: StateFlow<Long> = combine(sel.sheetId, sheets) { id, list ->
        if (list.any { it.id == id }) id else list.firstOrNull()?.id ?: 0L
    }.stateIn(viewModelScope, share, 0L)

    val columns: StateFlow<List<Col>> = selectedSheetId
        .flatMapLatest { if (it == 0L) flowOf(emptyList()) else repo.columns(it) }
        .stateIn(viewModelScope, share, emptyList())

    val rows: StateFlow<List<RowItem>> = selectedSheetId
        .flatMapLatest { if (it == 0L) flowOf(emptyList()) else repo.rows(it) }
        .stateIn(viewModelScope, share, emptyList())

    // ---- search / filter / sort ----
    val searchInput = MutableStateFlow("")
    val query = MutableStateFlow(QueryState())

    private val debouncedSearch = searchInput.debounce { if (it.isEmpty()) 0L else 300L }

    val results: StateFlow<List<RowItem>> = combine(rows, columns, debouncedSearch, query) { r, c, s, q ->
        FilterEngine.apply(r, c, s, q.searchCol, q.filters, q.sort)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, share, emptyList())

    val analysis: StateFlow<Analysis?> = combine(columns, rows) { c, r ->
        if (c.isEmpty()) null else Analyzer.analyze(c, r)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, share, null)

    // ---- UI events ----
    val importState = MutableStateFlow<ImportState>(ImportState.Idle)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun selectFile(id: Long) = sel.selectFile(id).also { clearQuery() }
    fun selectSheet(id: Long) = sel.selectSheet(id).also { clearQuery() }
    fun setAutoSync(on: Boolean) = sel.setAutoSync(on)

    fun importFile(uri: Uri) {
        if (importState.value is ImportState.Running) return
        viewModelScope.launch {
            importState.value = ImportState.Running(0f)
            try {
                val id = repo.importFile(uri) { importState.value = ImportState.Running(it) }
                sel.selectFile(id); sel.selectSheet(0L); clearQuery()
                _messages.send("File imported")
            } catch (e: Exception) {
                _messages.send(e.message ?: "Import failed")
            } finally {
                importState.value = ImportState.Idle
            }
        }
    }

    fun deleteFile(id: Long) = viewModelScope.launch {
        repo.deleteFile(id)
        if (sel.fileId.value == id) sel.selectFile(0L)
        _messages.send("File deleted")
    }

    // ---- editing ----
    fun addRow(values: List<String>) = mutate { repo.addRow(selectedSheetId.value, values) }
    fun updateRow(row: RowItem) = mutate { repo.updateRow(selectedSheetId.value, row) }
    fun deleteRow(row: RowItem) = mutate { repo.deleteRow(selectedSheetId.value, row.id) }

    /** Every edit is saved to Room immediately; with auto-sync on, the Excel file is rewritten too. */
    private fun mutate(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        if (sel.autoSync.value && sel.fileId.value != 0L) {
            val ok = repo.exportToOriginal(sel.fileId.value)
            if (!ok) _messages.send("Saved locally. Auto-sync could not write the original file; use Export.")
        }
    }

    // ---- export ----
    suspend fun suggestedExportName(): String {
        val name = repo.fileName(sel.fileId.value) ?: "export.xlsx"
        val base = name.substringBeforeLast('.'); val ext = name.substringAfterLast('.', "xlsx")
        return "${base}_updated.$ext"
    }

    /** Tries the original file first; [onNeedSaveAs] fires if SAF denies write access. */
    fun export(onNeedSaveAs: () -> Unit) = viewModelScope.launch {
        val id = sel.fileId.value
        if (id == 0L) return@launch
        if (repo.exportToOriginal(id)) _messages.send("Original file updated") else onNeedSaveAs()
    }

    fun exportTo(uri: Uri, open: (Uri) -> OutputStream?) = viewModelScope.launch {
        try {
            val out = open(uri) ?: throw IllegalStateException("Cannot write to that location")
            out.use { repo.export(sel.fileId.value, it) }
            _messages.send("Saved a new copy")
        } catch (e: Exception) {
            _messages.send("Export failed: ${e.message}")
        }
    }

    // ---- query helpers ----
    fun addFilter(f: Filter) = query.update { it.copy(filters = it.filters + f) }
    fun removeFilter(f: Filter) = query.update { it.copy(filters = it.filters - f) }
    fun setSearchColumn(col: Int?) = query.update { it.copy(searchCol = col) }
    fun setSort(s: Sort?) = query.update { it.copy(sort = s) }
    fun clearQuery() { searchInput.value = ""; query.value = QueryState() }
}
