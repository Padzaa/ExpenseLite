package com.example.expenselite;

public class Expense {
    private int id;
    private String name;
    private double amount;
    private String category;
    private long createdAt;

    // Used when the user creates a new expense. The id is left at 0 because the database
    // will assign a real one on insert. createdAt captures the exact moment of creation
    // as a Unix millisecond timestamp so it can be used in date-range filter queries.
    public Expense(String name, double amount, String category) {
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.createdAt = System.currentTimeMillis();
    }

    // Used when reading a row back from the database, where both id and createdAt
    // are already known values stored in the row.
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
