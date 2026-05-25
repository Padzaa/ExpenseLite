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

    // Registered as a field (before onAttach) because the Activity Result API requires launchers
    // to be registered before the fragment is attached to the activity. The callback fires when
    // AddExpenseActivity finishes: if the result code is RESULT_OK (an expense was saved, not
    // just the back button pressed), the list is reloaded to show the new entry.
    private final ActivityResultLauncher<Intent> addExpenseLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                loadExpenses();
            }
        });

    // Inflates the XML layout for this fragment and returns the root view.
    // No logic here — view references are assigned in onViewCreated once the view tree is ready.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expense_list, container, false);
    }

    // Runs after the view is fully created. Creates the DAO, finds view references by ID,
    // then delegates wiring to three setup helpers. Sets the initial filter to today's
    // start-of-day timestamp and performs the first data load.
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

    // Creates the adapter with an empty list and wires showDeleteDialog as the long-press
    // delete callback. Attaches a LinearLayoutManager (vertical top-to-bottom scroll)
    // and sets the adapter on the RecyclerView.
    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(new ArrayList<>(), this::showDeleteDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    // Finds the toggle button group, programmatically selects Day as the initial state,
    // and registers a listener. The listener guards on isChecked because the event fires
    // once for the button being deselected and once for the new button being selected —
    // without the guard, loadExpenses would run twice per tap.
    private void setupFilterToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_filter);
        toggle.check(R.id.btn_day);
        // The listener fires for both check AND uncheck events; guard on isChecked so we only
        // react once per selection rather than twice (once for the old button, once for the new).
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) onFilterSelected(checkedId);
        });
    }

    // Attaches a click listener to the floating action button. When tapped, creates an Intent
    // for AddExpenseActivity and launches it through addExpenseLauncher (not startActivity)
    // so the result callback fires when that activity finishes.
    private void setupFab(View view) {
        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddExpenseActivity.class);
            addExpenseLauncher.launch(intent);
        });
    }

    // Maps the selected button's ID to a start-of-period timestamp, stores it in
    // currentFromMs, then reloads the expense list for that new time window.
    private void onFilterSelected(int buttonId) {
        if (buttonId == R.id.btn_day) currentFromMs = getStartOfDay();
        else if (buttonId == R.id.btn_week) currentFromMs = getStartOfWeek();
        else currentFromMs = getStartOfMonth();
        loadExpenses();
    }

    // Single entry point for refreshing both the list and the total after any data change.
    // Fetches expenses from the DAO for the current filter window, hands the list to the
    // adapter, and toggles visibility so exactly one of the empty-state label or the
    // RecyclerView is shown at any time. Then updates the total amount label.
    private void loadExpenses() {
        List<Expense> expenses = expenseDao.getExpenses(currentFromMs);
        adapter.setExpenses(expenses);
        boolean empty = expenses.isEmpty();
        // Toggle the empty-state placeholder and the list so exactly one is visible at a time.
        textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        updateTotal();
    }

    // Asks the DAO for the sum of all amounts in the current filter window and formats
    // it into the total text view using the total_format string resource (e.g. "Total: 42.00 €").
    private void updateTotal() {
        double total = expenseDao.getTotalAmount(currentFromMs);
        textTotal.setText(getString(R.string.total_format, total));
    }

    // Builds and shows a confirmation AlertDialog before deleting. The positive button
    // calls deleteExpense; the negative button passes null as its listener, which dismisses
    // the dialog with no action taken.
    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setPositiveButton(R.string.btn_confirm, (d, w) -> deleteExpense(expense))
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    // Tells the DAO to remove the row by its ID, then reloads the list and total
    // so the deleted item disappears and the total reflects the new state.
    private void deleteExpense(Expense expense) {
        expenseDao.deleteExpense(expense.getId());
        loadExpenses();
    }

    // Called when the fragment's view is being torn down. Closes the DAO to release
    // the SQLite connection; not doing this would leak the connection.
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        expenseDao.close();
    }

    // Takes the current moment, zeroes out the hours, minutes, seconds, and milliseconds,
    // and returns the result as a Unix millisecond timestamp — i.e. midnight of today.
    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Calculates the timestamp for midnight of the Monday that started the current week.
    // Calendar.DAY_OF_WEEK is 1 (Sun) through 7 (Sat). Adding 5 then taking mod 7 remaps
    // that to 0 (Mon) through 6 (Sun), giving the number of days since Monday regardless
    // of locale. Subtracting that count from today lands on Monday, which is then zeroed
    // to midnight.
    private long getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        // Calendar.DAY_OF_WEEK is 1 (Sun) … 7 (Sat). Adding 5 then taking mod 7 remaps that
        // to 0 (Mon) … 6 (Sun), giving locale-independent days-since-Monday.
        int daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Same concept as getStartOfDay but also sets DAY_OF_MONTH to 1,
    // landing on the first of the current month at midnight.
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
