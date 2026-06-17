package com.expensemanager.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.FinancialAlertType;
import com.expensemanager.app.data.model.FinancialHealthStatus;
import com.expensemanager.app.data.model.FinancialInsights;
import com.expensemanager.app.data.model.HomeSummary;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.databinding.FragmentHomeBinding;
import com.expensemanager.app.ui.adapter.TransactionAdapter;
import com.expensemanager.app.ui.transaction.AddTransactionActivity;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.viewmodel.HomeViewModel;
import com.expensemanager.app.viewmodel.HomeViewModelHolder;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TransactionAdapter transactionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = HomeViewModelHolder.get();

        setupRecyclerView();

        viewModel.getSummary().observe(getViewLifecycleOwner(), this::bindSummary);
        viewModel.getInsights().observe(getViewLifecycleOwner(), this::bindInsights);
        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), this::bindRecentTransactions);
        viewModel.getCategoryMap().observe(getViewLifecycleOwner(), transactionAdapter::setCategoryMap);
        viewModel.getWalletMap().observe(getViewLifecycleOwner(), transactionAdapter::setWalletMap);

        binding.textSeeAll.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.reportFragment));

        binding.layoutHeader.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddTransactionActivity.class)));
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter();
        binding.recyclerRecentTransactions.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.recyclerRecentTransactions.setAdapter(transactionAdapter);
        transactionAdapter.setOnItemClick(t -> {
            Intent i = new Intent(requireContext(), AddTransactionActivity.class);
            i.putExtra("transaction_id", t.getId());
            startActivity(i);
        });
    }

    private void bindSummary(HomeSummary s) {
        if (s == null) return;
        binding.textBalance.setText(MoneyFormat.format(s.totalBalance));
        binding.textIncome.setText(MoneyFormat.format(s.monthIncome));
        binding.textExpense.setText(MoneyFormat.format(s.monthExpense));

        if (s.todayExpense > 0) {
            binding.textTodayExpense.setText(getString(
                    R.string.today_expense_value, MoneyFormat.format(s.todayExpense)));
        } else {
            binding.textTodayExpense.setText("");
        }

        if (s.topCategoryName != null && !s.topCategoryName.isEmpty()) {
            binding.textTopCategory.setText(getString(
                    R.string.top_category_value,
                    s.topCategoryName,
                    MoneyFormat.format(s.topCategoryAmount)));
        } else {
            binding.textTopCategory.setText("");
        }
    }

    private void bindInsights(FinancialInsights fi) {
        if (fi == null) return;

        binding.textHealthScore.setText(String.valueOf(fi.healthScore));

        // Trạng thái
        FinancialHealthStatus status = fi.status;
        if (status == null) status = FinancialHealthStatus.fromScore(fi.healthScore);
        binding.textHealthStatus.setText(getStatusLabel(status));

        // Màu score theo trạng thái
        int scoreColor;
        switch (status) {
            case EXCELLENT:
                scoreColor = ContextCompat.getColor(requireContext(), R.color.income_green);
                break;
            case GOOD:
                scoreColor = ContextCompat.getColor(requireContext(), R.color.primary);
                break;
            case WARNING:
                scoreColor = ContextCompat.getColor(requireContext(), R.color.warning);
                break;
            default:
                scoreColor = ContextCompat.getColor(requireContext(), R.color.expense_red);
                break;
        }
        binding.textHealthScore.setTextColor(scoreColor);

        // Tỷ lệ tiết kiệm
        if (fi.incomeAmount > 0 && !Double.isNaN(fi.savingRate)) {
            int pct = (int) Math.round(fi.savingRate * 100);
            binding.textSavingRate.setText(pct + "%");
            binding.textSavingRate.setTextColor(
                    pct >= 20
                            ? ContextCompat.getColor(requireContext(), R.color.income_green)
                            : ContextCompat.getColor(requireContext(), R.color.warning));
        } else {
            binding.textSavingRate.setText("--");
        }

        // Tỷ lệ sử dụng ngân sách
        if (!Double.isNaN(fi.budgetUsageRate) && fi.budgetUsageRate > 0) {
            int pct = (int) Math.round(fi.budgetUsageRate * 100);
            binding.textBudgetUsage.setText(pct + "%");
            binding.textBudgetUsage.setTextColor(
                    pct >= 100
                            ? ContextCompat.getColor(requireContext(), R.color.expense_red)
                            : pct >= 80
                                    ? ContextCompat.getColor(requireContext(), R.color.warning)
                                    : ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            binding.textBudgetUsage.setText("--");
        }

        // So với tháng trước
        if (!Double.isNaN(fi.expenseChangeRate) && fi.expenseChangeRate != 0) {
            int pct = (int) Math.round(Math.abs(fi.expenseChangeRate) * 100);
            String sign = fi.expenseChangeRate < 0 ? "-" : "+";
            binding.textExpenseChange.setText(sign + pct + "%");
            binding.textExpenseChange.setTextColor(
                    fi.expenseChangeRate < 0
                            ? ContextCompat.getColor(requireContext(), R.color.income_green)
                            : ContextCompat.getColor(requireContext(), R.color.expense_red));
        } else {
            binding.textExpenseChange.setText("--");
        }

        // Dự đoán
        if (fi.dailyAllowanceAmount > 0) {
            binding.textDailyAllowance.setText(
                    MoneyFormat.format(fi.dailyAllowanceAmount));
        } else {
            binding.textDailyAllowance.setText("--");
        }

        if (fi.predictedMonthExpense > 0) {
            binding.textPrediction.setText(
                    MoneyFormat.format(fi.predictedMonthExpense));
        } else {
            binding.textPrediction.setText("--");
        }

        // Alerts: chỉ hiển thị 1 cảnh báo quan trọng nhất
        if (fi.alerts != null && !fi.alerts.isEmpty()) {
            binding.cardAlerts.setVisibility(View.VISIBLE);
            FinancialAlertType top = fi.alerts.get(0);
            String alertLabel;
            if (top == FinancialAlertType.CASH_RUNOUT_RISK && fi.projectedRunOutDay > 0) {
                alertLabel = getString(R.string.alert_cash_runout, fi.projectedRunOutDay);
            } else {
                alertLabel = getAlertLabel(top);
            }
            binding.textAlerts.setText(alertLabel);
        } else {
            binding.cardAlerts.setVisibility(View.GONE);
        }
    }

    private String getAlertLabel(FinancialAlertType type) {
        if (type == null) return "";
        switch (type) {
            case OVER_BUDGET:
                return getString(R.string.alert_over_budget);
            case NEAR_BUDGET_LIMIT:
                return getString(R.string.alert_near_budget);
            case EXPENSE_INCREASED:
                return getString(R.string.alert_expense_increased);
            case MISSING_RECORDS:
                return getString(R.string.alert_missing_records);
            case ABNORMAL_SPENDING:
                return getString(R.string.alert_abnormal_spending);
            case CASH_RUNOUT_RISK:
                return getString(R.string.alert_cash_runout_generic);
            default:
                return "";
        }
    }

    private String getStatusLabel(FinancialHealthStatus status) {
        switch (status) {
            case EXCELLENT:  return getString(R.string.health_excellent);
            case GOOD:      return getString(R.string.health_good);
            case WARNING:    return getString(R.string.health_warning);
            default:         return getString(R.string.health_critical);
        }
    }

    private void bindRecentTransactions(List<Transaction> txs) {
        if (txs == null || txs.isEmpty()) {
            binding.textEmpty.setVisibility(View.GONE);
            binding.recyclerRecentTransactions.setVisibility(View.GONE);
            return;
        }
        binding.textEmpty.setVisibility(View.GONE);
        binding.recyclerRecentTransactions.setVisibility(View.VISIBLE);
        transactionAdapter.setItems(txs);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
