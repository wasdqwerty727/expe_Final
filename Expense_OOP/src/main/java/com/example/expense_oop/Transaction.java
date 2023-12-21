package com.example.expense_oop;

import java.time.LocalDate;

public class Transaction {

    private String category;
    private double amount;
    private String type;
    private LocalDate date;
    private String currency;

    public Transaction(String category, double amount, String type, LocalDate date, String currency) {
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.date = date;
        this.currency = currency;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCurrency() {
        return currency;
    }
}
