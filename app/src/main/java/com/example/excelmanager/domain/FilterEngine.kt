package com.example.excelmanager.domain

import java.time.LocalDate

enum class Op(val label: String) {
    CONTAINS("contains"), EQUALS("equals"), STARTS_WITH("starts with"),
    GT("greater than"), LT("less than"), BETWEEN("between"),
    BEFORE("before"), AFTER("after"), DATE_RANGE("date range")
}

fun opsFor(type: ColType): List<Op> = when (type) {
    ColType.TEXT, ColType.BOOLEAN -> listOf(Op.CONTAINS, Op.EQUALS, Op.STARTS_WITH)
    ColType.NUMBER -> listOf(Op.EQUALS, Op.GT, Op.LT, Op.BETWEEN)
    ColType.DATE -> listOf(Op.EQUALS, Op.BEFORE, Op.AFTER, Op.DATE_RANGE)
}

data class Filter(val col: Int, val op: Op, val v1: String, val v2: String = "")
data class Sort(val col: Int, val ascending: Boolean = true)

/** Pure, UI-free filtering/sorting so it can be unit-tested on the JVM. */
object FilterEngine {

    fun apply(
        rows: List<RowItem>,
        columns: List<Col>,
        search: String,
        searchCol: Int?,
        filters: List<Filter>,
        sort: Sort?
    ): List<RowItem> {
        val q = search.trim()
        val types = columns.associate { it.index to it.type }
        val filtered = rows.filter { r ->
            val searchOk = q.isEmpty() ||
                if (searchCol != null) r.cells.getOrElse(searchCol) { "" }.contains(q, ignoreCase = true)
                else r.cells.any { it.contains(q, ignoreCase = true) }
            searchOk && filters.all { f ->
                matches(r.cells.getOrElse(f.col) { "" }, f, types[f.col] ?: ColType.TEXT)
            }
        }
        if (sort == null) return filtered
        val type = types[sort.col] ?: ColType.TEXT
        return filtered.sortedWith { a, b ->
            val x = a.cells.getOrElse(sort.col) { "" }
            val y = b.cells.getOrElse(sort.col) { "" }
            when {
                x.isBlank() && y.isBlank() -> 0
                x.isBlank() -> 1          // blanks always last
                y.isBlank() -> -1
                else -> compare(x, y, type).let { if (sort.ascending) it else -it }
            }
        }
    }

    private fun compare(x: String, y: String, type: ColType): Int = when (type) {
        ColType.NUMBER -> (x.toDoubleOrNull() ?: 0.0).compareTo(y.toDoubleOrNull() ?: 0.0)
        ColType.DATE -> x.compareTo(y) // ISO dates sort lexicographically
        else -> String.CASE_INSENSITIVE_ORDER.compare(x, y)
    }

    /** A filter whose value cannot be parsed is ignored (treated as a match) instead of hiding everything. */
    fun matches(cell: String, f: Filter, type: ColType): Boolean {
        val v1 = f.v1.trim()
        return when (f.op) {
            Op.CONTAINS -> v1.isEmpty() || cell.contains(v1, true)
            Op.STARTS_WITH -> v1.isEmpty() || cell.startsWith(v1, true)
            Op.EQUALS -> when {
                v1.isEmpty() -> true
                type == ColType.NUMBER -> num(cell) != null && num(cell) == num(v1)
                else -> cell.trim().equals(v1, true)
            }
            Op.GT -> cmpNum(cell, v1) { n, a -> n > a }
            Op.LT -> cmpNum(cell, v1) { n, a -> n < a }
            Op.BETWEEN -> {
                val lo = num(v1); val hi = num(f.v2)
                if (lo == null || hi == null) true else num(cell)?.let { it >= minOf(lo, hi) && it <= maxOf(lo, hi) } ?: false
            }
            Op.BEFORE -> cmpDate(cell, v1) { d, a -> d.isBefore(a) }
            Op.AFTER -> cmpDate(cell, v1) { d, a -> d.isAfter(a) }
            Op.DATE_RANGE -> {
                val lo = date(v1); val hi = date(f.v2)
                if (lo == null || hi == null) true else date(cell)?.let { !it.isBefore(lo) && !it.isAfter(hi) } ?: false
            }
        }
    }

    private fun num(s: String) = s.trim().toDoubleOrNull()
    private fun date(s: String) = runCatching { LocalDate.parse(s.trim()) }.getOrNull()
    private fun cmpNum(cell: String, arg: String, f: (Double, Double) -> Boolean): Boolean {
        val a = num(arg) ?: return true
        return num(cell)?.let { f(it, a) } ?: false
    }
    private fun cmpDate(cell: String, arg: String, f: (LocalDate, LocalDate) -> Boolean): Boolean {
        val a = date(arg) ?: return true
        return date(cell)?.let { f(it, a) } ?: false
    }
}
