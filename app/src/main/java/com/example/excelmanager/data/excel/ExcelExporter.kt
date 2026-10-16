package com.example.excelmanager.data.excel

import com.example.excelmanager.domain.Col
import com.example.excelmanager.domain.ColType
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

data class ExportSheet(val name: String, val headerRowIndex: Int, val columns: List<Col>, val rows: List<List<String>>)

/**
 * Format-preserving export: instead of building a new workbook, we re-open the ORIGINAL file and
 * overwrite only the data rows. Sheet names, header row, column widths, fonts, fills and any
 * other sheets stay exactly as they were. Each column's number/date format is copied from the
 * first existing data cell of that column.
 */
class ExcelExporter @Inject constructor() {

    fun export(source: File, sheets: List<ExportSheet>, out: OutputStream) {
        source.inputStream().use { input ->
            WorkbookFactory.create(input).use { wb ->
                sheets.forEach { writeSheet(wb, it) }
                wb.write(out)
            }
        }
    }

    private fun writeSheet(wb: Workbook, s: ExportSheet) {
        val sheet = wb.getSheet(s.name) ?: return
        val h = s.headerRowIndex

        // 1. Remember one style per column from the existing data rows.
        val scanTo = minOf(sheet.lastRowNum, h + 50)
        val styles: List<CellStyle?> = s.columns.map { col ->
            (h + 1..scanTo).firstNotNullOfOrNull { r -> sheet.getRow(r)?.getCell(col.index)?.cellStyle }
        }
        // Fallback date style for columns that had no data originally.
        val dateStyle by lazy {
            wb.createCellStyle().apply { dataFormat = wb.creationHelper.createDataFormat().getFormat("yyyy-mm-dd") }
        }

        // 2. Clear old data rows (header stays).
        for (r in sheet.lastRowNum downTo h + 1) sheet.getRow(r)?.let { sheet.removeRow(it) }

        // 3. Write current rows back, keeping each column's type.
        s.rows.forEachIndexed { i, values ->
            val row = sheet.createRow(h + 1 + i)
            s.columns.forEachIndexed { ci, col ->
                val v = values.getOrElse(col.index) { "" }.trim()
                val cell = row.createCell(col.index)
                styles[ci]?.let { cell.cellStyle = it }
                if (v.isEmpty()) return@forEachIndexed
                when (col.type) {
                    ColType.NUMBER -> v.toDoubleOrNull()?.let { cell.setCellValue(it) } ?: cell.setCellValue(v)
                    ColType.BOOLEAN -> cell.setCellValue(v.equals("true", ignoreCase = true))
                    ColType.DATE -> runCatching { LocalDate.parse(v) }.fold(
                        onSuccess = {
                            cell.setCellValue(Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                            if (styles[ci] == null) cell.cellStyle = dateStyle
                        },
                        onFailure = { cell.setCellValue(v) }
                    )
                    ColType.TEXT -> cell.setCellValue(v)
                }
            }
        }
    }
}
