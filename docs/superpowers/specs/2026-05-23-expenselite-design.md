# ExpenseLite — Design Spec
**Date:** 2026-05-23  
**Platform:** Android (Java, minSdk 30, compileSdk 36)  
**Package:** `com.example.expenselite`

---

## Overview

ExpenseLite is a single-user, local-only personal expense tracking app. Users can add expenses with a name, amount, and category, filter their view by day/week/month, see the total for the selected period, and visualize spending by category via a pie chart. No login or registration.

---

## Architecture

**Approach:** Flat package (no sub-packages), raw SQLite via `SQLiteOpenHelper`, one dedicated CRUD class.

---

## Class Inventory

### Activities
| Class | Layout | Purpose |
|---|---|---|
| `MainActivity` | `activity_main.xml` | Hosts BottomNavigationView + FragmentContainerView |
| `AddExpenseActivity` | `activity_add_expense.xml` | Add expense form |

### Fragments
| Class | Layout | Purpose |
|---|---|---|
| `ExpenseListFragment` | `fragment_expense_list.xml` | Expense list, total sum, period filter |
| `ChartFragment` | `fragment_chart.xml` | Pie chart of spending by category, period filter |

### Supporting Classes
| Class | Purpose |
|---|---|
| `Expense` | POJO model — `id`, `name`, `amount`, `category`, `createdAt` |
| `DatabaseHelper` | Extends `SQLiteOpenHelper` — DB creation and schema upgrades |
| `ExpenseDao` | All CRUD operations (see below) |
| `ExpenseAdapter` | `RecyclerView.Adapter` for the expense list rows |

---

## Database Schema

**Database name:** `expense_lite.db`  
**Version:** 1

### Table: `expenses`

| Column | Type | Constraint |
|---|---|---|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` |
| `name` | `TEXT` | `NOT NULL` |
| `amount` | `REAL` | `NOT NULL` |
| `category` | `TEXT` | `NOT NULL` |
| `created_at` | `INTEGER` | `NOT NULL` — Unix timestamp in ms |

Naming convention: `snake_case` columns, plural table name.

---

## ExpenseDao — CRUD Methods

| Method | Signature | Description |
|---|---|---|
| Add | `long addExpense(Expense e)` | Inserts a new expense row; returns the new row ID |
| Read (filtered) | `List<Expense> getExpenses(long fromMs)` | Returns all expenses where `created_at >= fromMs` |
| Delete | `void deleteExpense(int id)` | Deletes expense by primary key |
| Total | `double getTotalAmount(long fromMs)` | SQL `SUM(amount)` for the period |
| Chart data | `Map<String, Double> getExpensesForChart(long fromMs)` | SQL `SUM(amount) GROUP BY category` for pie chart |

Period filtering: caller computes start-of-day/week/month as Unix ms and passes it as `fromMs`.

---

## Screen Designs

### MainActivity
- `BottomNavigationView` with two items:
  - **Expenses** (list icon) → `ExpenseListFragment`
  - **Chart** (pie icon) → `ChartFragment`
- `FragmentContainerView` fills the space above the nav bar
- Fragment swap on tab selection; fragments are re-used (not recreated)

### ExpenseListFragment
- `TextView` — total sum label: "Total: X.XX" at the top, updates when filter changes or list refreshes
- `MaterialButtonToggleGroup` — three single-select toggle buttons: **Day | Week | Month**; default selection: Day
- `RecyclerView` — list of expenses for the selected period
- Empty state `TextView` — "No expenses for this period" — visible only when list is empty
- `FloatingActionButton` — bottom-right; launches `AddExpenseActivity` via `startActivityForResult`; refreshes list on `RESULT_OK`

### AddExpenseActivity
- `TextInputLayout` + `TextInputEditText` — expense name (text input)
- `TextInputLayout` + `TextInputEditText` — amount (inputType = numberDecimal)
- `TextInputLayout` + `AutoCompleteTextView` — category (Material exposed dropdown); options: **Food, Hygiene, Transport, Entertainment**
- Save button — validates all fields are non-empty before calling `ExpenseDao.addExpense()`; shows `TextInputLayout` error on invalid fields
- On success: `setResult(RESULT_OK)`, `finish()`

### ChartFragment
- Same `MaterialButtonToggleGroup` (Day | Week | Month) — independent filter state from `ExpenseListFragment`
- `PieChart` (MPAndroidChart) — one slice per category with amount label; legend below
- "No data for this period" `TextView` — shown when all sums are zero

### RecyclerView Row (item_expense.xml)
- Left side: expense `name` (primary text) + `category` (secondary text, smaller)
- Right side: `amount` formatted to 2 decimal places
- Long press: shows `AlertDialog` "Delete this expense?" with Confirm/Cancel; calls `ExpenseDao.deleteExpense()` on confirm and notifies adapter

---

## Categories

Fixed list (no user-defined categories):
- Food
- Hygiene
- Transport
- Entertainment

Stored as plain strings in the `category` column.

---

## Period Filter Logic

| Filter | `fromMs` value |
|---|---|
| Day | Start of today: midnight of current date in device timezone |
| Week | Start of current ISO week (Monday) |
| Month | First day of current month, midnight |

Computed via `java.util.Calendar` — no external date library needed.

---

## Input Validation Rules

| Rule | Feedback |
|---|---|
| Name must not be blank | `TextInputLayout` error: "Enter a name" |
| Amount must not be blank | `TextInputLayout` error: "Enter an amount" |
| Amount must be a valid positive number | `TextInputLayout` error: "Enter a valid amount" |
| Category must be selected | `TextInputLayout` error: "Select a category" |

No Toast for validation errors — inline field errors only.

---

## Dependencies to Add

| Library | Purpose |
|---|---|
| `com.github.PhilJay:MPAndroidChart:v3.1.0` | Pie chart in ChartFragment |

JitPack repository must be added to `settings.gradle` (or root `build.gradle` depending on Gradle version).

Existing deps (AppCompat, Material, ConstraintLayout, Activity) cover all other needs — no additional libraries required.

---

## Empty States

| Screen | Condition | Message |
|---|---|---|
| ExpenseListFragment | No expenses in period | "No expenses for this period" |
| ChartFragment | No expenses in period | "No data for this period" |

---

## Out of Scope

- Login / registration / multiple user accounts
- Editing existing expenses (only add and delete)
- Export / backup
- Currency selection
- Notifications or reminders
- Cloud sync
