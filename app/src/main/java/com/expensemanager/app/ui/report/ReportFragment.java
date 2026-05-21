package com.expensemanager.app.ui.report;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.FragmentReportBinding;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportFragment extends Fragment {
    private FragmentReportBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private Map<String, Category> categoryMap = new HashMap<>();
    private double lastMonthExpense = 0;

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
        String uid = authRepo.getUid();
        if (uid == null) return;

        CategoryRepository catRepo = new CategoryRepository();
        TransactionRepository txRepo = new TransactionRepository();
        WalletRepository walletRepo = new WalletRepository();

        catRepo.observeAll(uid).observe(getViewLifecycleOwner(), categories -> {
            categoryMap = CategoryRepository.toMap(categories);

            txRepo.observeMonth(uid, DateUtils.currentMonthKey())
                    .observe(getViewLifecycleOwner(), txs -> {
                        if (txs == null) txs = new ArrayList<>();
                        bindMonthData(txs);
                    });

            String lastMonth = getLastMonthKey();
            txRepo.observeMonth(uid, lastMonth)
                    .observe(getViewLifecycleOwner(), lastTxs -> {
                        if (lastTxs == null) lastTxs = new ArrayList<>();
                        double expense = 0;
                        for (Transaction t : lastTxs) {
                            if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                                expense += t.getAmount();
                            }
                        }
                        lastMonthExpense = expense;
                    });
        });

        walletRepo.observeAll(uid).observe(getViewLifecycleOwner(), wallets -> {
            double totalBalance = 0;
            for (Wallet w : wallets) {
                totalBalance += w.getCurrentBalance();
            }
            binding.textTotalBalance.setText(MoneyFormat.format(totalBalance));
        });
    }

    private String getLastMonthKey() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        return DateUtils.monthKey(cal.getTime());
    }

    private void bindMonthData(List<Transaction> txs) {
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
        binding.textTotalBalance.setText(MoneyFormat.format(income - expense));

        // Comparison
        if (lastMonthExpense > 0) {
            double diff = expense - lastMonthExpense;
            String sign = diff > 0 ? "+" : "";
            String pct = String.format("%s%.0f%% so với tháng trước",
                    sign, (diff / lastMonthExpense * 100));
            binding.textComparison.setText(pct);
        }

        // Top category
        String topCat = findTopCategory(byCat);
        if (topCat != null) {
            double topAmount = byCat.get(topCat);
            Category cat = categoryMap.get(topCat);
            String catName = cat != null ? cat.getName() : topCat;
            binding.textTopCategory.setText(catName + ": " + MoneyFormat.format(topAmount));
        }

        // Pie chart
        bindPieChart(byCat);

        // Bar chart (thu vs chi)
        bindBarChart(income, expense);

        // Monthly analysis
        binding.textMonthlyAnalysis.setText("Chi tiêu trung bình: "
                + MoneyFormat.format(txCount(txs, Transaction.TYPE_EXPENSE) > 0
                    ? expense / txCount(txs, Transaction.TYPE_EXPENSE) : 0));
    }

    private int txCount(List<Transaction> txs, String type) {
        int count = 0;
        for (Transaction t : txs) {
            if (type.equals(t.getType())) count++;
        }
        return count;
    }

    private String findTopCategory(Map<String, Double> byCat) {
        if (byCat.isEmpty()) return null;
        String top = null;
        double max = 0;
        for (Map.Entry<String, Double> e : byCat.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                top = e.getKey();
            }
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
        binding.pieChart.getLegend().setTextColor(getResources().getColor(R.color.text_primary, null));
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
        binding.barChart.getAxisLeft().setTextColor(getResources().getColor(R.color.text_secondary, null));
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
