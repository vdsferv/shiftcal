package com.example.shiftcal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ShiftDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "ShiftDB"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "shifts"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_SHIFT = "shift"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME ($COLUMN_DATE TEXT PRIMARY KEY, $COLUMN_SHIFT TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun saveShift(date: String, shift: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_SHIFT, shift)
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getShift(date: String): String? {
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, arrayOf(COLUMN_SHIFT), "$COLUMN_DATE=?", arrayOf(date), null, null, null)
        return if (cursor?.moveToFirst() == true) {
            val shift = cursor.getString(0)
            cursor.close()
            shift
        } else {
            cursor?.close()
            null
        }
    }
}