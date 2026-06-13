package com.expensemanager.app.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.FragmentHomeBinding;
import com.expensemanager.app.data.model.FinancialInsights;
import com.expensemanager.app.data.model.HomeSummary;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.viewmodel.HomeViewModel;
import com.expensemanager.app.viewmodel.HomeViewModelHolder;

import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

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

        viewModel.getSummary().observe(getViewLifecycleOwner(), this::bindSummary);
        viewModel.getInsights().observe(getViewLifecycleOwner(), this::bindInsights);
        viewModel.getBudgetAlerts().observe(getViewLifecycleOwner(), this::bindAlerts);

        binding.textSeeAll.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.transactionListFragment);
        });
    }

    private void bindSummary(HomeSummary s) {
        if (s == null) return;
        binding.textBalance.setText(MoneyFormat.format(s.totalBalance));
        binding.textIncome.setText(MoneyFormat.format(s.monthIncome));
        binding.textExpense.setText(MoneyFormat.format(s.monthExpense));
        if (s.todayExpense > 0) {
            binding.textTodayExpense.setText("Chi tiêu hôm nay: " + MoneyFormat.format(s.todayExpense));
        } else {
            binding.textTodayExpense.setText("");
        }
        if (s.topCategoryName != null && !s.topCategoryName.isEmpty()) {
            binding.textTopCategory.setText("Chi nhiều nhất: " + s.topCategoryName
                    + " - " + MoneyFormat.format(s.topCategoryAmount));
        } else {
            binding.textTopCategory.setText("");
        }
    }

    private void bindInsights(FinancialInsights fi) {
        if (fi == null) return;
        binding.textHealthScore.setText(fi.healthScore + "/100");
        binding.textHealthMsg.setText(fi.healthMessage);
        binding.textFeed.setText(fi.feedMessage);
        if (fi.dailyAllowance != null) binding.textDailyAllowance.setText(fi.dailyAllowance);
        if (fi.monthPrediction != null) binding.textPrediction.setText(fi.monthPrediction);
        if (fi.monthComparison != null) binding.textComparison.setText(fi.monthComparison);
    }

    private void bindAlerts(List<String> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            binding.cardAlerts.setVisibility(View.GONE);
            return;
        }
        binding.cardAlerts.setVisibility(View.VISIBLE);
        binding.textAlerts.setText(TextUtils.join("\n", alerts));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
