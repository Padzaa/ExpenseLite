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

    // Stores the initial expense list and the delete callback for use throughout the adapter's lifetime.
    public ExpenseAdapter(List<Expense> expenses, OnDeleteListener deleteListener) {
        this.expenses = expenses;
        this.deleteListener = deleteListener;
    }

    // Replaces the current list reference with a new one and calls notifyDataSetChanged(),
    // which tells the RecyclerView that every item may have changed and to rebind all
    // currently visible rows. Fine for small lists; for larger datasets consider DiffUtil
    // to compute and dispatch only the minimal set of changes.
    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    // Called by the RecyclerView when it needs a new row view that is not available in the
    // recycle pool. Inflates item_expense.xml into a View — the false argument means "don't
    // attach to parent yet" since the RecyclerView manages that itself — wraps it in a
    // ViewHolder, and returns it.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    // Called every time a row becomes visible, whether freshly created or recycled from a
    // row that scrolled off-screen. Gets the Expense at the given position, populates the
    // three text views, and attaches a long-click listener that invokes the delete callback.
    // Returning true from the long-click listener marks the event as consumed so it does
    // not also trigger any regular click handler on the same item.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.textName.setText(expense.getName());
        holder.textCategory.setText(expense.getCategory());
        holder.textAmount.setText(String.format("%.2f €", expense.getAmount()));
        // Long-press triggers delete. Returning true marks the event as consumed so it
        // doesn't also fire a regular click on the item.
        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onDelete(expense);
            return true;
        });
    }

    // Returns the total number of items so the RecyclerView knows how many rows to create.
    @Override
    public int getItemCount() {
        return expenses.size();
    }

    // Holds direct references to the three TextViews inside one row view. The RecyclerView
    // pattern requires this so onBindViewHolder can update text by accessing these fields
    // directly, instead of calling findViewById on every bind which would be slow.
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
