package com.expensemanager.app.data.model;

public class HomeSummary {
    public double totalBalance;
    public double monthIncome;
    public double monthExpense;
    public double todayExpense;
    public String topCategoryName;
    public double topCategoryAmount;

    public HomeSummary(double totalBalance, double monthIncome, double monthExpense,
                       double todayExpense, String topCategoryName, double topCategoryAmount) {
        this.totalBalance = totalBalance;
        this.monthIncome = monthIncome;
        this.monthExpense = monthExpense;
        this.todayExpense = todayExpense;
        this.topCategoryName = topCategoryName;
        this.topCategoryAmount = topCategoryAmount;
    }
}
