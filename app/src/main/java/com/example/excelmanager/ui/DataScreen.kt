package com.example.excelmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.excelmanager.domain.Col
import com.example.excelmanager.domain.RowItem

private val CellWidth = 140.dp

@Composable
fun DataScreen(vm: MainViewModel, onExport: () -> Unit) {
    val fileId by vm.selectedFileId.collectAsStateWithLifecycle()
    val sheets by vm.sheets.collectAsStateWithLifecycle()
    val sheetId by vm.selectedSheetId.collectAsStateWithLifecycle()
    val columns by vm.columns.collectAsStateWithLifecycle()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val autoSync by vm.autoSync.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<RowItem?>(null) }
    var adding by remember { mutableStateOf(false) }

    if (fileId == 0L || columns.isEmpty()) {
        EmptyState("No data to show", "Open the Files tab, upload a spreadsheet and select it.")
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${rows.size} rows", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(autoSync, { vm.setAutoSync(it) })
                        Spacer(Modifier.width(8.dp))
                        Text("Auto-sync", style = MaterialTheme.typography.bodySmall)
                    }
                }
                FilledTonalButton(onClick = onExport) { Text("Export / Sync") }
            }
            SheetTabs(sheets, sheetId, vm::selectSheet)
            DataTable(columns, rows, onRowClick = { editing = it })
        }
        FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, "Add row")
        }
    }

    if (adding) RowEditorDialog(columns, null,
        onSave = { vm.addRow(it); adding = false }, onDelete = null, onDismiss = { adding = false })
    editing?.let { row ->
        RowEditorDialog(columns, row,
            onSave = { vm.updateRow(row.copy(cells = it)); editing = null },
            onDelete = { vm.deleteRow(row); editing = null },
            onDismiss = { editing = null })
    }
}

/** Horizontally scrollable table; the LazyColumn only composes visible rows so 10k+ rows stay smooth. */
@Composable
fun DataTable(columns: List<Col>, rows: List<RowItem>, onRowClick: (RowItem) -> Unit) {
    Column(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
            columns.forEach {
                Text("${it.name}\n(${it.type.label})", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(CellWidth).padding(8.dp))
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(rows, key = { it.id }) { r ->
                Row(Modifier.clickable { onRowClick(r) }) {
                    columns.forEach { c ->
                        Text(r.cells.getOrElse(c.index) { "" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(CellWidth).padding(8.dp))
                    }
                }
                HorizontalDivider()
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}
