package com.expensemanager.app.data.model;

/**
 * Loại cảnh báo sức khỏe tài chính — UI tự hiện thị theo ngôn ngữ.
 */
public enum FinancialAlertType {
    OVER_BUDGET,
    NEAR_BUDGET_LIMIT,
    EXPENSE_INCREASED,
    MISSING_RECORDS,
    ABNORMAL_SPENDING,
    /** Theo tốc độ chi hiện tại, số dư sẽ hết trước cuối tháng. */
    CASH_RUNOUT_RISK,
}
