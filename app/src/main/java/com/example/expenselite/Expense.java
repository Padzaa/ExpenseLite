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
