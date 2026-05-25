package com.example.expenselite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpenseDao {

    private final DatabaseHelper dbHelper;

    // Creates a DatabaseHelper which registers the database name and version with the OS,
    // but does not open the file yet — the actual connection is opened lazily on the
    // first getReadableDatabase() or getWritableDatabase() call.
    public ExpenseDao(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // Opens the database in write mode, packs each field of the expense into a
    // ContentValues map (Android's typed key-value container for SQL parameters),
    // then calls insert(). Returns the new row's auto-assigned ID, or -1 on failure.
    public long addExpense(Expense expense) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_NAME, expense.getName());
        values.put(DatabaseHelper.COL_AMOUNT, expense.getAmount());
        values.put(DatabaseHelper.COL_CATEGORY, expense.getCategory());
        values.put(DatabaseHelper.COL_CREATED_AT, expense.getCreatedAt());
        return db.insert(DatabaseHelper.TABLE_EXPENSES, null, values);
    }

    // Queries all rows where created_at >= fromMs, ordered newest-first.
    // The ? placeholder is used instead of string concatenation to prevent SQL injection.
    // Iterates the Cursor row by row, converts each to an Expense via cursorToExpense,
    // then closes the cursor before returning the populated list.
    public List<Expense> getExpenses(long fromMs) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String where = DatabaseHelper.COL_CREATED_AT + " >= ?";
        String[] args = {String.valueOf(fromMs)};
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_EXPENSES, null, where, args,
            null, null, DatabaseHelper.COL_CREATED_AT + " DESC"
        );
        while (cursor.moveToNext()) {
            list.add(cursorToExpense(cursor));
        }
        cursor.close();
        return list;
    }

    // Opens in write mode and deletes exactly the row whose id matches.
    // The ? placeholder ensures only that one specific row is affected.
    public void deleteExpense(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_EXPENSES,
            DatabaseHelper.COL_ID + " = ?",
            new String[]{String.valueOf(id)});
    }

    // Runs SELECT SUM(amount) with a WHERE created_at >= ? filter.
    // SUM always returns exactly one row, but when no rows match the filter
    // that row contains NULL rather than 0. The cursor.isNull(0) guard prevents
    // reading a NULL column as 0.0, which would be a silent incorrect result.
    public double getTotalAmount(long fromMs) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + DatabaseHelper.COL_AMOUNT + ")" +
            " FROM " + DatabaseHelper.TABLE_EXPENSES +
            " WHERE " + DatabaseHelper.COL_CREATED_AT + " >= ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(fromMs)});
        double total = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    // Runs a GROUP BY category query that collapses all expenses in the time window
    // into per-category totals. Returns a LinkedHashMap so the insertion order of
    // entries is stable across calls — this matters because the pie chart and its
    // legend are both built by iterating this map in the same order, so the colors
    // must line up between the slices and the legend labels.
    public Map<String, Double> getExpensesForChart(long fromMs) {
        Map<String, Double> map = new LinkedHashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query =
            "SELECT " + DatabaseHelper.COL_CATEGORY +
            ", SUM(" + DatabaseHelper.COL_AMOUNT + ")" +
            " FROM " + DatabaseHelper.TABLE_EXPENSES +
            " WHERE " + DatabaseHelper.COL_CREATED_AT + " >= ?" +
            " GROUP BY " + DatabaseHelper.COL_CATEGORY;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(fromMs)});
        while (cursor.moveToNext()) {
            map.put(cursor.getString(0), cursor.getDouble(1));
        }
        cursor.close();
        return map;
    }

    // Closes the underlying SQLite connection. Must be called in onDestroyView / onDestroy
    // of whatever component owns this DAO, otherwise the connection leaks.
    public void close() {
        dbHelper.close();
    }

    // Reads one database row from the current cursor position into an Expense object.
    // getColumnIndexOrThrow is used instead of getColumnIndex so that any column-name
    // mismatch (e.g. a typo) throws an exception immediately with a clear message,
    // rather than silently returning -1 and reading the wrong column's data.
    private Expense cursorToExpense(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
        double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT));
        String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY));
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT));
        return new Expense(id, name, amount, category, createdAt);
    }
}
