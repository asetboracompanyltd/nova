package com.example.excelmanager.data.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the current file/sheet and the auto-sync toggle so they survive process death. */
@Singleton
class SelectionStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("selection", Context.MODE_PRIVATE)
    private val _fileId = MutableStateFlow(prefs.getLong("file", 0L))
    private val _sheetId = MutableStateFlow(prefs.getLong("sheet", 0L))
    private val _autoSync = MutableStateFlow(prefs.getBoolean("autosync", false))
    val fileId: StateFlow<Long> = _fileId
    val sheetId: StateFlow<Long> = _sheetId
    val autoSync: StateFlow<Boolean> = _autoSync

    fun selectFile(id: Long) { _fileId.value = id; prefs.edit().putLong("file", id).apply() }
    fun selectSheet(id: Long) { _sheetId.value = id; prefs.edit().putLong("sheet", id).apply() }
    fun setAutoSync(on: Boolean) { _autoSync.value = on; prefs.edit().putBoolean("autosync", on).apply() }
}
