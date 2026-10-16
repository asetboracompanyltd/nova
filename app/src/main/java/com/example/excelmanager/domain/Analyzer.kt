package com.example.excelmanager.domain

data class NumStat(val sum: Double, val avg: Double, val min: Double, val max: Double)
data class ColStat(val col: Col, val empty: Int, val num: NumStat?, val top: List<Pair<String, Int>>)
data class Analysis(val rows: Int, val cols: Int, val duplicateRows: Int, val stats: List<ColStat>)

object Analyzer {
    fun analyze(columns: List<Col>, rows: List<RowItem>): Analysis {
        // Duplicates = rows beyond the first occurrence of an identical row.
        val duplicates = rows.groupingBy { it.cells }.eachCount().values.sumOf { if (it > 1) it - 1 else 0 }
        val stats = columns.map { col ->
            val values = rows.map { it.cells.getOrElse(col.index) { "" } }
            val filled = values.filter { it.isNotBlank() }
            val num = if (col.type == ColType.NUMBER) {
                val nums = filled.mapNotNull { it.toDoubleOrNull() }
                if (nums.isEmpty()) null else NumStat(nums.sum(), nums.average(), nums.min(), nums.max())
            } else null
            val top = if (col.type == ColType.TEXT || col.type == ColType.BOOLEAN)
                filled.groupingBy { it }.eachCount().entries
                    .sortedByDescending { it.value }.take(5).map { it.key to it.value }
            else emptyList()
            ColStat(col, values.size - filled.size, num, top)
        }
        return Analysis(rows.size, columns.size, duplicates, stats)
    }
}
