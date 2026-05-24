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
        textTotal.setText(getString(R.string.total_format, total));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        expenseDao.close();
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
        // Days since Monday — works for all locales
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
