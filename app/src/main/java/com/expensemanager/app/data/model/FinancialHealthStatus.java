package com.expensemanager.app.data.model;

/**
 * Trạng thái sức khỏe tài chính — UI tự hiện thị theo ngôn ngữ.
 */
public enum FinancialHealthStatus {
    EXCELLENT,   // 85–100
    GOOD,        // 70–84
    WARNING,     // 50–69
    CRITICAL;    //  0–49

    public static FinancialHealthStatus fromScore(int score) {
        if (score >= 85) return EXCELLENT;
        if (score >= 70) return GOOD;
        if (score >= 50) return WARNING;
        return CRITICAL;
    }
}
