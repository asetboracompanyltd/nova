package com.example.excelmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.excelmanager.data.db.SheetEntity
import com.example.excelmanager.domain.Col
import com.example.excelmanager.domain.ColType
import com.example.excelmanager.domain.RowItem
import com.example.excelmanager.domain.Validator

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SheetTabs(sheets: List<SheetEntity>, selectedId: Long, onSelect: (Long) -> Unit) {
    if (sheets.size < 2) return
    val index = sheets.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    ScrollableTabRow(selectedTabIndex = index, edgePadding = 8.dp) {
        sheets.forEach { s ->
            Tab(selected = s.id == selectedId, onClick = { onSelect(s.id) },
                text = { Text("${s.name} (${s.rowCount})") })
        }
    }
}

/** Highlights every case-insensitive occurrence of [term] inside [text]. */
@Composable
fun highlight(text: String, term: String, prefix: String = ""): AnnotatedString {
    val bg = MaterialTheme.colorScheme.tertiaryContainer
    val fg = MaterialTheme.colorScheme.onTertiaryContainer
    return buildAnnotatedString {
        append(prefix)
        val t = term.trim()
        if (t.isEmpty()) { append(text); return@buildAnnotatedString }
        var i = 0
        while (i < text.length) {
            val hit = text.indexOf(t, i, ignoreCase = true)
            if (hit < 0) { append(text.substring(i)); break }
            append(text.substring(i, hit))
            withStyle(SpanStyle(background = bg, color = fg, fontWeight = FontWeight.Bold)) { append(text.substring(hit, hit + t.length)) }
            i = hit + t.length
        }
    }
}

/** Add / edit / view dialog. Values are validated against each column's detected type before saving. */
@Composable
fun RowEditorDialog(
    columns: List<Col>,
    initial: RowItem?,
    onSave: (List<String>) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val values = remember { mutableStateListOf<String>().apply { columns.forEach { add(initial?.cells?.getOrNull(it.index) ?: "") } } }
    var errors by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add row" else "Row details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                columns.forEachIndexed { i, c ->
                    OutlinedTextField(
                        value = values[i], onValueChange = { values[i] = it; errors = errors - c.index },
                        label = { Text("${c.name} (${c.type.name.lowercase()})") },
                        isError = errors.containsKey(c.index),
                        supportingText = { errors[c.index]?.let { Text(it) } },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val padded = MutableList(columns.maxOf { it.index } + 1) { "" }
                columns.forEachIndexed { i, c -> padded[c.index] = values[i].trim() }
                val errs = Validator.validateRow(columns, padded)
                if (errs.isEmpty()) onSave(padded) else errors = errs
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/** Simple dropdown used for column / operator pickers. */
@Composable
fun <T> Dropdown(label: String, selected: String, options: List<Pair<String, T>>, onPick: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $selected", maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, value) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; onPick(value) })
            }
        }
    }
}

val ColType.label get() = name.lowercase()
