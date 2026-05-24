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
        holder.textAmount.setText(String.format("%.2f €", expense.getAmount()));
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
