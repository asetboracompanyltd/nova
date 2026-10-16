package com.example.excelmanager.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    NavTab("files", "Files", Icons.Default.Home),
    NavTab("data", "Data", Icons.Default.List),
    NavTab("analysis", "Analysis", Icons.Default.Info),
    NavTab("search", "Search", Icons.Default.Search)
)

private val EXCEL_MIMES = arrayOf(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    "application/octet-stream"
)

@Composable
fun ExcelManagerApp(vm: MainViewModel) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Keep access so Export can write back to the original later. Falls back to read-only.
            val rw = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, rw) }
                .recoverCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.importFile(uri)
            nav.navigate("files")
        }
    }
    val saveAsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) vm.exportTo(uri) { ctx.contentResolver.openOutputStream(it, "wt") }
    }
    val doExport: () -> Unit = {
        vm.export { scope.launch { saveAsLauncher.launch(vm.suggestedExportName()) } }
    }

    val backStack by nav.currentBackStackEntryAsState()
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = backStack?.destination?.route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo("files") { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "files", modifier = Modifier.padding(pad)) {
            composable("files") {
                FilesScreen(vm, onPick = { openLauncher.launch(EXCEL_MIMES) }, onOpen = { nav.navigate("data") })
            }
            composable("data") { DataScreen(vm, onExport = doExport) }
            composable("analysis") { AnalysisScreen(vm) }
            composable("search") { SearchScreen(vm) }
        }
    }
}
