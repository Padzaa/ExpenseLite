# ExpenseLite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-user Android expense tracking app with a list view, pie chart, local SQLite storage, and day/week/month filtering.

**Architecture:** Two activities (MainActivity, AddExpenseActivity), two fragments (ExpenseListFragment, ChartFragment), raw SQLite via DatabaseHelper + ExpenseDao, RecyclerView adapter. All classes in `com.example.expenselite` (flat package). All methods kept short and single-purpose.

**Tech Stack:** Java, Android minSdk 30, raw SQLite, MPAndroidChart v3.1.0, Material3 (1.13.0), RecyclerView 1.4.0, androidx.activity 1.13.0

---

## File Map

| File | Action | Purpose |
|---|---|---|
| `settings.gradle` | Modify | Add JitPack repository |
| `app/build.gradle` | Modify | Add MPAndroidChart + RecyclerView deps |
| `app/src/main/java/.../Expense.java` | Create | POJO model |
| `app/src/main/java/.../DatabaseHelper.java` | Create | SQLiteOpenHelper — creates/upgrades DB |
| `app/src/main/java/.../ExpenseDao.java` | Create | All CRUD methods |
| `app/src/main/java/.../ExpenseAdapter.java` | Create | RecyclerView adapter |
| `app/src/main/java/.../AddExpenseActivity.java` | Create | Add expense form |
| `app/src/main/java/.../ExpenseListFragment.java` | Create | List + total + filter |
| `app/src/main/java/.../ChartFragment.java` | Create | Pie chart + filter |
| `app/src/main/java/.../MainActivity.java` | Modify | Bottom nav + fragment host |
| `app/src/main/res/values/strings.xml` | Modify | All string resources |
| `app/src/main/res/menu/bottom_nav_menu.xml` | Create | Bottom nav items |
| `app/src/main/res/layout/activity_main.xml` | Modify | Toolbar + FragmentContainer + BottomNav |
| `app/src/main/res/layout/activity_add_expense.xml` | Create | Add expense form layout |
| `app/src/main/res/layout/fragment_expense_list.xml` | Create | List fragment layout |
| `app/src/main/res/layout/fragment_chart.xml` | Create | Chart fragment layout |
| `app/src/main/res/layout/item_expense.xml` | Create | RecyclerView row layout |
| `app/src/main/AndroidManifest.xml` | Modify | Register AddExpenseActivity |

---

## Task 1: Add Dependencies

**Files:**
- Modify: `settings.gradle`
- Modify: `app/build.gradle`

- [ ] **Step 1: Add JitPack to settings.gradle**

```groovy
// settings.gradle — add maven { url 'https://jitpack.io' } inside the repositories block
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

- [ ] **Step 2: Add MPAndroidChart and RecyclerView to app/build.gradle**

Add these two lines inside the `dependencies { }` block, after the existing entries:

```groovy
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
implementation 'androidx.recyclerview:recyclerview:1.4.0'
```

- [ ] **Step 3: Sync and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (no unresolved dependency errors)

---

## Task 2: Expense POJO

**Files:**
- Create: `app/src/main/java/com/example/expenselite/Expense.java`

- [ ] **Step 1: Create Expense.java**

```java
package com.example.expenselite;

public class Expense {
    private int id;
    private String name;
    private double amount;
    private String category;
    private long createdAt;

    // Used when adding a new expense (DB assigns the id)
    public Expense(String name, double amount, String category) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.createdAt = System.currentTimeMillis();
    }

    // Used when reading from DB (id and createdAt are known)
    public Expense(int id, String name, double amount, String category, long createdAt) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public long getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 3: DatabaseHelper

**Files:**
- Create: `app/src/main/java/com/example/expenselite/DatabaseHelper.java`

- [ ] **Step 1: Create DatabaseHelper.java**

```java
package com.example.expenselite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    static final String DB_NAME = "expense_lite.db";
    static final int DB_VERSION = 1;

    static final String TABLE_EXPENSES = "expenses";
    static final String COL_ID = "id";
    static final String COL_NAME = "name";
    static final String COL_AMOUNT = "amount";
    static final String COL_CATEGORY = "category";
    static final String COL_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

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

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        onCreate(db);
    }
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 4: ExpenseDao

**Files:**
- Create: `app/src/main/java/com/example/expenselite/ExpenseDao.java`

- [ ] **Step 1: Create ExpenseDao.java**

```java
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
        long id = db.insert(DatabaseHelper.TABLE_EXPENSES, null, values);
        db.close();
        return id;
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
        db.close();
        return list;
    }

    public void deleteExpense(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_EXPENSES,
            DatabaseHelper.COL_ID + " = ?",
            new String[]{String.valueOf(id)});
        db.close();
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
        db.close();
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
        db.close();
        return map;
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
```

- [ ] **Step 2: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 5: String Resources and Bottom Nav Menu

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/menu/bottom_nav_menu.xml`

- [ ] **Step 1: Replace strings.xml content**

```xml
<resources>
    <string name="app_name">ExpenseLite</string>

    <!-- AddExpenseActivity -->
    <string name="title_add_expense">Add Expense</string>
    <string name="hint_name">Expense name</string>
    <string name="hint_amount">Amount</string>
    <string name="hint_category">Category</string>
    <string name="btn_save">Save</string>
    <string name="error_name">Enter a name</string>
    <string name="error_amount_empty">Enter an amount</string>
    <string name="error_amount_invalid">Enter a valid positive amount</string>
    <string name="error_category">Select a category</string>

    <!-- ExpenseListFragment -->
    <string name="total_default">Total: 0.00</string>
    <string name="filter_day">Day</string>
    <string name="filter_week">Week</string>
    <string name="filter_month">Month</string>
    <string name="empty_expenses">No expenses for this period</string>
    <string name="fab_add_desc">Add expense</string>

    <!-- Delete dialog -->
    <string name="delete_title">Delete Expense</string>
    <string name="delete_message">Delete this expense?</string>
    <string name="btn_confirm">Delete</string>
    <string name="btn_cancel">Cancel</string>

    <!-- ChartFragment -->
    <string name="no_chart_data">No data for this period</string>

    <!-- Bottom navigation -->
    <string name="nav_expenses">Expenses</string>
    <string name="nav_chart">Chart</string>
</resources>
```

- [ ] **Step 2: Create res/menu/ directory and bottom_nav_menu.xml**

Create the file at `app/src/main/res/menu/bottom_nav_menu.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_expenses"
        android:icon="@android:drawable/ic_menu_agenda"
        android:title="@string/nav_expenses" />
    <item
        android:id="@+id/nav_chart"
        android:icon="@android:drawable/ic_menu_report_image"
        android:title="@string/nav_chart" />
</menu>
```

- [ ] **Step 3: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 6: XML Layouts

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/layout/activity_add_expense.xml`
- Create: `app/src/main/res/layout/fragment_expense_list.xml`
- Create: `app/src/main/res/layout/fragment_chart.xml`
- Create: `app/src/main/res/layout/item_expense.xml`

- [ ] **Step 1: Replace activity_main.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        app:title="@string/app_name" />

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/fragment_container"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:menu="@menu/bottom_nav_menu" />

</LinearLayout>
```

- [ ] **Step 2: Create activity_add_expense.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/layout_name"
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/hint_name">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/edit_name"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="text" />

        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/layout_amount"
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="@string/hint_amount">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/edit_amount"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="numberDecimal" />

        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/layout_category"
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="@string/hint_category">

            <AutoCompleteTextView
                android:id="@+id/dropdown_category"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:focusable="false"
                android:focusableInTouchMode="false" />

        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_save"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="@string/btn_save" />

    </LinearLayout>

</LinearLayout>
```

- [ ] **Step 3: Create fragment_expense_list.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp"
        android:paddingBottom="88dp">

        <TextView
            android:id="@+id/text_total"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="20sp"
            android:textStyle="bold"
            android:paddingBottom="12dp"
            android:text="@string/total_default" />

        <com.google.android.material.button.MaterialButtonToggleGroup
            android:id="@+id/toggle_filter"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp"
            app:singleSelection="true"
            app:selectionRequired="true">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_day"
                style="@style/Widget.Material3.Button.OutlinedButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/filter_day" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_week"
                style="@style/Widget.Material3.Button.OutlinedButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/filter_week" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_month"
                style="@style/Widget.Material3.Button.OutlinedButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/filter_month" />

        </com.google.android.material.button.MaterialButtonToggleGroup>

        <TextView
            android:id="@+id/text_empty"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/empty_expenses"
            android:visibility="gone"
            android:gravity="center"
            android:paddingTop="40dp" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_expenses"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

    </LinearLayout>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab_add"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:contentDescription="@string/fab_add_desc"
        app:srcCompat="@android:drawable/ic_input_add" />

</FrameLayout>
```

- [ ] **Step 4: Create fragment_chart.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <com.google.android.material.button.MaterialButtonToggleGroup
        android:id="@+id/toggle_filter"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="12dp"
        app:singleSelection="true"
        app:selectionRequired="true">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_day"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/filter_day" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_week"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/filter_week" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_month"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/filter_month" />

    </com.google.android.material.button.MaterialButtonToggleGroup>

    <TextView
        android:id="@+id/text_no_data"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/no_chart_data"
        android:visibility="gone"
        android:gravity="center"
        android:paddingTop="40dp" />

    <com.github.mikephil.charting.charts.PieChart
        android:id="@+id/pie_chart"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

</LinearLayout>
```

- [ ] **Step 5: Create item_expense.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="12dp"
    android:background="?attr/selectableItemBackground">

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/text_expense_name"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="16sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/text_expense_category"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="13sp" />

    </LinearLayout>

    <TextView
        android:id="@+id/text_expense_amount"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="16sp"
        android:layout_gravity="center_vertical" />

</LinearLayout>
```

- [ ] **Step 6: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 7: ExpenseAdapter

**Files:**
- Create: `app/src/main/java/com/example/expenselite/ExpenseAdapter.java`

- [ ] **Step 1: Create ExpenseAdapter.java**

```java
package com.example.expenselite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(Expense expense);
    }

    private List<Expense> expenses;
    private final OnDeleteListener deleteListener;

    public ExpenseAdapter(List<Expense> expenses, OnDeleteListener deleteListener) {
        this.expenses = expenses;
        this.deleteListener = deleteListener;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.textName.setText(expense.getName());
        holder.textCategory.setText(expense.getCategory());
        holder.textAmount.setText(String.format("%.2f", expense.getAmount()));
        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onDelete(expense);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textCategory, textAmount;

        ViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_expense_name);
            textCategory = itemView.findViewById(R.id.text_expense_category);
            textAmount = itemView.findViewById(R.id.text_expense_amount);
        }
    }
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 8: AddExpenseActivity

**Files:**
- Create: `app/src/main/java/com/example/expenselite/AddExpenseActivity.java`

- [ ] **Step 1: Create AddExpenseActivity.java**

```java
package com.example.expenselite;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddExpenseActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {"Food", "Hygiene", "Transport", "Entertainment"};

    private TextInputLayout layoutName, layoutAmount, layoutCategory;
    private TextInputEditText editName, editAmount;
    private AutoCompleteTextView dropdownCategory;
    private ExpenseDao expenseDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        expenseDao = new ExpenseDao(this);
        setupToolbar();
        bindViews();
        setupCategoryDropdown();

        findViewById(R.id.btn_save).setOnClickListener(v -> onSaveClicked());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_add_expense);
        }
    }

    private void bindViews() {
        layoutName = findViewById(R.id.layout_name);
        layoutAmount = findViewById(R.id.layout_amount);
        layoutCategory = findViewById(R.id.layout_category);
        editName = findViewById(R.id.edit_name);
        editAmount = findViewById(R.id.edit_amount);
        dropdownCategory = findViewById(R.id.dropdown_category);
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        dropdownCategory.setAdapter(adapter);
    }

    private void onSaveClicked() {
        if (!validateInputs()) return;

        String name = editName.getText().toString().trim();
        double amount = Double.parseDouble(editAmount.getText().toString().trim());
        String category = dropdownCategory.getText().toString().trim();

        expenseDao.addExpense(new Expense(name, amount, category));
        setResult(RESULT_OK);
        finish();
    }

    private boolean validateInputs() {
        boolean valid = true;

        String name = editName.getText().toString().trim();
        if (name.isEmpty()) {
            layoutName.setError(getString(R.string.error_name));
            valid = false;
        } else {
            layoutName.setError(null);
        }

        String amountStr = editAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            layoutAmount.setError(getString(R.string.error_amount_empty));
            valid = false;
        } else if (!isValidAmount(amountStr)) {
            layoutAmount.setError(getString(R.string.error_amount_invalid));
            valid = false;
        } else {
            layoutAmount.setError(null);
        }

        String category = dropdownCategory.getText().toString().trim();
        if (category.isEmpty()) {
            layoutCategory.setError(getString(R.string.error_category));
            valid = false;
        } else {
            layoutCategory.setError(null);
        }

        return valid;
    }

    private boolean isValidAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 9: ExpenseListFragment

**Files:**
- Create: `app/src/main/java/com/example/expenselite/ExpenseListFragment.java`

- [ ] **Step 1: Create ExpenseListFragment.java**

```java
package com.example.expenselite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExpenseListFragment extends Fragment {

    private ExpenseDao expenseDao;
    private ExpenseAdapter adapter;
    private TextView textTotal;
    private TextView textEmpty;
    private RecyclerView recyclerView;
    private long currentFromMs;

    private final ActivityResultLauncher<Intent> addExpenseLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                loadExpenses();
            }
        });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        expenseDao = new ExpenseDao(requireContext());
        textTotal = view.findViewById(R.id.text_total);
        textEmpty = view.findViewById(R.id.text_empty);
        recyclerView = view.findViewById(R.id.recycler_expenses);

        setupRecyclerView();
        setupFilterToggle(view);
        setupFab(view);

        currentFromMs = getStartOfDay();
        loadExpenses();
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(new ArrayList<>(), this::showDeleteDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_filter);
        toggle.check(R.id.btn_day);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) onFilterSelected(checkedId);
        });
    }

    private void setupFab(View view) {
        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddExpenseActivity.class);
            addExpenseLauncher.launch(intent);
        });
    }

    private void onFilterSelected(int buttonId) {
        if (buttonId == R.id.btn_day) currentFromMs = getStartOfDay();
        else if (buttonId == R.id.btn_week) currentFromMs = getStartOfWeek();
        else currentFromMs = getStartOfMonth();
        loadExpenses();
    }

    private void loadExpenses() {
        List<Expense> expenses = expenseDao.getExpenses(currentFromMs);
        adapter.setExpenses(expenses);
        boolean empty = expenses.isEmpty();
        textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        updateTotal();
    }

    private void updateTotal() {
        double total = expenseDao.getTotalAmount(currentFromMs);
        textTotal.setText(String.format("Total: %.2f", total));
    }

    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setPositiveButton(R.string.btn_confirm, (d, w) -> deleteExpense(expense))
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    private void deleteExpense(Expense expense) {
        expenseDao.deleteExpense(expense.getId());
        loadExpenses();
    }

    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        // Calculate days since Monday (works for any locale)
        int daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getStartOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 10: ChartFragment

**Files:**
- Create: `app/src/main/java/com/example/expenselite/ChartFragment.java`

- [ ] **Step 1: Create ChartFragment.java**

```java
package com.example.expenselite;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class ChartFragment extends Fragment {

    private ExpenseDao expenseDao;
    private PieChart pieChart;
    private TextView textNoData;
    private long currentFromMs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        expenseDao = new ExpenseDao(requireContext());
        pieChart = view.findViewById(R.id.pie_chart);
        textNoData = view.findViewById(R.id.text_no_data);

        setupFilterToggle(view);

        currentFromMs = getStartOfDay();
        loadChart();
    }

    private void setupFilterToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_filter);
        toggle.check(R.id.btn_day);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) onFilterSelected(checkedId);
        });
    }

    private void onFilterSelected(int buttonId) {
        if (buttonId == R.id.btn_day) currentFromMs = getStartOfDay();
        else if (buttonId == R.id.btn_week) currentFromMs = getStartOfWeek();
        else currentFromMs = getStartOfMonth();
        loadChart();
    }

    private void loadChart() {
        Map<String, Double> data = expenseDao.getExpensesForChart(currentFromMs);
        if (data.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            textNoData.setVisibility(View.VISIBLE);
            return;
        }
        textNoData.setVisibility(View.GONE);
        pieChart.setVisibility(View.VISIBLE);
        renderPieChart(data);
    }

    private void renderPieChart(Map<String, Double> data) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();
    }

    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        int daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getStartOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 11: MainActivity

**Files:**
- Modify: `app/src/main/java/com/example/expenselite/MainActivity.java`

- [ ] **Step 1: Replace MainActivity.java**

```java
package com.example.expenselite;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ExpenseListFragment expenseListFragment;
    private ChartFragment chartFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            initFragments();
        } else {
            restoreFragments();
        }

        setupBottomNav();
    }

    private void initFragments() {
        expenseListFragment = new ExpenseListFragment();
        chartFragment = new ChartFragment();

        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, expenseListFragment, "list")
            .add(R.id.fragment_container, chartFragment, "chart")
            .hide(chartFragment)
            .commit();
    }

    private void restoreFragments() {
        expenseListFragment = (ExpenseListFragment)
            getSupportFragmentManager().findFragmentByTag("list");
        chartFragment = (ChartFragment)
            getSupportFragmentManager().findFragmentByTag("chart");
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_expenses) {
                showFragment(expenseListFragment, chartFragment);
                return true;
            } else if (id == R.id.nav_chart) {
                showFragment(chartFragment, expenseListFragment);
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment show, Fragment hide) {
        getSupportFragmentManager().beginTransaction()
            .show(show)
            .hide(hide)
            .commit();
    }
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 12: AndroidManifest and Final Build

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Register AddExpenseActivity in AndroidManifest.xml**

Add the `AddExpenseActivity` entry inside `<application>`, after the existing `<activity>` block:

```xml
<activity
    android:name=".AddExpenseActivity"
    android:parentActivityName=".MainActivity"
    android:exported="false" />
```

Full manifest should look like:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ExpenseLite">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".AddExpenseActivity"
            android:parentActivityName=".MainActivity"
            android:exported="false" />

    </application>

</manifest>
```

- [ ] **Step 2: Final build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — APK at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Manual test checklist**

Install on device/emulator (`./gradlew installDebug`) and verify:

1. App launches showing Expenses tab with "Total: 0.00" and "No expenses for this period"
2. Tap FAB → AddExpense screen opens with back arrow
3. Try saving with empty fields → inline errors appear on each field
4. Fill in name, amount, select category from dropdown → tap Save → returns to list
5. Expense appears in list with name, category, amount
6. Total updates to reflect the added expense
7. Long press an expense → delete dialog appears → tap Delete → expense removed, total updates
8. Long press → tap Cancel → expense remains
9. Switch filter to Week → expense still visible (added today = this week)
10. Switch filter to Month → expense still visible
11. Tap Chart tab → pie chart shows a slice for the category used
12. Switch chart filter to a period with no data → "No data for this period" message appears
13. Rotate device → app state preserved (filter selection, visible fragment)
