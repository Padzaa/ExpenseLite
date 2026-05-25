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

    // Inflates the layout, creates the DAO, runs the setup helpers in order, then wires
    // the save button to onSaveClicked. Setup is split into helpers so onCreate stays readable.
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

    // Registers the MaterialToolbar as the action bar and enables the back arrow.
    // The null check on getSupportActionBar() is defensive — it should never be null here,
    // but the API returns nullable so the check keeps the compiler happy.
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_add_expense);
        }
    }

    // Finds all form-field views by ID and stores them in fields so other methods can
    // access them directly without repeated findViewById calls.
    private void bindViews() {
        layoutName = findViewById(R.id.layout_name);
        layoutAmount = findViewById(R.id.layout_amount);
        layoutCategory = findViewById(R.id.layout_category);
        editName = findViewById(R.id.edit_name);
        editAmount = findViewById(R.id.edit_amount);
        dropdownCategory = findViewById(R.id.dropdown_category);
    }

    // Wraps the CATEGORIES string array in an ArrayAdapter using Android's built-in
    // single-line dropdown item layout, then attaches it to the MaterialAutoCompleteTextView.
    // This makes the field show a selectable dropdown list when the user taps it.
    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        dropdownCategory.setAdapter(adapter);
    }

    // Runs validation first and exits immediately if anything is invalid. Then reads the
    // trimmed text from each field, parses the amount string to a double (safe here because
    // validateInputs already confirmed it is a valid positive number), constructs an Expense,
    // and saves it to the database. Sets the activity result to RESULT_OK so the calling
    // fragment's ActivityResultLauncher callback knows an expense was actually saved
    // (as opposed to the user pressing back), then closes the activity.
    private void onSaveClicked() {
        if (!validateInputs()) return;

        String name = editName.getText().toString().trim();
        // parseDouble is safe here because validateInputs() already confirmed the string is a valid positive number.
        double amount = Double.parseDouble(editAmount.getText().toString().trim());
        String category = dropdownCategory.getText().toString().trim();

        expenseDao.addExpense(new Expense(name, amount, category));
        // Signal to the caller (ExpenseListFragment via ActivityResultLauncher) that an expense was saved.
        setResult(RESULT_OK);
        finish();
    }

    // Checks all three fields in sequence without returning early on the first failure,
    // so all errors are shown at once rather than one at a time. For each field: if invalid,
    // calls setError() on the wrapping TextInputLayout which renders a red message below
    // the field and marks valid = false; if valid, clears any previous error with null.
    // Returns the accumulated valid flag at the end.
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

    // Attempts to parse the string as a double and checks it is greater than zero.
    // If parseDouble throws NumberFormatException (non-numeric input such as "abc"),
    // the catch block returns false. This combines both failure cases — unparseable
    // and non-positive — into a single false return value.
    private boolean isValidAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Called when any toolbar menu item is tapped. Checks if the item is the back arrow
    // (android.R.id.home) and calls finish() to close this activity. Without this override
    // the up button would follow Android's default behavior and navigate to MainActivity's
    // parent in the task stack, which is incorrect here — we just want to go back.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Called when the activity is fully destroyed. Closes the DAO to release
    // the SQLite connection; not doing this would leak the connection.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        expenseDao.close();
    }
}
