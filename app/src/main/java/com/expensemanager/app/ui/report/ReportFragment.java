package com.expensemanager.app.ui.report;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.FragmentReportBinding;
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
import com.expensemanager.app.data.model.FinancialInsights;
import com.expensemanager.app.domain.usecase.BudgetService;
import com.expensemanager.app.ui.adapter.TransactionAdapter;
import com.expensemanager.app.ui.transaction.AddTransactionActivity;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.InsightsEngine;
import com.expensemanager.app.util.MoneyFormat;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportFragment extends Fragment {
    private FragmentReportBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private Map<String, Category> categoryMap = new HashMap<>();
    private Map<String, Wallet> walletMap = new HashMap<>();
    private List<Wallet> wallets = new ArrayList<>();
    private Map<String, Double> lastMonthByCat = new HashMap<>();
    private List<Transaction> currentTxs = new ArrayList<>();
    private boolean lastMonthLoaded = false;

    // Dự báo dòng tiền (luôn theo tháng hiện tại + tổng số dư)
    private List<Transaction> forecastMonthTx = new ArrayList<>();
    private long forecastBalance = 0L;
    private boolean forecastTxLoaded = false;
    private boolean forecastWalletsLoaded = false;

    // Bức tranh tài chính tháng này (cố định theo THÁNG HIỆN TẠI, độc lập bộ lọc kỳ)
    private String overviewMonthKey;
    private long ovMonthIncome = 0L;
    private final Map<String, Long> ovSpentByCat = new HashMap<>();
    private final Map<String, Long> ovAllocatedMap = new HashMap<>();
    private final Map<String, Long> ovPrevAllocatedMap = new HashMap<>();
    private final Map<String, Long> ovPrevSpentMap = new HashMap<>();
    private Map<String, Long> ovRolloverMap = new HashMap<>();
    private List<Category> ovCategories = new ArrayList<>();
    private List<Wallet> ovWallets = new ArrayList<>();
    private List<SavingsGoal> ovGoals = new ArrayList<>();

    private final Calendar selectedDate = Calendar.getInstance();
    private int selectedPeriod = 0; // 0=Ngày, 1=Tuần, 2=Tháng, 3=Quý, 4=Năm
    private String selectedWalletId = null;
    private String selectedType = "all"; // all/income/expense

    private TransactionRepository txRepo;
    private String currentUid;
    private TransactionAdapter txAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        currentUid = authRepo.getUid();
        if (currentUid == null) return;

        txRepo = new TransactionRepository();

        setupRecyclerView();
        setupPeriodSpinner();
        setupDatePickerButton();
        setupFilters();
        setupChartListeners();

        loadData();
        setupForecast();
        setupOverview();
    }

    /**
     * Bức tranh tài chính tháng này: tình trạng ngân sách ZBB, số dư ví, tiến độ mục tiêu.
     * Luôn theo THÁNG HIỆN TẠI, độc lập với bộ lọc kỳ báo cáo bên trên.
     */
    private void setupOverview() {
        overviewMonthKey = DateUtils.currentMonthKey();
        binding.textOverviewMonth.setText(DateUtils.formatMonthYear(overviewMonthKey));

        String prevMonthKey = DateUtils.previousMonthKey(overviewMonthKey);

        BudgetRepository budgetRepo = new BudgetRepository();
        GoalRepository goalRepo = new GoalRepository();

        txRepo.observeMonth(currentUid, overviewMonthKey).observe(getViewLifecycleOwner(), txs -> {
            ovMonthIncome = 0L;
            ovSpentByCat.clear();
            if (txs != null) {
                for (Transaction t : txs) {
                    if (Transaction.TYPE_INCOME.equals(t.getType())) {
                        ovMonthIncome += t.getAmount();
                    } else if (Transaction.TYPE_EXPENSE.equals(t.getType()) && t.getCategoryId() != null) {
                        Long cur = ovSpentByCat.get(t.getCategoryId());
                        ovSpentByCat.put(t.getCategoryId(), (cur != null ? cur : 0L) + t.getAmount());
                    }
                }
            }
            renderOverview();
        });

        budgetRepo.observeMonth(currentUid, overviewMonthKey).observe(getViewLifecycleOwner(), list -> {
            ovAllocatedMap.clear();
            if (list != null) {
                for (Budget b : list) {
                    if (b.getCategoryId() != null) ovAllocatedMap.put(b.getCategoryId(), b.getAllocatedAmount());
                }
            }
            renderOverview();
        });

        budgetRepo.observeMonth(currentUid, prevMonthKey).observe(getViewLifecycleOwner(), list -> {
            ovPrevAllocatedMap.clear();
            if (list != null) {
                for (Budget b : list) {
                    if (b.getCategoryId() != null) ovPrevAllocatedMap.put(b.getCategoryId(), b.getAllocatedAmount());
                }
            }
            recomputeOverviewRollover();
            renderOverview();
        });

        txRepo.observeMonth(currentUid, prevMonthKey).observe(getViewLifecycleOwner(), txs -> {
            ovPrevSpentMap.clear();
            if (txs != null) {
                for (Transaction t : txs) {
                    if (Transaction.TYPE_EXPENSE.equals(t.getType()) && t.getCategoryId() != null) {
                        Long cur = ovPrevSpentMap.get(t.getCategoryId());
                        ovPrevSpentMap.put(t.getCategoryId(), (cur != null ? cur : 0L) + t.getAmount());
                    }
                }
            }
            recomputeOverviewRollover();
            renderOverview();
        });

        new CategoryRepository().observeAll(currentUid).observe(getViewLifecycleOwner(), list -> {
            ovCategories = list != null ? list : new ArrayList<>();
            renderOverview();
        });

        new WalletRepository().observeAll(currentUid).observe(getViewLifecycleOwner(), list -> {
            ovWallets = new ArrayList<>();
            if (list != null) {
                for (Wallet w : list) {
                    if (w != null && !w.isArchived()) ovWallets.add(w);
                }
            }
            renderOverview();
        });

        goalRepo.observeAll(currentUid).observe(getViewLifecycleOwner(), list -> {
            ovGoals = new ArrayList<>();
            if (list != null) {
                for (SavingsGoal g : list) {
                    if (g != null && !g.isArchived()) ovGoals.add(g);
                }
            }
            renderOverview();
        });
    }

    private void recomputeOverviewRollover() {
        ovRolloverMap = new HashMap<>();
        Set<String> catIds = new HashSet<>();
        catIds.addAll(ovPrevAllocatedMap.keySet());
        catIds.addAll(ovPrevSpentMap.keySet());
        for (String catId : catIds) {
            long a = ovPrevAllocatedMap.containsKey(catId) ? ovPrevAllocatedMap.get(catId) : 0L;
            long s = ovPrevSpentMap.containsKey(catId) ? ovPrevSpentMap.get(catId) : 0L;
            long roll = BudgetService.categoryRollover(a, s);
            if (roll != 0L) ovRolloverMap.put(catId, roll);
        }
    }

    private void renderOverview() {
        if (binding == null) return;
        renderBudgetStatus();
        renderOverviewWallets();
        renderGoals();
    }

    private void renderBudgetStatus() {
        long totalAllocated = 0L;
        for (Long v : ovAllocatedMap.values()) totalAllocated += v;

        BudgetService.BudgetPool pool = BudgetService.pool(ovMonthIncome, totalAllocated);
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
        binding.textToBeBudgeted.setTextColor(ContextCompat.getColor(requireContext(), tbbColor));

        int overCount = 0;
        for (Category c : ovCategories) {
            if (!Category.TYPE_EXPENSE.equals(c.getType())) continue;
            long alloc = ovAllocatedMap.containsKey(c.getId()) ? ovAllocatedMap.get(c.getId()) : 0L;
            long roll = ovRolloverMap.containsKey(c.getId()) ? ovRolloverMap.get(c.getId()) : 0L;
            long spent = ovSpentByCat.containsKey(c.getId()) ? ovSpentByCat.get(c.getId()) : 0L;
            if (BudgetService.envelope(alloc, roll, spent).isOverspent()) overCount++;
        }
        if (overCount > 0) {
            binding.textBudgetStatusLine.setText(getString(R.string.dash_over_categories, overCount));
            binding.textBudgetStatusLine.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        } else {
            binding.textBudgetStatusLine.setText(statusText);
            binding.textBudgetStatusLine.setTextColor(ContextCompat.getColor(requireContext(), statusColor));
        }

        long totalRollover = 0L;
        for (Long r : ovRolloverMap.values()) totalRollover += r;
        if (totalRollover != 0L) {
            String sign = totalRollover > 0 ? "+" : "";
            binding.textRolloverLine.setText(getString(R.string.dash_rollover_total,
                    sign + MoneyFormat.formatLong(totalRollover)));
            binding.textRolloverLine.setTextColor(ContextCompat.getColor(requireContext(),
                    totalRollover >= 0 ? R.color.income_green : R.color.expense_red));
            binding.textRolloverLine.setVisibility(View.VISIBLE);
        } else {
            binding.textRolloverLine.setVisibility(View.GONE);
        }
    }

    private void renderOverviewWallets() {
        binding.layoutWallets.removeAllViews();
        long total = 0L;
        for (Wallet w : ovWallets) {
            total += w.getCurrentBalance();
            View row = getLayoutInflater()
                    .inflate(R.layout.item_dashboard_wallet, binding.layoutWallets, false);
            ((TextView) row.findViewById(R.id.textWalletName)).setText(w.getName());
            TextView bal = row.findViewById(R.id.textWalletBalance);
            bal.setText(MoneyFormat.formatLong(w.getCurrentBalance()));
            bal.setTextColor(ContextCompat.getColor(requireContext(),
                    w.getCurrentBalance() >= 0 ? R.color.text_primary : R.color.expense_red));
            binding.layoutWallets.addView(row);
        }
        binding.textWalletsTotal.setText(MoneyFormat.formatLong(total));
    }

    private void renderGoals() {
        binding.layoutGoals.removeAllViews();
        if (ovGoals.isEmpty()) {
            binding.textNoGoals.setVisibility(View.VISIBLE);
            return;
        }
        binding.textNoGoals.setVisibility(View.GONE);

        for (SavingsGoal g : ovGoals) {
            View row = getLayoutInflater()
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
            bar.setIndicatorColor(ContextCompat.getColor(requireContext(), barColor));
            percent.setTextColor(ContextCompat.getColor(requireContext(), barColor));

            ((TextView) row.findViewById(R.id.textGoalAmounts)).setText(
                    getString(R.string.dash_goal_progress_value,
                            MoneyFormat.formatLong(g.getSavedAmount()),
                            MoneyFormat.formatLong(g.getTargetAmount())));

            TextView remaining = row.findViewById(R.id.textGoalRemaining);
            long left = g.getTargetAmount() - g.getSavedAmount();
            if (g.isCompleted() || left <= 0L) {
                remaining.setText(getString(R.string.dash_goal_done));
                remaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
            } else {
                remaining.setText(getString(R.string.dash_goal_remaining,
                        MoneyFormat.formatLong(left)));
                remaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }

            TextView deadline = row.findViewById(R.id.textGoalDeadline);
            if (g.isCompleted() || left <= 0L || g.getDeadline() == null) {
                deadline.setVisibility(View.GONE);
            } else if (g.isOverdue()) {
                deadline.setText(getString(R.string.dash_goal_overdue));
                deadline.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                deadline.setVisibility(View.VISIBLE);
            } else {
                long monthly = g.getMonthlyRequired();
                String text = getString(R.string.dash_goal_days_left, g.getRemainingDays());
                if (monthly > 0L) {
                    text = getString(R.string.dash_goal_monthly, MoneyFormat.formatLong(monthly))
                            + " · " + text;
                }
                deadline.setText(text);
                deadline.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                deadline.setVisibility(View.VISIBLE);
            }

            binding.layoutGoals.addView(row);
        }
    }

    /**
     * Dự báo dòng tiền dựa trên tốc độ chi của THÁNG HIỆN TẠI và tổng số dư ví,
     * độc lập với bộ lọc kỳ báo cáo bên trên.
     */
    private void setupForecast() {
        txRepo.observeMonth(currentUid, DateUtils.currentMonthKey())
                .observe(getViewLifecycleOwner(), list -> {
                    forecastMonthTx = list != null ? list : new ArrayList<>();
                    forecastTxLoaded = true;
                    recomputeForecast();
                });

        new WalletRepository().observeAll(currentUid)
                .observe(getViewLifecycleOwner(), list -> {
                    long total = 0L;
                    if (list != null) {
                        for (Wallet w : list) {
                            if (w != null && !w.isArchived()) total += w.getCurrentBalance();
                        }
                    }
                    forecastBalance = total;
                    forecastWalletsLoaded = true;
                    recomputeForecast();
                });
    }

    private void recomputeForecast() {
        if (binding == null || !forecastTxLoaded || !forecastWalletsLoaded) return;

        FinancialInsights fi = InsightsEngine.compute(
                forecastMonthTx, new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), forecastBalance, 0L);

        binding.textForecastAvgDaily.setText(MoneyFormat.format(fi.avgDailyExpense));
        binding.textForecastPredicted.setText(
                fi.predictedMonthExpense > 0 ? MoneyFormat.format(fi.predictedMonthExpense) : "--");

        if (fi.daysBalanceLasts < 0) {
            binding.textForecastDaysLeft.setText("--");
        } else {
            binding.textForecastDaysLeft.setText(
                    getString(R.string.forecast_days_value, fi.daysBalanceLasts));
        }

        if (fi.willRunOutThisMonth && fi.projectedRunOutDay > 0) {
            binding.textForecastWarning.setVisibility(View.VISIBLE);
            binding.textForecastWarning.setText(
                    getString(R.string.forecast_runout_warning, fi.projectedRunOutDay));
        } else {
            binding.textForecastWarning.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        txAdapter = new TransactionAdapter();
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTransactions.setAdapter(txAdapter);

        txAdapter.setOnItemClick(t -> {
            Intent i = new Intent(requireContext(), AddTransactionActivity.class);
            i.putExtra(AddTransactionActivity.EXTRA_TX_ID, t.getId());
            startActivity(i);
        });
    }

    private void setupPeriodSpinner() {
        String[] periods = {
                getString(R.string.j3_period_day),
                getString(R.string.j3_period_week),
                getString(R.string.j3_period_month),
                getString(R.string.j3_period_quarter),
                getString(R.string.j3_period_year)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, java.util.Arrays.asList(periods));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPeriod.setAdapter(adapter);

        binding.spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedPeriod = pos;
                updateDateButtonLabel();
                loadData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePickerButton() {
        updateDateButtonLabel();
        binding.btnPickDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, day);
                        updateDateButtonLabel();
                        loadData();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });
    }

    private void updateDateButtonLabel() {
        if (binding == null) return;
        String prefix;
        switch (selectedPeriod) {
            case 1:
                prefix = getString(R.string.j3_period_week);
                break;
            case 2:
                prefix = getString(R.string.j3_period_month);
                break;
            case 3:
                prefix = getString(R.string.j3_period_quarter);
                break;
            case 4:
                prefix = getString(R.string.j3_period_year);
                break;
            default:
                prefix = getString(R.string.j3_period_day);
                break;
        }
        binding.btnPickDate.setText(prefix + ": "
                + String.format(java.util.Locale.forLanguageTag("vi-VN"),
                "%02d/%02d/%04d",
                selectedDate.get(Calendar.DAY_OF_MONTH),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.YEAR)));
    }

    private void setupFilters() {
        CategoryRepository catRepo = new CategoryRepository();
        WalletRepository walletRepo = new WalletRepository();

        walletRepo.observeAll(currentUid).observe(getViewLifecycleOwner(), list -> {
            wallets = list != null ? list : new ArrayList<>();
            walletMap.clear();
            for (Wallet w : wallets) {
                if (w.getId() != null) walletMap.put(w.getId(), w);
            }
            txAdapter.setWalletMap(walletMap);
            setupWalletSpinner();
        });

        catRepo.observeAll(currentUid).observe(getViewLifecycleOwner(), categories -> {
            categoryMap = CategoryRepository.toMap(categories);
            txAdapter.setCategoryMap(categoryMap);
            applyFiltersAndBind();
        });

        binding.radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAll) selectedType = "all";
            else if (checkedId == R.id.radioIncome) selectedType = "income";
            else if (checkedId == R.id.radioExpense) selectedType = "expense";
            applyFiltersAndBind();
        });
    }

    private void setupWalletSpinner() {
        List<String> names = new ArrayList<>();
        names.add(getString(R.string.j3_all_wallets));
        for (Wallet w : wallets) names.add(w.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerWallet.setAdapter(adapter);

        binding.spinnerWallet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    selectedWalletId = null;
                } else if (pos - 1 < wallets.size()) {
                    selectedWalletId = wallets.get(pos - 1).getId();
                }
                applyFiltersAndBind();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupChartListeners() {
        // Trang thai rong tieng Viet + tranh truc am khi chua co du lieu
        int faint = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary);
        binding.pieChart.setNoDataText(getString(R.string.j3_no_chart_data));
        binding.pieChart.setNoDataTextColor(faint);
        binding.barChart.setNoDataText(getString(R.string.j3_no_chart_data));
        binding.barChart.setNoDataTextColor(faint);
        binding.barChart.getAxisLeft().setAxisMinimum(0f);

        binding.pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                PieEntry entry = (PieEntry) e;
                String catName = entry.getLabel();
                String catId = findCategoryIdByName(catName);
                if (catId != null) {
                    showCategoryTransactions(catId, catName);
                }
            }
            @Override public void onNothingSelected() {}
        });
    }

    private String findCategoryIdByName(String name) {
        for (Map.Entry<String, Category> e : categoryMap.entrySet()) {
            if (e.getValue().getName().equals(name)) return e.getKey();
        }
        return null;
    }

    private void showCategoryTransactions(String catId, String catName) {
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : currentTxs) {
            if (catId.equals(t.getCategoryId())
                    && Transaction.TYPE_EXPENSE.equals(t.getType())
                    && passesWalletFilter(t)
                    && passesTypeFilter(t)) {
                filtered.add(t);
            }
        }

        if (filtered.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.j3_no_tx_in_category, catName), Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Transaction t : filtered) {
            sb.append("• ").append(MoneyFormat.format(t.getAmount()))
                    .append(" — ").append(t.getNote())
                    .append("\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.j3_transactions_of, catName))
                .setMessage(sb.toString())
                .setPositiveButton(getString(R.string.j3_close), null)
                .show();
    }

    private void loadData() {
        Calendar[] range = getDateRange();
        Date start = range[0].getTime();
        Date end = range[1].getTime();

        txRepo.observeRange(currentUid, start, end)
                .observe(getViewLifecycleOwner(), txs -> {
                    currentTxs = (txs != null) ? txs : new ArrayList<>();
                    applyFiltersAndBind();
                });

        Calendar[] lastRange = getLastPeriodRange();
        Date lastStart = lastRange[0].getTime();
        Date lastEnd = lastRange[1].getTime();
        txRepo.observeRange(currentUid, lastStart, lastEnd)
                .observe(getViewLifecycleOwner(), lastTxs -> {
                    lastMonthByCat = new HashMap<>();
                    double expense = 0;
                    if (lastTxs != null) {
                        for (Transaction t : lastTxs) {
                            if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                                expense += t.getAmount();
                                String catId = t.getCategoryId();
                                if (catId != null && !catId.isEmpty()) {
                                    lastMonthByCat.put(catId,
                                            lastMonthByCat.getOrDefault(catId, 0.0) + t.getAmount());
                                }
                            }
                        }
                    }
                    lastMonthLoaded = true;
                    applyFiltersAndBind();
                });

        WalletRepository walletRepo = new WalletRepository();
        walletRepo.observeAll(currentUid).observe(getViewLifecycleOwner(), wallets -> {
            double totalBalance = 0;
            for (Wallet w : wallets) {
                totalBalance += w.getCurrentBalance();
            }
            if (binding != null) {
                binding.textTotalBalance.setText(MoneyFormat.format(totalBalance));
            }
        });
    }

    private void applyFiltersAndBind() {
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : currentTxs) {
            if (!passesWalletFilter(t)) continue;
            if (!passesTypeFilter(t)) continue;
            filtered.add(t);
        }

        // Hien thi danh sach giao dich
        List<Transaction> sortedTxs = new ArrayList<>(filtered);
        Collections.sort(sortedTxs, (a, b) -> {
            Date da = a.getDateAsDate();
            Date db = b.getDateAsDate();
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        txAdapter.setItems(sortedTxs);
        binding.textEmptyTransactions.setVisibility(sortedTxs.isEmpty() ? View.VISIBLE : View.GONE);

        bindMonthData(filtered);
    }

    private boolean passesWalletFilter(Transaction t) {
        if (selectedWalletId == null) return true;
        return selectedWalletId.equals(t.getWalletId());
    }

    private boolean passesTypeFilter(Transaction t) {
        switch (selectedType) {
            case "income": return Transaction.TYPE_INCOME.equals(t.getType());
            case "expense": return Transaction.TYPE_EXPENSE.equals(t.getType());
            default: return true;
        }
    }


    private Calendar[] getDateRange() {
        Calendar start = (Calendar) selectedDate.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();

        switch (selectedPeriod) {
            case 0: // Day
                end.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case 1: // Week (Mon-Sun around selected date)
                int dow = start.get(Calendar.DAY_OF_WEEK);
                int diffToMon = (dow == Calendar.SUNDAY) ? -6 : -(dow - Calendar.MONDAY);
                start.add(Calendar.DAY_OF_MONTH, diffToMon);
                end = (Calendar) start.clone();
                end.add(Calendar.DAY_OF_MONTH, 7);
                break;
            case 2: // Month
                start.set(Calendar.DAY_OF_MONTH, 1);
                end = (Calendar) start.clone();
                end.add(Calendar.MONTH, 1);
                break;
            case 3: // Quarter
                int quarter = start.get(Calendar.MONTH) / 3;
                start.set(Calendar.MONTH, quarter * 3);
                start.set(Calendar.DAY_OF_MONTH, 1);
                end.set(Calendar.MONTH, quarter * 3 + 2);
                end.set(Calendar.DAY_OF_MONTH, 1);
                end.add(Calendar.MONTH, 1);
                break;
            case 4: // Year
                start.set(Calendar.MONTH, Calendar.JANUARY);
                start.set(Calendar.DAY_OF_MONTH, 1);
                end = (Calendar) start.clone();
                end.add(Calendar.YEAR, 1);
                break;
        }
        return new Calendar[]{start, end};
    }

    private Calendar[] getLastPeriodRange() {
        Calendar[] curr = getDateRange();
        Calendar start = (Calendar) curr[0].clone();
        Calendar end = (Calendar) curr[1].clone();

        switch (selectedPeriod) {
            case 0: // Previous day
                start.add(Calendar.DAY_OF_MONTH, -1);
                end.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case 1: // Last week
                start.add(Calendar.DAY_OF_MONTH, -7);
                end.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case 3: // Last quarter
                start.add(Calendar.MONTH, -3);
                end.add(Calendar.MONTH, -3);
                break;
            case 4: // Last year
                start.add(Calendar.YEAR, -1);
                end.add(Calendar.YEAR, -1);
                break;
            default: // Last month
                start.add(Calendar.MONTH, -1);
                end.add(Calendar.MONTH, -1);
                break;
        }
        return new Calendar[]{start, end};
    }

    private void bindMonthData(List<Transaction> txs) {
        if (binding == null) return;

        double income = 0, expense = 0;
        Map<String, Double> byCat = new HashMap<>();
        for (Transaction t : txs) {
            if (Transaction.TYPE_INCOME.equals(t.getType())) income += t.getAmount();
            if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                expense += t.getAmount();
                String catId = t.getCategoryId();
                if (catId != null && !catId.isEmpty()) {
                    byCat.put(catId, byCat.getOrDefault(catId, 0.0) + t.getAmount());
                }
            }
        }

        binding.textTotalIncome.setText(MoneyFormat.format(income));
        binding.textTotalExpense.setText(MoneyFormat.format(expense));

        double lastMonthExpense = 0;
        for (Double v : lastMonthByCat.values()) lastMonthExpense += v;

        if (lastMonthExpense > 0) {
            double diff = expense - lastMonthExpense;
            String arrow = diff > 0 ? "▲ " : (diff < 0 ? "▼ " : "");
            String sign = diff > 0 ? "+" : "";
            String pctValue = String.format(java.util.Locale.getDefault(), "%.0f",
                    Math.abs(diff / lastMonthExpense * 100));
            String pct = getString(R.string.j3_vs_previous_period, sign, pctValue);
            binding.textComparison.setText(arrow + pct);
            int cmpColor = diff > 0 ? R.color.expense_red
                    : (diff < 0 ? R.color.income_green : R.color.text_secondary);
            binding.textComparison.setTextColor(
                    ContextCompat.getColor(requireContext(), cmpColor));
            binding.textComparison.setVisibility(View.VISIBLE);
        } else {
            binding.textComparison.setVisibility(View.GONE);
        }

        String topCat = findTopCategory(byCat);
        if (topCat != null) {
            double topAmount = byCat.get(topCat);
            Category cat = categoryMap.get(topCat);
            String catName = cat != null ? cat.getName() : topCat;
            binding.textTopCategory.setText(getString(R.string.j3_top_category_value,
                    catName, MoneyFormat.format(topAmount)));
        } else {
            binding.textTopCategory.setText(getString(R.string.j3_no_data));
        }

        bindPieChart(byCat);
        bindBarChart(income, expense);

        int txCount = 0;
        for (Transaction t : txs) {
            if (Transaction.TYPE_EXPENSE.equals(t.getType())) txCount++;
        }
        double avgExpense = txCount > 0 ? expense / txCount : 0;
        binding.textMonthlyAnalysis.setText(getString(R.string.j3_monthly_analysis,
                MoneyFormat.format(avgExpense)));
    }

    private String findTopCategory(Map<String, Double> byCat) {
        if (byCat.isEmpty()) return null;
        String top = null;
        double max = 0;
        for (Map.Entry<String, Double> e : byCat.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); top = e.getKey(); }
        }
        return top;
    }

    private void bindPieChart(Map<String, Double> byCat) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(byCat.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (Map.Entry<String, Double> e : sorted) {
            Category c = categoryMap.get(e.getKey());
            String name = c != null ? c.getName() : e.getKey();
            entries.add(new PieEntry(e.getValue().floatValue(), name));
            if (c != null && c.getColorHex() != null) {
                try { colors.add(Color.parseColor(c.getColorHex())); }
                catch (Exception ex) { colors.add(Color.LTGRAY); }
            } else {
                colors.add(Color.LTGRAY);
            }
        }

        if (entries.isEmpty()) {
            binding.pieChart.clear();
            binding.pieChart.invalidate();
            return;
        }

        double total = 0;
        for (Double v : byCat.values()) total += v;

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(colors);
        set.setValueTextSize(11f);
        set.setValueTextColor(Color.WHITE);
        set.setSliceSpace(2f);

        PieData data = new PieData(set);
        binding.pieChart.setData(data);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.setHoleRadius(62f);
        binding.pieChart.setTransparentCircleRadius(66f);
        binding.pieChart.setDrawEntryLabels(false);

        // Tổng chi ở giữa donut để không bị trống
        binding.pieChart.setDrawCenterText(true);
        binding.pieChart.setCenterText(MoneyFormat.format(total));
        binding.pieChart.setCenterTextSize(13f);
        binding.pieChart.setCenterTextColor(
                getResources().getColor(R.color.text_primary, null));

        Legend legend = binding.pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(getResources().getColor(R.color.text_primary, null));
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(false);
        legend.setTextSize(11f);
        legend.setXEntrySpace(8f);
        legend.setYEntrySpace(6f);

        binding.pieChart.setExtraOffsets(6, 4, 6, 4);
        binding.pieChart.animateY(800);
        binding.pieChart.invalidate();
    }

    private void bindBarChart(double income, double expense) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) income));
        entries.add(new BarEntry(1, (float) expense));

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(
                getResources().getColor(R.color.income_green, null),
                getResources().getColor(R.color.expense_red, null));
        set.setValueTextSize(12f);
        set.setValueTextColor(getResources().getColor(R.color.text_primary, null));

        BarData data = new BarData(set);
        data.setBarWidth(0.4f);
        binding.barChart.setData(data);
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.getXAxis().setEnabled(false);
        binding.barChart.getAxisLeft().setTextColor(
                getResources().getColor(R.color.text_secondary, null));
        binding.barChart.getAxisRight().setEnabled(false);
        binding.barChart.getLegend().setEnabled(false);
        binding.barChart.setFitBars(true);
        binding.barChart.setExtraOffsets(8, 8, 8, 8);
        binding.barChart.animateY(600);
        binding.barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
