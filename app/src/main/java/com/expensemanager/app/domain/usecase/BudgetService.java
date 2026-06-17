package com.expensemanager.app.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nghiệp vụ ngân sách: validate, tính sử dụng, sinh cảnh báo.
 *
 * <p>Tách riêng khỏi UI và Repository theo ràng buộc 4.1:
 * <ul>
 *   <li>UI chỉ gọi service.</li>
 *   <li>Service tính toán thuần tuý, không truy cập Firestore.</li>
 *   <li>Repository chỉ lưu/đọc, không validate nghiệp vụ phức tạp.</li>
 * </ul>
 */
public final class BudgetService {

    /** Kết quả validate — rỗng nếu hợp lệ. */
    public static class ValidationResult {
        public final boolean valid;
        @Nullable public final String errorMessage;

        private ValidationResult(boolean valid, @Nullable String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult error(@NonNull String message) {
            return new ValidationResult(false, message);
        }
    }

    /** Kết quả tính sử dụng ngân sách. */
    public static class Usage {
        public final long limitAmount;
        public final long usedAmount;
        public final long remainingAmount; // có thể âm
        public final double usageRate;     // 0.0 - ∞, dùng double cho tỷ lệ

        public Usage(long limitAmount, long usedAmount) {
            this.limitAmount = limitAmount;
            this.usedAmount = usedAmount;
            this.remainingAmount = limitAmount - usedAmount;
            this.usageRate = limitAmount > 0 ? (double) usedAmount / limitAmount : 0.0;
        }

        public boolean isExceeded() { return remainingAmount < 0; }
    }

    private BudgetService() {}

    // ---------------------- Validate ----------------------

    /**
     * Validate một {@link Budget} trước khi lưu.
     *
     * @param b budget cần validate (id có thể null khi tạo mới)
     * @param existing danh sách budget đã tồn tại cùng tháng, để kiểm tra trùng lặp
     */
    @NonNull
    public static ValidationResult validate(@NonNull Budget b, @NonNull List<Budget> existing) {
        // 1. month format yyyy-MM
        if (b.getMonth() == null || !b.getMonth().matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            return ValidationResult.error("Tháng phải có định dạng yyyy-MM");
        }

        // 2. limitAmount > 0
        if (b.getLimitAmount() <= 0) {
            return ValidationResult.error("Hạn mức phải lớn hơn 0");
        }

        // 3. scope
        String scope = b.getScope();
        if (scope == null) {
            return ValidationResult.error("Phải chọn loại ngân sách");
        }
        if (Budget.SCOPE_MONTHLY.equals(scope)) {
            if (b.getCategoryId() != null && !b.getCategoryId().isEmpty()) {
                return ValidationResult.error("Ngân sách tổng không được có danh mục");
            }
        } else if (Budget.SCOPE_CATEGORY.equals(scope)) {
            if (b.getCategoryId() == null || b.getCategoryId().isEmpty()) {
                return ValidationResult.error("Ngân sách theo danh mục phải có categoryId");
            }
        } else {
            return ValidationResult.error("Loại ngân sách không hợp lệ");
        }

        // 4. alertAt: mỗi phần tử 0 < value <= 1, không trùng, sắp xếp tăng dần
        List<Double> alerts = b.getAlertAt();
        if (alerts == null || alerts.isEmpty()) {
            return ValidationResult.error("Phải có ít nhất 1 ngưỡng cảnh báo");
        }
        Set<Double> seen = new HashSet<>();
        double prev = 0.0;
        for (Double a : alerts) {
            if (a == null) return ValidationResult.error("Ngưỡng cảnh báo không hợp lệ");
            if (a <= 0 || a > 1) {
                return ValidationResult.error("Ngưỡng cảnh báo phải nằm trong (0, 1]");
            }
            if (a <= prev) {
                return ValidationResult.error("Ngưỡng cảnh báo phải sắp xếp tăng dần");
            }
            if (!seen.add(a)) {
                return ValidationResult.error("Ngưỡng cảnh báo bị trùng");
            }
            prev = a;
        }

        // 5. uniqueness: mỗi tháng chỉ 1 monthly; mỗi (tháng, category) chỉ 1 category
        for (Budget other : existing) {
            if (other.getId() != null && other.getId().equals(b.getId())) continue;
            if (!b.getMonth().equals(other.getMonth())) continue;
            if (other.isArchived()) continue;
            if (Budget.SCOPE_MONTHLY.equals(b.getScope())
                    && Budget.SCOPE_MONTHLY.equals(other.getScope())) {
                return ValidationResult.error("Đã có ngân sách tổng cho tháng " + b.getMonth());
            }
            if (Budget.SCOPE_CATEGORY.equals(b.getScope())
                    && Budget.SCOPE_CATEGORY.equals(other.getScope())
                    && b.getCategoryId() != null
                    && b.getCategoryId().equals(other.getCategoryId())) {
                return ValidationResult.error("Đã có ngân sách cho danh mục này trong tháng "
                        + b.getMonth());
            }
        }

        return ValidationResult.ok();
    }

    // ---------------------- Compute usage ----------------------

    /**
     * Tính số tiền đã chi cho một budget, chỉ tính expense (ràng buộc 9),
     * bỏ qua income.
     *
     * @param b budget
     * @param allTransactionsOfMonth tập giao dịch (thường là của tháng tương ứng)
     */
    public static long sumUsed(@NonNull Budget b, @NonNull List<Transaction> allTransactionsOfMonth) {
        long sum = 0L;
        for (Transaction t : allTransactionsOfMonth) {
            if (!Transaction.TYPE_EXPENSE.equals(t.getType())) continue;
            if (Budget.SCOPE_CATEGORY.equals(b.getScope())) {
                String catId = b.getCategoryId();
                if (catId == null || !catId.equals(t.getCategoryId())) continue;
            }
            sum += t.getAmount();
        }
        return sum;
    }

    /**
     * Tính {@link Usage} đầy đủ cho một budget.
     */
    @NonNull
    public static Usage computeUsage(@NonNull Budget b, @NonNull List<Transaction> txs) {
        long used = sumUsed(b, txs);
        return new Usage(b.getLimitAmount(), used);
    }

    // ---------------------- Alerts ----------------------

    /**
     * Sinh thông báo cảnh báo cho một budget. Trả về danh sách có thể rỗng.
     */
    @NonNull
    public static List<String> checkAlerts(@NonNull Budget b, @NonNull List<Transaction> txs) {
        List<String> alerts = new ArrayList<>();
        Usage u = computeUsage(b, txs);
        if (u.limitAmount <= 0) return alerts;

        if (u.isExceeded()) {
            long over = -u.remainingAmount;
            alerts.add("Vượt ngân sách "
                    + MoneyFormat.formatLong(over)
                    + (b.getCategoryId() != null ? " (danh mục)" : " (tổng)"));
            return alerts;
        }

        // Tìm ngưỡng CAO NHẤT mà pct >= threshold (alertAt sắp xếp tăng dần → duyệt ngược).
        double pct = u.usageRate;
        List<Double> thresholds = b.getAlertAt();
        for (int i = thresholds.size() - 1; i >= 0; i--) {
            Double threshold = thresholds.get(i);
            if (threshold != null && pct >= threshold) {
                int percent = (int) Math.floor(pct * 100);
                alerts.add("Đã dùng " + percent + "% ngân sách, còn "
                        + MoneyFormat.formatLong(u.remainingAmount));
                break; // chỉ thông báo ngưỡng cao nhất
            }
        }
        return alerts;
    }

    /**
     * Tính tổng hạn mức (chỉ tính budget monthly và/hoặc category tương ứng).
     * Hữu ích cho UI tổng quan tháng.
     */
    public static long sumLimits(@NonNull List<Budget> budgets) {
        long sum = 0L;
        for (Budget b : budgets) {
            if (!b.isArchived()) sum += b.getLimitAmount();
        }
        return sum;
    }

    /**
     * Trả về cặp khoá tháng hiện tại và tháng kế tiếp theo ICT.
     */
    @NonNull
    public static String[] getCurrentAndNextMonthKey() {
        String current = DateUtils.currentMonthKey();
        String next = DateUtils.nextMonthKey(current);
        return new String[] { current, next };
    }

    /**
     * Chuẩn hoá alertAt: bỏ null, ép trong khoảng (0, 1], loại trùng, sắp xếp tăng dần.
     * Trả về danh sách mới, không sửa list gốc.
     */
    @NonNull
    public static List<Double> normalizeAlertAt(@NonNull List<Double> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>(Collections.singletonList(0.8));
        }
        Set<Double> set = new HashSet<>();
        for (Double d : raw) {
            if (d == null) continue;
            double clamped = Math.max(0.01, Math.min(1.0, d));
            set.add(clamped);
        }
        List<Double> sorted = new ArrayList<>(set);
        Collections.sort(sorted);
        if (sorted.isEmpty()) sorted.add(0.8);
        return sorted;
    }
}
