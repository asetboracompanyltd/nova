package com.example.excelmanager.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcelDao {
    @Query("SELECT * FROM FileEntity ORDER BY uploadedAt DESC") fun observeFiles(): Flow<List<FileEntity>>
    @Query("SELECT * FROM FileEntity WHERE id = :id") suspend fun file(id: Long): FileEntity?
    @Insert suspend fun insertFile(f: FileEntity): Long
    @Delete suspend fun deleteFile(f: FileEntity)

    @Query("SELECT * FROM SheetEntity WHERE fileId = :fileId ORDER BY orderIndex") fun observeSheets(fileId: Long): Flow<List<SheetEntity>>
    @Query("SELECT * FROM SheetEntity WHERE fileId = :fileId ORDER BY orderIndex") suspend fun sheets(fileId: Long): List<SheetEntity>
    @Insert suspend fun insertSheet(s: SheetEntity): Long
    @Query("UPDATE SheetEntity SET rowCount = (SELECT COUNT(*) FROM RowEntity WHERE sheetId = :sheetId) WHERE id = :sheetId")
    suspend fun refreshRowCount(sheetId: Long)

    @Insert suspend fun insertColumns(c: List<ColumnEntity>)
    @Query("SELECT * FROM ColumnEntity WHERE sheetId = :sheetId ORDER BY position") fun observeColumns(sheetId: Long): Flow<List<ColumnEntity>>
    @Query("SELECT * FROM ColumnEntity WHERE sheetId = :sheetId ORDER BY position") suspend fun columns(sheetId: Long): List<ColumnEntity>

    @Insert suspend fun insertRows(r: List<RowEntity>)
    @Insert suspend fun insertRow(r: RowEntity): Long
    @Update suspend fun updateRow(r: RowEntity)
    @Query("DELETE FROM RowEntity WHERE id = :id") suspend fun deleteRow(id: Long)
    @Query("SELECT * FROM RowEntity WHERE sheetId = :sheetId ORDER BY rowIndex, id") fun observeRows(sheetId: Long): Flow<List<RowEntity>>
    @Query("SELECT * FROM RowEntity WHERE sheetId = :sheetId ORDER BY rowIndex, id") suspend fun rows(sheetId: Long): List<RowEntity>
    @Query("SELECT COALESCE(MAX(rowIndex), 0) FROM RowEntity WHERE sheetId = :sheetId") suspend fun maxRowIndex(sheetId: Long): Int
}

@Database(
    entities = [FileEntity::class, SheetEntity::class, ColumnEntity::class, RowEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): ExcelDao
}
