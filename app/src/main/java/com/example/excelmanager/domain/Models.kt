package com.example.excelmanager.domain

enum class ColType { TEXT, NUMBER, DATE, BOOLEAN }

/** A column of a sheet. [index] is the position in the original sheet (and in [RowItem.cells]). */
data class Col(val index: Int, val name: String, val type: ColType)

/** One data row. Every value is stored as a canonical string: numbers plain, dates ISO (yyyy-MM-dd), booleans true/false. */
data class RowItem(val id: Long, val rowIndex: Int, val cells: List<String>)
