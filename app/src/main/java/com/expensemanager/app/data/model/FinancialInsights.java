package com.expensemanager.app.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả phân tích sức khỏe tài chính — chỉ chứa dữ liệu số, không chứa câu tiếng Việt/Anh.
 *
 * <p>UI chịu trách nhiệm format tiền, phần trăm, chọn màu, chọn chuỗi theo ngôn ngữ.
 */
public class FinancialInsights {
    // --- Điểm sức khỏe ---
    public int healthScore;
    public FinancialHealthStatus status;

    // --- Dòng tiền ---
    public long incomeAmount;
    public long expenseAmount;
    /** Thu nhập - Chi tiêu */
    public long netCashFlow;

    // --- Tỷ lệ (0.0–1.0+) ---
    /** netCashFlow / incomeAmount. Double.NaN nếu incomeAmount <= 0. */
    public double savingRate;
    /** expenseAmount / budgetLimit. Double.NaN nếu chưa đặt ngân sách. */
    public double budgetUsageRate;
    /** (currentExpense - previousExpense) / previousExpense. Double.NaN nếu tháng trước = 0. */
    public double expenseChangeRate;

    // --- Các chỉ số dự đoán ---
    /** Số tiền nên tiêu tối đa mỗi ngày còn lại */
    public long dailyAllowanceAmount;
    /** Dự đoán tổng chi cuối tháng */
    public long predictedMonthExpense;

    // --- Top ---
    /** ID danh mục chi nhiều nhất tháng này */
    public String topExpenseCategoryId;

    // --- Dự báo dòng tiền (run-rate forecast) ---
    /** Chi tiêu trung bình mỗi ngày tính từ đầu tháng đến hôm nay. */
    public long avgDailyExpense;
    /** Tổng tài sản hiện tại (số dư tất cả ví) dùng cho dự báo. */
    public long balanceSnapshot;
    /**
     * Số ngày số dư hiện tại còn trụ được nếu giữ nguyên tốc độ chi.
     * {@code -1} nếu chưa đủ dữ liệu (chưa chi đồng nào).
     */
    public int daysBalanceLasts = -1;
    /** {@code true} nếu dự báo hết tiền trước khi hết tháng. */
    public boolean willRunOutThisMonth;
    /** Ngày trong tháng (1-31) dự kiến hết tiền. {@code 0} nếu không áp dụng. */
    public int projectedRunOutDay;

    // --- Cảnh báo ---
    public List<FinancialAlertType> alerts = new ArrayList<>();

    public FinancialInsights() {}

    public FinancialInsights(int score, FinancialHealthStatus status) {
        this.healthScore = score;
        this.status = status;
    }
}
