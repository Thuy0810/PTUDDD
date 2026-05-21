package com.expensemanager.app.util;

import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.FinancialInsights;
import com.expensemanager.app.data.model.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InsightsEngine {
    private InsightsEngine() {}

    public static FinancialInsights compute(
            List<Transaction> thisMonth,
            List<Transaction> lastMonth,
            List<Transaction> last7Days,
            List<Budget> budgets,
            List<Category> categories,
            double totalBalance,
            double monthlyBudgetLimit) {

        FinancialInsights fi = new FinancialInsights();
        List<String> alerts = new ArrayList<>();
        int score = 70;

        double income = sumType(thisMonth, Transaction.TYPE_INCOME);
        double expense = sumType(thisMonth, Transaction.TYPE_EXPENSE);
        double lastExpense = sumType(lastMonth, Transaction.TYPE_EXPENSE);

        if (lastExpense > 0 && expense > lastExpense * 1.2) {
            score -= 10;
            alerts.add("Chi tiêu tháng này tăng mạnh so với tháng trước.");
        } else if (expense < lastExpense) {
            score += 10;
        }
        if (income > expense) score += 10;

        for (Budget b : budgets) {
            if (Budget.SCOPE_MONTHLY.equals(b.getScope()) && b.getLimitAmount() > 0) {
                double pct = expense / b.getLimitAmount();
                if (pct >= 1) {
                    score -= 15;
                    alerts.add("Bạn đã vượt ngân sách tháng!");
                } else if (pct >= 0.9) {
                    score -= 5;
                    alerts.add("Bạn đã dùng 90% ngân sách tháng này.");
                } else if (pct >= 0.8) {
                    alerts.add("Bạn đã dùng 80% ngân sách tháng này.");
                }
            }
        }

        Set<String> days = new HashSet<>();
        for (Transaction t : thisMonth) {
            days.add(DateUtils.formatDisplay(t.getDateAsDate()));
        }
        if (days.size() >= 15) score += 10;
        if (days.size() < 5) {
            score -= 5;
            alerts.add("Bạn có thể đã bỏ sót ghi chi tiêu gần đây.");
        }

        score = Math.max(0, Math.min(100, score));
        fi.healthScore = score;
        fi.healthMessage = score >= 70
                ? "Bạn đang kiểm soát chi tiêu khá tốt."
                : "Cần chú ý chi tiêu nhiều hơn.";
        fi.alerts = alerts;

        Date now = new Date();
        int daysInMonth = DateUtils.daysInMonth(now);
        int day = DateUtils.dayOfMonth(now);
        int daysLeft = daysInMonth - day + 1;
        double limit = monthlyBudgetLimit > 0 ? monthlyBudgetLimit : expense * 1.5;
        double remaining = limit - expense;
        if (daysLeft > 0 && limit > 0) {
            fi.dailyAllowance = "Hôm nay nên tiêu tối đa: "
                    + MoneyFormat.format(remaining / daysLeft);
        }
        if (day > 0) {
            double predicted = expense / day * daysInMonth;
            fi.monthPrediction = "Dự đoán chi cuối tháng: " + MoneyFormat.format(predicted);
        }
        if (lastExpense > 0) {
            double diff = expense - lastExpense;
            double pct = Math.abs(diff / lastExpense * 100);
            if (diff < 0) {
                fi.monthComparison = String.format("Tốt hơn tháng trước %.0f%%", pct);
            } else if (diff > 0) {
                fi.monthComparison = "Chi nhiều hơn tháng trước " + MoneyFormat.format(diff);
            }
        }

        fi.feedMessage = limit > 0 && expense / limit >= 0.65
                ? String.format("Bạn đã tiêu %.0f%% ngân sách tháng. Hãy giữ nhịp chi tiêu.",
                expense / limit * 100)
                : "Tình hình tài chính ổn. Tiếp tục ghi chép đều đặn!";

        checkAbnormalSpending(thisMonth, last7Days, alerts);
        fi.alerts = alerts;
        return fi;
    }

    private static void checkAbnormalSpending(List<Transaction> thisMonth,
                                              List<Transaction> last7Days,
                                              List<String> alerts) {
        Date today = new Date();
        Map<String, Double> todayByCat = new HashMap<>();
        for (Transaction t : thisMonth) {
            if (!Transaction.TYPE_EXPENSE.equals(t.getType())) continue;
            if (!DateUtils.isSameDay(t.getDateAsDate(), today)) continue;
            String cat = t.getCategoryId();
            todayByCat.put(cat, todayByCat.getOrDefault(cat, 0.0) + t.getAmount());
        }
        for (Map.Entry<String, Double> e : todayByCat.entrySet()) {
            double prev7 = 0;
            for (Transaction t : last7Days) {
                if (Transaction.TYPE_EXPENSE.equals(t.getType())
                        && e.getKey().equals(t.getCategoryId())) {
                    prev7 += t.getAmount();
                }
            }
            double avg = prev7 / 7.0;
            if (avg > 0 && e.getValue() > avg * 1.5) {
                alerts.add("Hôm nay chi danh mục này cao hơn trung bình 7 ngày.");
                break;
            }
        }
    }

    private static double sumType(List<Transaction> list, String type) {
        double sum = 0;
        for (Transaction t : list) {
            if (type.equals(t.getType())) sum += t.getAmount();
        }
        return sum;
    }

    public static String topExpenseCategory(List<Transaction> monthTx,
                                            Map<String, Category> catMap) {
        Map<String, Double> sums = new HashMap<>();
        for (Transaction t : monthTx) {
            if (!Transaction.TYPE_EXPENSE.equals(t.getType())) continue;
            sums.put(t.getCategoryId(), sums.getOrDefault(t.getCategoryId(), 0.0) + t.getAmount());
        }
        String topId = null;
        double max = 0;
        for (Map.Entry<String, Double> e : sums.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                topId = e.getKey();
            }
        }
        if (topId == null) return null;
        Category c = catMap.get(topId);
        return c != null ? c.getName() + " - " + MoneyFormat.format(max) : MoneyFormat.format(max);
    }
}
