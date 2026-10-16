package com.example.excelmanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.excelmanager.domain.*

@Composable
fun SearchScreen(vm: MainViewModel) {
    val columns by vm.columns.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val input by vm.searchInput.collectAsStateWithLifecycle()
    val q by vm.query.collectAsStateWithLifecycle()
    val sheets by vm.sheets.collectAsStateWithLifecycle()
    val sheetId by vm.selectedSheetId.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RowItem?>(null) }

    if (columns.isEmpty()) { EmptyState("Nothing to search", "Upload and select a file first."); return }
    val name = { i: Int -> columns.firstOrNull { it.index == i }?.name ?: "?" }

    Column(Modifier.fillMaxSize()) {
        SheetTabs(sheets, sheetId, vm::selectSheet)
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(input, { vm.searchInput.value = it }, label = { Text("Search") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dropdown("In", q.searchCol?.let(name) ?: "All columns",
                    listOf("All columns" to null) + columns.map { it.name to it.index as Int? },
                    onPick = vm::setSearchColumn, modifier = Modifier.weight(1f))
                Dropdown("Sort", q.sort?.let { "${name(it.col)} ${if (it.ascending) "▲" else "▼"}" } ?: "None",
                    listOf("None" to null) + columns.flatMap {
                        listOf("${it.name} ▲" to Sort(it.index, true), "${it.name} ▼" to Sort(it.index, false))
                    }.map { it.first to it.second as Sort? },
                    onPick = vm::setSort, modifier = Modifier.weight(1f))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { showFilter = true }, label = { Text("+ Filter") })
                q.filters.forEach { f ->
                    InputChip(selected = true, onClick = { vm.removeFilter(f) },
                        label = { Text("${name(f.col)} ${f.op.label} ${f.v1}${if (f.v2.isNotEmpty()) " – ${f.v2}" else ""}") },
                        trailingIcon = { Text("✕") })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${results.size} results", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = vm::clearQuery,
                    enabled = input.isNotEmpty() || q.filters.isNotEmpty() || q.sort != null || q.searchCol != null) { Text("Clear filters") }
            }
        }
        if (results.isEmpty()) EmptyState("No matches", "Try a different search or remove a filter.")
        else LazyColumn(contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth().clickable { editing = r }) {
                    Column(Modifier.padding(12.dp)) {
                        columns.forEach { c ->
                            val v = r.cells.getOrElse(c.index) { "" }
                            if (v.isNotBlank()) Text(
                                highlight(v, input, prefix = "${c.name}: "), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showFilter) FilterDialog(columns, onAdd = { vm.addFilter(it); showFilter = false }, onDismiss = { showFilter = false })
    editing?.let { row ->
        RowEditorDialog(columns, row,
            onSave = { vm.updateRow(row.copy(cells = it)); editing = null },
            onDelete = { vm.deleteRow(row); editing = null }, onDismiss = { editing = null })
    }
}

@Composable
private fun FilterDialog(columns: List<Col>, onAdd: (Filter) -> Unit, onDismiss: () -> Unit) {
    var col by remember { mutableStateOf(columns.first()) }
    var op by remember { mutableStateOf(opsFor(col.type).first()) }
    var v1 by remember { mutableStateOf("") }
    var v2 by remember { mutableStateOf("") }
    val two = op == Op.BETWEEN || op == Op.DATE_RANGE
    val hint = if (col.type == ColType.DATE) "yyyy-MM-dd" else if (col.type == ColType.NUMBER) "number" else "text"

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Add filter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Dropdown("Column", "${col.name} (${col.type.label})", columns.map { "${it.name} (${it.type.label})" to it },
                    onPick = { col = it; op = opsFor(it.type).first(); v1 = ""; v2 = "" })
                Dropdown("Condition", op.label, opsFor(col.type).map { it.label to it }, onPick = { op = it })
                OutlinedTextField(v1, { v1 = it }, label = { Text(if (two) "From ($hint)" else "Value ($hint)") }, singleLine = true)
                if (two) OutlinedTextField(v2, { v2 = it }, label = { Text("To ($hint)") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(Filter(col.index, op, v1.trim(), v2.trim())) }, enabled = v1.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
