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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.FragmentReportBinding;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.data.model.FinancialInsights;
import com.expensemanager.app.ui.adapter.TransactionAdapter;
import com.expensemanager.app.ui.transaction.AddTransactionActivity;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.InsightsEngine;
import com.expensemanager.app.util.MoneyFormat;
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
import java.util.List;
import java.util.Map;

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
            String sign = diff > 0 ? "+" : "";
            String pctValue = String.format(java.util.Locale.getDefault(), "%.0f",
                    Math.abs(diff / lastMonthExpense * 100));
            String pct = getString(R.string.j3_vs_previous_period, sign, pctValue);
            binding.textComparison.setText(pct);
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
                getResources().getColor(R.color.text_primary, null));
        binding.pieChart.getLegend().setWordWrapEnabled(true);
        binding.pieChart.setExtraOffsets(8, 8, 8, 8);
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
