package com.example.excelmanager.data.repo

import org.json.JSONArray

/** JSON <-> List<String> for the RowEntity.cellsJson column. */
object Cells {
    fun encode(cells: List<String>): String = JSONArray(cells).toString()
    fun decode(json: String): List<String> {
        val a = JSONArray(json)
        return List(a.length()) { a.optString(it, "") }
    }
}
