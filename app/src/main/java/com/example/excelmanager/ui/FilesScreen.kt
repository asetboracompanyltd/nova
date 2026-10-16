package com.example.excelmanager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date

@Composable
fun FilesScreen(vm: MainViewModel, onPick: () -> Unit, onOpen: () -> Unit) {
    val files by vm.files.collectAsStateWithLifecycle()
    val selected by vm.selectedFileId.collectAsStateWithLifecycle()
    val imp by vm.importState.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Long?>(null) }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text("Files", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            (imp as? ImportState.Running)?.let {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Importing… ${(it.progress * 100).toInt()}%")
                    LinearProgressIndicator(progress = { it.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (files.isEmpty()) {
                EmptyState("No files yet", "Tap “Upload Excel” to pick an .xlsx or .xls file. A sample file is included in the project's sample folder.")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files, key = { it.id }) { f ->
                        Card(
                            Modifier.fillMaxWidth().clickable { vm.selectFile(f.id); onOpen() },
                            colors = CardDefaults.cardColors(
                                containerColor = if (f.id == selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(f.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Uploaded ${fmt.format(Date(f.uploadedAt))}", style = MaterialTheme.typography.bodySmall)
                                    if (f.id == selected) Text("Currently open", style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = { toDelete = f.id }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onPick, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Upload Excel") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
    toDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete file?") },
            text = { Text("This removes the file and its data from the app. The original on your phone is not touched.") },
            confirmButton = { TextButton(onClick = { vm.deleteFile(id); toDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}
