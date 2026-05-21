package com.expensemanager.app.util;

import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public final class BudgetChecker {
    private BudgetChecker() {}

    public static List<String> checkAlerts(List<Budget> budgets, List<Transaction> monthExpenses) {
        List<String> alerts = new ArrayList<>();
        for (Budget b : budgets) {
            double spent = 0;
            if (Budget.SCOPE_CATEGORY.equals(b.getScope())) {
                for (Transaction t : monthExpenses) {
                    if (Transaction.TYPE_EXPENSE.equals(t.getType())
                            && b.getCategoryId() != null
                            && b.getCategoryId().equals(t.getCategoryId())) {
                        spent += t.getAmount();
                    }
                }
            } else {
                for (Transaction t : monthExpenses) {
                    if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                        spent += t.getAmount();
                    }
                }
            }
            if (b.getLimitAmount() <= 0) continue;
            double pct = spent / b.getLimitAmount();
            for (Double threshold : b.getAlertAt()) {
                if (pct >= threshold && pct < 1) {
                    alerts.add(String.format("Đã dùng %.0f%% ngân sách (còn %s)",
                            pct * 100, MoneyFormat.format(b.getLimitAmount() - spent)));
                    break;
                }
            }
            if (pct >= 1) {
                alerts.add("Đã vượt ngân sách!");
            }
        }
        return alerts;
    }
}
