package com.expensemanager.app.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.GoalRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.ActivityDashboardBinding;
import com.expensemanager.app.domain.usecase.BudgetService;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bức tranh tài chính tổng thể trên 1 trang:
 * <ul>
 *   <li>Thu / chi / còn lại trong tháng.</li>
 *   <li>Tình trạng ngân sách Zero-Based (Cần phân bổ, đã phân bổ, vượt, cuốn chiếu).</li>
 *   <li>Biểu đồ chi theo danh mục + số dư các ví.</li>
 *   <li>Tiến độ từng mục tiêu tiết kiệm so với mục tiêu hướng tới.</li>
 * </ul>
 */
public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;

    private final AuthRepository authRepo = new AuthRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private final GoalRepository goalRepo = new GoalRepository();

    private String uid;
    private String monthKey;

    // Trạng thái tháng này
    private long monthIncome = 0L;
    private long monthExpense = 0L;
    private final Map<String, Long> spentByCat = new HashMap<>();
    private final Map<String, Long> allocatedMap = new HashMap<>();

    // Cuốn chiếu từ tháng trước
    private final Map<String, Long> prevAllocatedMap = new HashMap<>();
    private final Map<String, Long> prevSpentMap = new HashMap<>();
    private Map<String, Long> rolloverMap = new HashMap<>();

    private List<Category> categories = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private List<SavingsGoal> goals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        monthKey = DateUtils.currentMonthKey();
        binding.textMonth.setText(DateUtils.formatMonthYear(monthKey));
        binding.btnBack.setOnClickListener(v -> finish());

        observeData();
    }

    private void observeData() {
        String prevMonthKey = DateUtils.previousMonthKey(monthKey);

        // Giao dịch tháng này -> thu, chi, chi theo danh mục
        txRepo.observeMonth(uid, monthKey).observe(this, txs -> {
            monthIncome = 0L;
            monthExpense = 0L;
            spentByCat.clear();
            if (txs != null) {
                for (Transaction t : txs) {
                    if (Transaction.TYPE_INCOME.equals(t.getType())) {
                        monthIncome += t.getAmount();
                    } else if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                        monthExpense += t.getAmount();
                        if (t.getCategoryId() != null) {
                            Long cur = spentByCat.get(t.getCategoryId());
                            spentByCat.put(t.getCategoryId(), (cur != null ? cur : 0L) + t.getAmount());
                        }
                    }
                }
            }
            render();
        });

        // Ngân sách tháng này (số tiền phân bổ ZBB)
        budgetRepo.observeMonth(uid, monthKey).observe(this, list -> {
            allocatedMap.clear();
            if (list != null) {
                for (Budget b : list) {
                    if (b.getCategoryId() != null) {
                        allocatedMap.put(b.getCategoryId(), b.getAllocatedAmount());
                    }
                }
            }
            render();
        });

        // Tháng trước -> cuốn chiếu
        budgetRepo.observeMonth(uid, prevMonthKey).observe(this, list -> {
            prevAllocatedMap.clear();
            if (list != null) {
                for (Budget b : list) {
                    if (b.getCategoryId() != null) {
                        prevAllocatedMap.put(b.getCategoryId(), b.getAllocatedAmount());
                    }
                }
            }
            recomputeRollover();
            render();
        });
        txRepo.observeMonth(uid, prevMonthKey).observe(this, txs -> {
            prevSpentMap.clear();
            if (txs != null) {
                for (Transaction t : txs) {
                    if (Transaction.TYPE_EXPENSE.equals(t.getType()) && t.getCategoryId() != null) {
                        Long cur = prevSpentMap.get(t.getCategoryId());
                        prevSpentMap.put(t.getCategoryId(), (cur != null ? cur : 0L) + t.getAmount());
                    }
                }
            }
            recomputeRollover();
            render();
        });

        categoryRepo.observeAll(uid).observe(this, list -> {
            categories = list != null ? list : new ArrayList<>();
            render();
        });

        walletRepo.observeAll(uid).observe(this, list -> {
            wallets = new ArrayList<>();
            if (list != null) {
                for (Wallet w : list) {
                    if (!w.isArchived()) wallets.add(w);
                }
            }
            render();
        });

        goalRepo.observeAll(uid).observe(this, list -> {
            goals = new ArrayList<>();
            if (list != null) {
                for (SavingsGoal g : list) {
                    if (!g.isArchived()) goals.add(g);
                }
            }
            render();
        });
    }

    private void recomputeRollover() {
        rolloverMap = new HashMap<>();
        Set<String> catIds = new HashSet<>();
        catIds.addAll(prevAllocatedMap.keySet());
        catIds.addAll(prevSpentMap.keySet());
        for (String catId : catIds) {
            long a = prevAllocatedMap.containsKey(catId) ? prevAllocatedMap.get(catId) : 0L;
            long s = prevSpentMap.containsKey(catId) ? prevSpentMap.get(catId) : 0L;
            long roll = BudgetService.categoryRollover(a, s);
            if (roll != 0L) rolloverMap.put(catId, roll);
        }
    }

    private void render() {
        if (binding == null) return;
        renderMonthSummary();
        renderBudgetStatus();
        renderPieChart();
        renderWallets();
        renderGoals();
    }

    // ----- 1. Thu / chi / còn lại -----
    private void renderMonthSummary() {
        binding.textIncome.setText(MoneyFormat.formatLong(monthIncome));
        binding.textExpense.setText(MoneyFormat.formatLong(monthExpense));
        long net = monthIncome - monthExpense;
        binding.textNet.setText(MoneyFormat.formatLong(net));
        binding.textNet.setTextColor(ContextCompat.getColor(this,
                net >= 0 ? R.color.income_green : R.color.expense_red));
    }

    // ----- 2. Tình trạng ngân sách ZBB -----
    private void renderBudgetStatus() {
        long totalAllocated = 0L;
        for (Long v : allocatedMap.values()) totalAllocated += v;

        BudgetService.BudgetPool pool = BudgetService.pool(monthIncome, totalAllocated);
        binding.textAllocated.setText(MoneyFormat.formatLong(totalAllocated));
        binding.textToBeBudgeted.setText(MoneyFormat.formatLong(pool.toBeBudgeted));

        int tbbColor;
        String statusText;
        int statusColor;
        if (pool.isOverBudgeted()) {
            tbbColor = R.color.expense_red;
            statusText = getString(R.string.s1_over_budgeted);
            statusColor = R.color.expense_red;
        } else if (pool.isBalanced()) {
            tbbColor = R.color.income_green;
            statusText = getString(R.string.s1_all_assigned);
            statusColor = R.color.income_green;
        } else {
            tbbColor = R.color.saving_blue;
            statusText = getString(R.string.s1_unallocated_money) + ": "
                    + MoneyFormat.formatLong(pool.toBeBudgeted);
            statusColor = R.color.saving_blue;
        }
        binding.textToBeBudgeted.setTextColor(ContextCompat.getColor(this, tbbColor));

        // Số danh mục vượt phân bổ (gồm cuốn chiếu)
        int overCount = 0;
        for (Category c : categories) {
            if (!Category.TYPE_EXPENSE.equals(c.getType())) continue;
            long alloc = allocatedMap.containsKey(c.getId()) ? allocatedMap.get(c.getId()) : 0L;
            long roll = rolloverMap.containsKey(c.getId()) ? rolloverMap.get(c.getId()) : 0L;
            long spent = spentByCat.containsKey(c.getId()) ? spentByCat.get(c.getId()) : 0L;
            if (BudgetService.envelope(alloc, roll, spent).isOverspent()) overCount++;
        }
        if (overCount > 0) {
            binding.textBudgetStatusLine.setText(getString(R.string.dash_over_categories, overCount));
            binding.textBudgetStatusLine.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
        } else {
            binding.textBudgetStatusLine.setText(statusText);
            binding.textBudgetStatusLine.setTextColor(ContextCompat.getColor(this, statusColor));
        }

        long totalRollover = 0L;
        for (Long r : rolloverMap.values()) totalRollover += r;
        if (totalRollover != 0L) {
            String sign = totalRollover > 0 ? "+" : "";
            binding.textRolloverLine.setText(getString(R.string.dash_rollover_total,
                    sign + MoneyFormat.formatLong(totalRollover)));
            binding.textRolloverLine.setTextColor(ContextCompat.getColor(this,
                    totalRollover >= 0 ? R.color.income_green : R.color.expense_red));
            binding.textRolloverLine.setVisibility(View.VISIBLE);
        } else {
            binding.textRolloverLine.setVisibility(View.GONE);
        }
    }

    // ----- 3. Biểu đồ chi theo danh mục -----
    private void renderPieChart() {
        Map<String, Category> catMap = new HashMap<>();
        for (Category c : categories) catMap.put(c.getId(), c);

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(spentByCat.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Map.Entry<String, Long> e : sorted) {
            if (e.getValue() <= 0L) continue;
            Category c = catMap.get(e.getKey());
            String name = c != null ? c.getName() : e.getKey();
            entries.add(new PieEntry((float) e.getValue(), name));
            if (c != null && c.getColorHex() != null) {
                try { colors.add(Color.parseColor(c.getColorHex())); }
                catch (Exception ex) { colors.add(Color.LTGRAY); }
            } else {
                colors.add(Color.LTGRAY);
            }
        }

        if (entries.isEmpty()) {
            binding.pieChart.clear();
            binding.pieChart.setVisibility(View.GONE);
            binding.textNoData.setVisibility(View.VISIBLE);
            return;
        }
        binding.pieChart.setVisibility(View.VISIBLE);
        binding.textNoData.setVisibility(View.GONE);

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(colors);
        set.setValueTextSize(11f);
        set.setValueTextColor(Color.WHITE);

        PieData data = new PieData(set);
        binding.pieChart.setData(data);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.setHoleRadius(45f);
        binding.pieChart.setTransparentCircleRadius(50f);
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.getLegend().setEnabled(true);
        binding.pieChart.getLegend().setTextColor(
                ContextCompat.getColor(this, R.color.text_primary));
        binding.pieChart.getLegend().setWordWrapEnabled(true);
        binding.pieChart.setExtraOffsets(8, 8, 8, 8);
        binding.pieChart.animateY(600);
        binding.pieChart.invalidate();
    }

    // ----- 4. Số dư ví -----
    private void renderWallets() {
        binding.layoutWallets.removeAllViews();
        long total = 0L;
        for (Wallet w : wallets) {
            total += w.getCurrentBalance();
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_dashboard_wallet, binding.layoutWallets, false);
            ((TextView) row.findViewById(R.id.textWalletName)).setText(w.getName());
            TextView bal = row.findViewById(R.id.textWalletBalance);
            bal.setText(MoneyFormat.formatLong(w.getCurrentBalance()));
            bal.setTextColor(ContextCompat.getColor(this,
                    w.getCurrentBalance() >= 0 ? R.color.text_primary : R.color.expense_red));
            binding.layoutWallets.addView(row);
        }
        binding.textTotalBalance.setText(MoneyFormat.formatLong(total));
    }

    // ----- 5. Tiến độ mục tiêu tiết kiệm -----
    private void renderGoals() {
        binding.layoutGoals.removeAllViews();
        if (goals.isEmpty()) {
            binding.textNoGoals.setVisibility(View.VISIBLE);
            return;
        }
        binding.textNoGoals.setVisibility(View.GONE);

        for (SavingsGoal g : goals) {
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_dashboard_goal, binding.layoutGoals, false);

            ((TextView) row.findViewById(R.id.textGoalTitle)).setText(g.getTitle());

            int pct = Math.round(g.getProgress() * 100f);
            TextView percent = row.findViewById(R.id.textGoalPercent);
            percent.setText(pct + "%");

            LinearProgressIndicator bar = row.findViewById(R.id.progressGoal);
            bar.setProgress(Math.max(0, Math.min(100, pct)));
            int barColor = g.isCompleted() || pct >= 100
                    ? R.color.income_green
                    : (g.isOverdue() ? R.color.expense_red : R.color.primary);
            bar.setIndicatorColor(ContextCompat.getColor(this, barColor));
            percent.setTextColor(ContextCompat.getColor(this, barColor));

            ((TextView) row.findViewById(R.id.textGoalAmounts)).setText(
                    getString(R.string.dash_goal_progress_value,
                            MoneyFormat.formatLong(g.getSavedAmount()),
                            MoneyFormat.formatLong(g.getTargetAmount())));

            TextView remaining = row.findViewById(R.id.textGoalRemaining);
            long left = g.getTargetAmount() - g.getSavedAmount();
            if (g.isCompleted() || left <= 0L) {
                remaining.setText(getString(R.string.dash_goal_done));
                remaining.setTextColor(ContextCompat.getColor(this, R.color.income_green));
            } else {
                remaining.setText(getString(R.string.dash_goal_remaining,
                        MoneyFormat.formatLong(left)));
                remaining.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }

            TextView deadline = row.findViewById(R.id.textGoalDeadline);
            if (g.isCompleted() || left <= 0L) {
                deadline.setVisibility(View.GONE);
            } else if (g.getDeadline() == null) {
                deadline.setVisibility(View.GONE);
            } else if (g.isOverdue()) {
                deadline.setText(getString(R.string.dash_goal_overdue));
                deadline.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
                deadline.setVisibility(View.VISIBLE);
            } else {
                long monthly = g.getMonthlyRequired();
                String text = getString(R.string.dash_goal_days_left, g.getRemainingDays());
                if (monthly > 0L) {
                    text = getString(R.string.dash_goal_monthly, MoneyFormat.formatLong(monthly))
                            + " · " + text;
                }
                deadline.setText(text);
                deadline.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                deadline.setVisibility(View.VISIBLE);
            }

            binding.layoutGoals.addView(row);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
