package com.example.expenselite;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddExpenseActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {"Food", "Hygiene", "Transport", "Entertainment"};

    private TextInputLayout layoutName, layoutAmount, layoutCategory;
    private TextInputEditText editName, editAmount;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView dropdownCategory;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        expenseDao.close();
    }
}
