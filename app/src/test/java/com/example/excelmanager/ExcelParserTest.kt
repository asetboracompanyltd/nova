package com.example.excelmanager

import com.example.excelmanager.data.excel.EmptyWorkbookException
import com.example.excelmanager.data.excel.ExcelParser
import com.example.excelmanager.domain.ColType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Test

class ExcelParserTest {
    private fun workbook(): XSSFWorkbook = XSSFWorkbook().apply {
        val s = createSheet("People")
        val dateStyle = createCellStyle().apply { dataFormat = creationHelper.createDataFormat().getFormat("yyyy-mm-dd") }
        s.createRow(0).apply { createCell(0).setCellValue("Name"); createCell(1).setCellValue("Age"); createCell(2).setCellValue("Joined"); createCell(3).setCellValue("Active") }
        s.createRow(1).apply {
            createCell(0).setCellValue("Amina"); createCell(1).setCellValue(31.0)
            createCell(2).apply { setCellValue(java.util.Date(1700000000000L)); cellStyle = dateStyle }
            createCell(3).setCellValue(true)
        }
        s.createRow(2) // blank row is skipped
        s.createRow(3).apply { createCell(0).setCellValue("Brian"); createCell(1).setCellValue(40.5) }
    }

    @Test fun detectsHeaderTypesAndSkipsBlankRows() {
        val sheet = ExcelParser().parse(workbook()).single()
        assertEquals(listOf("Name", "Age", "Joined", "Active"), sheet.columns.map { it.name })
        assertEquals(listOf(ColType.TEXT, ColType.NUMBER, ColType.DATE, ColType.BOOLEAN), sheet.columns.map { it.type })
        assertEquals(2, sheet.rows.size)
        assertEquals("40.5", sheet.rows[1][1])
        assertEquals("", sheet.rows[1][3])
    }

    @Test(expected = EmptyWorkbookException::class) fun emptyWorkbookFails() {
        ExcelParser().parse(XSSFWorkbook().apply { createSheet("Empty") })
    }
}
