package com.example.excelmanager.domain

import java.time.LocalDate

/** Validates user input against the column type detected at upload. Blank is always allowed. */
object Validator {
    fun validate(type: ColType, value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return null
        return when (type) {
            ColType.TEXT -> null
            ColType.NUMBER -> if (v.toDoubleOrNull() == null) "Enter a number" else null
            ColType.DATE -> if (runCatching { LocalDate.parse(v) }.isFailure) "Use yyyy-MM-dd" else null
            ColType.BOOLEAN -> if (v.lowercase() in setOf("true", "false")) null else "Use true or false"
        }
    }

    /** Returns column index -> error message for every invalid field. */
    fun validateRow(columns: List<Col>, values: List<String>): Map<Int, String> =
        columns.mapNotNull { c -> validate(c.type, values.getOrElse(c.index) { "" })?.let { c.index to it } }.toMap()
}
