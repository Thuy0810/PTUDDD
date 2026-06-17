package com.expensemanager.app.util;

import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.FinancialAlertType;
import com.expensemanager.app.data.model.FinancialHealthStatus;
import com.expensemanager.app.data.model.FinancialInsights;
import com.expensemanager.app.data.model.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tính toán sức khỏe tài chính — KHÔNG ghi câu tiếng Việt/Anh.
 * Chỉ trả về dữ liệu số, enum. UI chịu trách nhiệm format và hiển thị.
 */
public final class InsightsEngine {
    private InsightsEngine() {}

    public static FinancialInsights compute(
            List<Transaction> thisMonth,
            List<Transaction> lastMonth,
            List<Transaction> last7Days,
            List<Budget> budgets,
            List<Category> categories,
            long totalBalance,
            long monthlyBudgetLimit) {

        FinancialInsights fi = new FinancialInsights();
        List<FinancialAlertType> alertTypes = new ArrayList<>();

        long income = sumType(thisMonth, Transaction.TYPE_INCOME);
        long expense = sumType(thisMonth, Transaction.TYPE_EXPENSE);
        long lastExpense = sumType(lastMonth, Transaction.TYPE_EXPENSE);

        fi.incomeAmount = income;
        fi.expenseAmount = expense;
        fi.netCashFlow = income - expense;

        // Saving rate
        if (income > 0) {
            fi.savingRate = (double) fi.netCashFlow / income;
        }

        int score = 70; // base

        // So với tháng trước
        if (lastExpense > 0L) {
            double changeRate = (double) (expense - lastExpense) / lastExpense;
            fi.expenseChangeRate = changeRate;
            if (changeRate > 0.20) {
                score -= 10;
                alertTypes.add(FinancialAlertType.EXPENSE_INCREASED);
            } else if (changeRate < 0) {
                score += 10;
            }
        }

        if (income > expense) score += 10;

        // Ngân sách
        for (Budget b : budgets) {
            if (Budget.SCOPE_MONTHLY.equals(b.getScope()) && b.getLimitAmount() > 0L) {
                double pct = (double) expense / b.getLimitAmount();
                if (fi.budgetUsageRate == 0.0 || pct > fi.budgetUsageRate) {
                    fi.budgetUsageRate = pct;
                }
                if (pct >= 1.0) {
                    score -= 15;
                    alertTypes.add(FinancialAlertType.OVER_BUDGET);
                } else if (pct >= 0.90) {
                    score -= 5;
                    alertTypes.add(FinancialAlertType.NEAR_BUDGET_LIMIT);
                } else if (pct >= 0.80) {
                    alertTypes.add(FinancialAlertType.NEAR_BUDGET_LIMIT);
                }
            }
        }

        // Tần suất ghi chép
        Set<String> days = new HashSet<>();
        for (Transaction t : thisMonth) {
            days.add(DateUtils.formatDisplay(t.getDateAsDate()));
        }
        if (days.size() >= 15) score += 10;
        if (days.size() < 5) {
            score -= 5;
            alertTypes.add(FinancialAlertType.MISSING_RECORDS);
        }

        score = Math.max(0, Math.min(100, score));
        fi.healthScore = score;
        fi.status = FinancialHealthStatus.fromScore(score);
        fi.alerts = alertTypes;

        // Dự đoán
        Date now = DateUtils.nowVietnam();
        int daysInMonth = DateUtils.daysInMonth(now);
        int day = DateUtils.dayOfMonth(now);
        int daysLeft = daysInMonth - day + 1;

        long limit = monthlyBudgetLimit > 0L ? monthlyBudgetLimit : (expense > 0 ? expense * 3 / 2 : 0L);
        long remaining = limit - expense;
        if (daysLeft > 0 && limit > 0L) {
            fi.dailyAllowanceAmount = remaining / daysLeft;
        }
        if (day > 0 && expense > 0) {
            fi.predictedMonthExpense = expense * daysInMonth / day;
        }

        // --- Dự báo dòng tiền theo tốc độ chi (run-rate forecast) ---
        fi.balanceSnapshot = totalBalance;
        long avgDaily = day > 0 ? expense / day : 0L;
        fi.avgDailyExpense = avgDaily;
        if (avgDaily > 0L) {
            long lasts = totalBalance > 0L ? totalBalance / avgDaily : 0L;
            fi.daysBalanceLasts = lasts > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) lasts;
            // Nếu số ngày trụ được < số ngày còn lại trong tháng → cảnh báo hết tiền.
            if (fi.daysBalanceLasts < daysLeft) {
                fi.willRunOutThisMonth = true;
                int runOutDay = day + fi.daysBalanceLasts;
                if (runOutDay > daysInMonth) runOutDay = daysInMonth;
                if (runOutDay < day) runOutDay = day;
                fi.projectedRunOutDay = runOutDay;
                score -= 10;
                // Cảnh báo quan trọng nhất → đặt đầu danh sách.
                alertTypes.add(0, FinancialAlertType.CASH_RUNOUT_RISK);
            }
        }
        score = Math.max(0, Math.min(100, score));
        fi.healthScore = score;
        fi.status = FinancialHealthStatus.fromScore(score);

        // Top expense category
        fi.topExpenseCategoryId = topExpenseCategoryId(thisMonth);

        // Kiểm tra chi bất thường
        checkAbnormalSpending(thisMonth, last7Days, alertTypes);
        fi.alerts = alertTypes;

        return fi;
    }

    private static void checkAbnormalSpending(List<Transaction> thisMonth,
                                              List<Transaction> last7Days,
                                              List<FinancialAlertType> alerts) {
        Date today = DateUtils.nowVietnam();
        Map<String, Long> todayByCat = new HashMap<>();
        for (Transaction t : thisMonth) {
            if (!Transaction.TYPE_EXPENSE.equals(t.getType())) continue;
            if (!DateUtils.isSameDay(t.getDateAsDate(), today)) continue;
            String cat = t.getCategoryId();
            todayByCat.put(cat, todayByCat.getOrDefault(cat, 0L) + t.getAmount());
        }
        for (Map.Entry<String, Long> e : todayByCat.entrySet()) {
            long prev7 = 0L;
            for (Transaction t : last7Days) {
                if (Transaction.TYPE_EXPENSE.equals(t.getType())
                        && e.getKey().equals(t.getCategoryId())) {
                    prev7 += t.getAmount();
                }
            }
            long avg = prev7 / 7L;
            if (avg > 0L && e.getValue() > avg * 3 / 2) {
                alerts.add(FinancialAlertType.ABNORMAL_SPENDING);
                break;
            }
        }
    }

    private static long sumType(List<Transaction> list, String type) {
        if (list == null) return 0L;
        long sum = 0L;
        for (Transaction t : list) {
            if (type.equals(t.getType())) sum += t.getAmount();
        }
        return sum;
    }

    public static String topExpenseCategory(List<Transaction> monthTx,
                                            Map<String, Category> catMap) {
        if (monthTx == null || monthTx.isEmpty()) return null;
        Map<String, Long> sums = new HashMap<>();
        for (Transaction t : monthTx) {
            if (!Transaction.TYPE_EXPENSE.equals(t.getType())) continue;
            sums.put(t.getCategoryId(),
                    sums.getOrDefault(t.getCategoryId(), 0L) + t.getAmount());
        }
        String topId = null;
        long max = 0L;
        for (Map.Entry<String, Long> e : sums.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                topId = e.getKey();
            }
        }
        if (topId == null) return null;
        Category c = catMap.get(topId);
        return c != null ? c.getName() : null;
    }

    private static String topExpenseCategoryId(List<Transaction> monthTx) {
        if (monthTx == null || monthTx.isEmpty()) return null;
        Map<String, Long> sums = new HashMap<>();
        for (Transaction t : monthTx) {
            if (!Transaction.TYPE_EXPENSE.equals(t.getType())) continue;
            sums.put(t.getCategoryId(),
                    sums.getOrDefault(t.getCategoryId(), 0L) + t.getAmount());
        }
        String topId = null;
        long max = 0L;
        for (Map.Entry<String, Long> e : sums.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                topId = e.getKey();
            }
        }
        return topId;
    }
}
