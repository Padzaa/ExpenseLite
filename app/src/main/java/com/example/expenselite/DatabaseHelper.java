package com.example.expenselite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "expense_lite.db";
    public static final int DB_VERSION = 1;

    public static final String TABLE_EXPENSES = "expenses";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_CATEGORY = "category";
    public static final String COL_CREATED_AT = "created_at";

    // Passes the database file name and version number to the Android framework.
    // The framework uses those to decide whether to create a new file or trigger an upgrade.
    // null cursor factory means use the default SQLite cursor implementation.
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // Called only once, the first time the app opens and no database file exists on the device yet.
    // Executes a single CREATE TABLE statement that defines the full schema:
    // - id: auto-incrementing integer, serves as the unique row identifier
    // - name, category: required text fields
    // - amount: a decimal number (REAL = floating point in SQLite)
    // - created_at: stored as Unix epoch milliseconds so date-range filter queries
    //   are simple integer comparisons (>= fromMs) with no date parsing needed
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE " + TABLE_EXPENSES + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_NAME + " TEXT NOT NULL, " +
            COL_AMOUNT + " REAL NOT NULL, " +
            COL_CATEGORY + " TEXT NOT NULL, " +
            COL_CREATED_AT + " INTEGER NOT NULL)"
        );
    }

    // Called when DB_VERSION is bumped in code, signalling a schema change.
    // Drops the existing table entirely and recreates it via onCreate.
    // This destroys all stored data, which is acceptable for a dev-stage app
    // where a full migration strategy is not yet needed.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        onCreate(db);
    }
}
