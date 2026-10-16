package com.example.excelmanager.data.excel

import com.example.excelmanager.domain.ColType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import java.math.BigDecimal
import java.time.ZoneId
import javax.inject.Inject

data class ParsedColumn(val name: String, val type: ColType)
data class ParsedSheet(
    val name: String,
    val headerRowIndex: Int,
    val columns: List<ParsedColumn>,
    val rows: List<List<String>>
)

class EmptyWorkbookException : Exception("This file has no readable data.")

class ExcelParser @Inject constructor() {

    fun parse(workbook: Workbook, onSheet: (done: Int, total: Int) -> Unit = { _, _ -> }): List<ParsedSheet> {
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val out = mutableListOf<ParsedSheet>()
        for (i in 0 until workbook.numberOfSheets) {
            onSheet(i, workbook.numberOfSheets)
            parseSheet(workbook.getSheetAt(i), evaluator)?.let(out::add)
        }
        if (out.isEmpty()) throw EmptyWorkbookException()
        return out
    }

    private fun parseSheet(sheet: Sheet, ev: FormulaEvaluator): ParsedSheet? {
        if (sheet.physicalNumberOfRows == 0) return null
        val headerIdx = detectHeaderRow(sheet, ev) ?: return null
        val header = sheet.getRow(headerIdx)
        val width = header.lastCellNum.toInt().coerceAtLeast(1)
        val names = List(width) { c -> read(header.getCell(c), ev)?.second ?: "Column ${c + 1}" }

        val kinds = List(width) { mutableSetOf<ColType>() }
        val rows = mutableListOf<List<String>>()
        for (r in headerIdx + 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val values = List(width) { c ->
                val parsed = read(row.getCell(c), ev)
                if (parsed != null) kinds[c].add(parsed.first)
                parsed?.second ?: ""
            }
            if (values.any { it.isNotBlank() }) rows.add(values) // skip fully blank rows
        }
        // A column gets a specific type only when every non-blank cell agrees; otherwise it is TEXT.
        val columns = names.mapIndexed { c, n ->
            ParsedColumn(n, kinds[c].singleOrNull() ?: ColType.TEXT)
        }
        return ParsedSheet(sheet.sheetName, headerIdx, columns, rows)
    }

    /** The header is the first of the top 10 rows whose non-blank cells are all text; falls back to the first non-empty row. */
    private fun detectHeaderRow(sheet: Sheet, ev: FormulaEvaluator): Int? {
        var firstNonEmpty: Int? = null
        for (r in sheet.firstRowNum..minOf(sheet.lastRowNum, sheet.firstRowNum + 9)) {
            val row = sheet.getRow(r) ?: continue
            val cells = (0 until row.lastCellNum.coerceAtLeast(0)).mapNotNull { read(row.getCell(it), ev) }
            if (cells.isEmpty()) continue
            if (firstNonEmpty == null) firstNonEmpty = r
            if (cells.all { it.first == ColType.TEXT }) return r
        }
        return firstNonEmpty
    }

    /** Converts a cell to (type, canonical string); null when blank. */
    private fun read(cell: Cell?, ev: FormulaEvaluator): Pair<ColType, String>? {
        if (cell == null) return null
        var type = cell.cellType
        var cached: org.apache.poi.ss.usermodel.CellValue? = null
        if (type == CellType.FORMULA) {
            try { cached = ev.evaluate(cell); type = cached.cellType }
            catch (e: Exception) { return ColType.TEXT to cell.toString() }
        }
        return when (type) {
            CellType.NUMERIC -> {
                val d = cached?.numberValue ?: cell.numericCellValue
                if (isDate(cell)) ColType.DATE to DateUtil.getJavaDate(d).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                else ColType.NUMBER to BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()
            }
            CellType.BOOLEAN -> ColType.BOOLEAN to (cached?.booleanValue ?: cell.booleanCellValue).toString()
            CellType.STRING -> (cached?.stringValue ?: cell.stringCellValue).trim()
                .takeIf { it.isNotEmpty() }?.let { ColType.TEXT to it }
            else -> null
        }
    }

    private fun isDate(cell: Cell): Boolean {
        val style = cell.cellStyle ?: return false
        return DateUtil.isADateFormat(style.dataFormat.toInt(), style.dataFormatString)
    }
}
