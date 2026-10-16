package com.example.excelmanager.data.repo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.example.excelmanager.data.db.AppDatabase
import com.example.excelmanager.data.db.ColumnEntity
import com.example.excelmanager.data.db.FileEntity
import com.example.excelmanager.data.db.RowEntity
import com.example.excelmanager.data.db.SheetEntity
import com.example.excelmanager.data.excel.EmptyWorkbookException
import com.example.excelmanager.data.excel.ExcelExporter
import com.example.excelmanager.data.excel.ExcelParser
import com.example.excelmanager.data.excel.ExportSheet
import com.example.excelmanager.domain.Col
import com.example.excelmanager.domain.ColType
import com.example.excelmanager.domain.RowItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class ImportException(message: String) : Exception(message)

@Singleton
class ExcelRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: AppDatabase,
    private val parser: ExcelParser,
    private val exporter: ExcelExporter
) {
    private val dao = db.dao()
    private val storeDir get() = File(ctx.filesDir, "excel").apply { mkdirs() }

    fun files() = dao.observeFiles()
    fun sheets(fileId: Long) = dao.observeSheets(fileId)
    fun columns(sheetId: Long): Flow<List<Col>> = dao.observeColumns(sheetId).map { list ->
        list.map { Col(it.position, it.name, ColType.valueOf(it.type)) }
    }
    fun rows(sheetId: Long): Flow<List<RowItem>> = dao.observeRows(sheetId).map { list ->
        list.map { RowItem(it.id, it.rowIndex, Cells.decode(it.cellsJson)) }
    }

    /** Copies the picked file into app storage, parses it on a background thread and stores everything in Room. */
    suspend fun importFile(uri: Uri, onProgress: (Float) -> Unit): Long = withContext(Dispatchers.IO) {
        val name = displayName(uri)
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext != "xlsx" && ext != "xls") throw ImportException("Unsupported format. Choose an .xlsx or .xls file.")

        val dest = File(storeDir, "${UUID.randomUUID()}.$ext")
        try {
            val input = ctx.contentResolver.openInputStream(uri) ?: throw ImportException("Could not open the file.")
            input.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
            onProgress(0.1f)

            val parsed = try {
                WorkbookFactory.create(dest, null, true).use { wb ->
                    parser.parse(wb) { done, total -> onProgress(0.1f + 0.4f * done / total) }
                }
            } catch (e: EmptyWorkbookException) {
                throw ImportException(e.message ?: "Empty file.")
            } catch (e: Exception) {
                throw ImportException("This file could not be read. It may be corrupt or password-protected.")
            }

            db.withTransaction {
                val fileId = dao.insertFile(FileEntity(name = name, sourceUri = uri.toString(), storedName = dest.name, uploadedAt = System.currentTimeMillis()))
                parsed.forEachIndexed { si, s ->
                    val sheetId = dao.insertSheet(SheetEntity(fileId = fileId, name = s.name, orderIndex = si, headerRowIndex = s.headerRowIndex, rowCount = s.rows.size))
                    dao.insertColumns(s.columns.mapIndexed { i, c -> ColumnEntity(sheetId = sheetId, position = i, name = c.name, type = c.type.name) })
                    // Chunked inserts keep memory and transaction size reasonable for 10k+ rows.
                    s.rows.withIndex().chunked(500).forEach { chunk ->
                        dao.insertRows(chunk.map { (i, r) -> RowEntity(sheetId = sheetId, rowIndex = i, cellsJson = Cells.encode(r)) })
                        onProgress(0.5f + 0.5f * ((si + 0.5f) / parsed.size))
                    }
                }
                onProgress(1f)
                fileId
            }
        } catch (e: Exception) {
            dest.delete()
            throw e
        }
    }

    suspend fun deleteFile(fileId: Long) = withContext(Dispatchers.IO) {
        dao.file(fileId)?.let {
            File(storeDir, it.storedName).delete()
            dao.deleteFile(it)
        }
    }

    suspend fun addRow(sheetId: Long, cells: List<String>) {
        dao.insertRow(RowEntity(sheetId = sheetId, rowIndex = dao.maxRowIndex(sheetId) + 1, cellsJson = Cells.encode(cells)))
        dao.refreshRowCount(sheetId)
    }

    suspend fun updateRow(sheetId: Long, row: RowItem) =
        dao.updateRow(RowEntity(row.id, sheetId, row.rowIndex, Cells.encode(row.cells)))

    suspend fun deleteRow(sheetId: Long, rowId: Long) {
        dao.deleteRow(rowId); dao.refreshRowCount(sheetId)
    }

    suspend fun fileName(fileId: Long) = dao.file(fileId)?.name

    /** Writes the current database state into the original workbook layout. */
    suspend fun export(fileId: Long, out: OutputStream) = withContext(Dispatchers.IO) {
        val file = dao.file(fileId) ?: throw ImportException("File not found.")
        val sheets = dao.sheets(fileId).map { s ->
            ExportSheet(
                s.name, s.headerRowIndex,
                dao.columns(s.id).map { Col(it.position, it.name, ColType.valueOf(it.type)) },
                dao.rows(s.id).map { Cells.decode(it.cellsJson) }
            )
        }
        exporter.export(File(storeDir, file.storedName), sheets, out)
    }

    /** Tries to overwrite the original document via SAF. Returns false if write access is not available. */
    suspend fun exportToOriginal(fileId: Long): Boolean = withContext(Dispatchers.IO) {
        val uri = dao.file(fileId)?.sourceUri ?: return@withContext false
        try {
            val buffer = ByteArrayOutputStream()   // build fully in memory first so a failure never truncates the original
            export(fileId, buffer)
            ctx.contentResolver.openOutputStream(Uri.parse(uri), "wt")?.use { it.write(buffer.toByteArray()) } ?: return@withContext false
            true
        } catch (e: Exception) { false }
    }

    private fun displayName(uri: Uri): String =
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: uri.lastPathSegment ?: "file.xlsx"
}
