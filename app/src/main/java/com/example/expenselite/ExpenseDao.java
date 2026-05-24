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

    public ExpenseDao(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public long addExpense(Expense expense) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_NAME, expense.getName());
        values.put(DatabaseHelper.COL_AMOUNT, expense.getAmount());
        values.put(DatabaseHelper.COL_CATEGORY, expense.getCategory());
        values.put(DatabaseHelper.COL_CREATED_AT, expense.getCreatedAt());
        return db.insert(DatabaseHelper.TABLE_EXPENSES, null, values);
    }

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

    public void deleteExpense(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_EXPENSES,
            DatabaseHelper.COL_ID + " = ?",
            new String[]{String.valueOf(id)});
    }

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

    public void close() {
        dbHelper.close();
    }

    private Expense cursorToExpense(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
        double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT));
        String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY));
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT));
        return new Expense(id, name, amount, category, createdAt);
    }
}
