package com.example.excelmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.excelmanager.domain.ColStat
import java.util.Locale

private fun fmt(d: Double) = if (d == Math.floor(d) && Math.abs(d) < 1e12) "%,d".format(Locale.US, d.toLong()) else "%,.2f".format(Locale.US, d)

@Composable
fun AnalysisScreen(vm: MainViewModel) {
    val a by vm.analysis.collectAsStateWithLifecycle()
    val sheets by vm.sheets.collectAsStateWithLifecycle()
    val sheetId by vm.selectedSheetId.collectAsStateWithLifecycle()
    val analysis = a
    if (analysis == null) { EmptyState("Nothing to analyze", "Upload and select a file first."); return }

    Column(Modifier.fillMaxSize()) {
        SheetTabs(sheets, sheetId, vm::selectSheet)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stat("Rows", analysis.rows.toString()); Stat("Columns", analysis.cols.toString())
                        Stat("Duplicates", analysis.duplicateRows.toString())
                    }
                }
            }
            items(analysis.stats, key = { it.col.index }) { ColumnCard(it, analysis.rows) }
        }
    }
}

@Composable private fun RowScope.Stat(label: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ColumnCard(s: ColStat, totalRows: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.col.name, style = MaterialTheme.typography.titleMedium)
            Text("Type: ${s.col.type.label}  •  Empty: ${s.empty}", style = MaterialTheme.typography.bodySmall)
            s.num?.let {
                Text("Sum ${fmt(it.sum)}  •  Avg ${fmt(it.avg)}", style = MaterialTheme.typography.bodyMedium)
                Text("Min ${fmt(it.min)}  •  Max ${fmt(it.max)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (s.top.isNotEmpty()) {
                Text("Most frequent", style = MaterialTheme.typography.labelMedium)
                s.top.forEach { (value, count) ->
                    Text("$value — $count", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { if (totalRows == 0) 0f else count / totalRows.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
