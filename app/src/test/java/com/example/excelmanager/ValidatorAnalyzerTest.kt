package com.example.excelmanager

import com.example.excelmanager.domain.*
import org.junit.Assert.*
import org.junit.Test

class ValidatorAnalyzerTest {
    @Test fun validatorAcceptsAndRejects() {
        assertNull(Validator.validate(ColType.NUMBER, "12.5"))
        assertNotNull(Validator.validate(ColType.NUMBER, "abc"))
        assertNull(Validator.validate(ColType.DATE, "2024-02-29"))
        assertNotNull(Validator.validate(ColType.DATE, "29/02/2024"))
        assertNull(Validator.validate(ColType.BOOLEAN, "TRUE"))
        assertNotNull(Validator.validate(ColType.BOOLEAN, "yes"))
        assertNull(Validator.validate(ColType.NUMBER, "  ")) // blank allowed
    }

    @Test fun analyzerCountsEmptyDuplicatesAndStats() {
        val cols = listOf(Col(0, "Dept", ColType.TEXT), Col(1, "Pay", ColType.NUMBER))
        val rows = listOf(
            RowItem(1, 1, listOf("A", "10")), RowItem(2, 2, listOf("A", "10")),
            RowItem(3, 3, listOf("B", "30")), RowItem(4, 4, listOf("", ""))
        )
        val a = Analyzer.analyze(cols, rows)
        assertEquals(1, a.duplicateRows)
        assertEquals(1, a.stats[0].empty)
        assertEquals("A", a.stats[0].top.first().first)
        val n = a.stats[1].num!!
        assertEquals(50.0, n.sum, 0.0); assertEquals(10.0, n.min, 0.0); assertEquals(30.0, n.max, 0.0)
        assertEquals(50.0 / 3, n.avg, 1e-9)
    }
}
