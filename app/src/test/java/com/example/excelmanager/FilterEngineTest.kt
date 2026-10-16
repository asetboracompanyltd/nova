package com.example.excelmanager

import com.example.excelmanager.domain.*
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterEngineTest {
    private val cols = listOf(Col(0, "Name", ColType.TEXT), Col(1, "Salary", ColType.NUMBER), Col(2, "Hired", ColType.DATE))
    private val rows = listOf(
        RowItem(1, 1, listOf("Amina", "1000", "2020-01-10")),
        RowItem(2, 2, listOf("Brian", "2500.5", "2021-06-01")),
        RowItem(3, 3, listOf("amos", "", "2022-03-15")),
        RowItem(4, 4, listOf("Carol", "900", ""))
    )
    private fun ids(r: List<RowItem>) = r.map { it.id }

    @Test fun searchAllColumnsIsCaseInsensitive() =
        assertEquals(listOf(1L, 3L), ids(FilterEngine.apply(rows, cols, "AM", null, emptyList(), null)))

    @Test fun searchSelectedColumnOnly() =
        assertEquals(listOf(2L), ids(FilterEngine.apply(rows, cols, "2500", 1, emptyList(), null)))

    @Test fun textStartsWith() =
        assertEquals(listOf(1L, 3L), ids(FilterEngine.apply(rows, cols, "", null, listOf(Filter(0, Op.STARTS_WITH, "Am")), null)))

    @Test fun numberGreaterThanIgnoresBlanks() =
        assertEquals(listOf(1L, 2L), ids(FilterEngine.apply(rows, cols, "", null, listOf(Filter(1, Op.GT, "950")), null)))

    @Test fun numberBetweenInclusive() =
        assertEquals(listOf(1L, 4L), ids(FilterEngine.apply(rows, cols, "", null, listOf(Filter(1, Op.BETWEEN, "900", "1000")), null)))

    @Test fun dateRange() =
        assertEquals(listOf(2L, 3L), ids(FilterEngine.apply(rows, cols, "", null, listOf(Filter(2, Op.DATE_RANGE, "2021-01-01", "2022-12-31")), null)))

    @Test fun dateBeforeAndAfter() {
        assertEquals(listOf(1L), ids(FilterEngine.apply(rows, cols, "", null, listOf(Filter(2, Op.BEFORE, "2021-01-01")), null)))
        assertEquals(listOf(3L), ids(FilterEngine.apply(rows, cols, "", null, listOf(Filter(2, Op.AFTER, "2021-12-31")), null)))
    }

    @Test fun filtersCombineWithAnd() =
        assertEquals(listOf(2L), ids(FilterEngine.apply(rows, cols, "", null,
            listOf(Filter(1, Op.GT, "950"), Filter(2, Op.AFTER, "2021-01-01")), null)))

    @Test fun invalidFilterValueIsIgnored() =
        assertEquals(4, FilterEngine.apply(rows, cols, "", null, listOf(Filter(1, Op.GT, "abc")), null).size)

    @Test fun numericSortPutsBlanksLast() {
        assertEquals(listOf(4L, 1L, 2L, 3L), ids(FilterEngine.apply(rows, cols, "", null, emptyList(), Sort(1, true))))
        assertEquals(listOf(2L, 1L, 4L, 3L), ids(FilterEngine.apply(rows, cols, "", null, emptyList(), Sort(1, false))))
    }
}
