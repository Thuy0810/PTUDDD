package com.expensemanager.app.util;

import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Kiểm tra cảnh báo ngân sách (legacy wrapper).
 *
 * <p>Logic chính đã được tách vào
 * {@link com.expensemanager.app.domain.usecase.BudgetService#checkAlerts(Budget, List)}.
 * Class này giữ lại để tương thích với code cũ, đồng thời hỗ trợ {@code long}
 * thay vì {@code double} (ràng buộc 5.1).
 */
public final class BudgetChecker {
    private BudgetChecker() {}

    public static List<String> checkAlerts(List<Budget> budgets, List<Transaction> monthExpenses) {
        List<String> alerts = new ArrayList<>();
        if (budgets == null || monthExpenses == null) return alerts;
        for (Budget b : budgets) {
            alerts.addAll(
                    com.expensemanager.app.domain.usecase.BudgetService.checkAlerts(b, monthExpenses));
        }
        return alerts;
    }
}
