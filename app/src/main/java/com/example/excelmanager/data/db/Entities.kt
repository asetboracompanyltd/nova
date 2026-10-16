package com.example.excelmanager.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One uploaded workbook. [storedName] is our private copy of the original file (used as the export template). */
@Entity
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceUri: String?,
    val storedName: String,
    val uploadedAt: Long
)

@Entity(
    foreignKeys = [ForeignKey(FileEntity::class, ["id"], ["fileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("fileId")]
)
data class SheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: Long,
    val name: String,
    val orderIndex: Int,
    val headerRowIndex: Int,
    val rowCount: Int
)

@Entity(
    foreignKeys = [ForeignKey(SheetEntity::class, ["id"], ["sheetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sheetId")]
)
data class ColumnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val position: Int,
    val name: String,
    val type: String
)

/** Row values are stored as a JSON array of strings, aligned with ColumnEntity.position. */
@Entity(
    foreignKeys = [ForeignKey(SheetEntity::class, ["id"], ["sheetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sheetId")]
)
data class RowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val rowIndex: Int,
    val cellsJson: String
)
