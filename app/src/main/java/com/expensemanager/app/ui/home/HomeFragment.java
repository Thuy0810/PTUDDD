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
import com.expensemanager.app.data.model.FinancialInsights;
import com.expensemanager.app.data.model.HomeSummary;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.databinding.FragmentHomeBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.ui.adapter.TransactionAdapter;
import com.expensemanager.app.ui.category.CategoryListActivity;
import com.expensemanager.app.ui.goal.GoalListActivity;
import com.expensemanager.app.ui.recurring.RecurringListActivity;
import com.expensemanager.app.ui.transaction.AddTransactionActivity;
import com.expensemanager.app.ui.wallet.WalletListActivity;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.viewmodel.HomeViewModel;
import com.expensemanager.app.viewmodel.HomeViewModelHolder;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TransactionAdapter transactionAdapter;
    private final AuthRepository authRepo = new AuthRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private long lastMonthIncome = 0L;
    private long lastTotalBudget = 0L;

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

        binding.cardBudgetShortcut.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.budgetFragment));

        // Thao tac nhanh
        binding.qaWallets.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), WalletListActivity.class)));
        binding.qaGoals.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), GoalListActivity.class)));
        binding.qaRecurring.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RecurringListActivity.class)));
        binding.qaCategories.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CategoryListActivity.class)));

        // Tap so du -> Quan ly vi
        binding.layoutBalance.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), WalletListActivity.class)));

        // Nhac "giao viec cho tien" -> tab Ke hoach
        binding.textUnassignedHint.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.budgetFragment));
        String uidForBudget = authRepo.getUid();
        if (uidForBudget != null) {
            budgetRepo.observeMonth(uidForBudget, DateUtils.currentMonthKey())
                    .observe(getViewLifecycleOwner(), list -> {
                        long total = 0L;
                        if (list != null) {
                            for (Budget b : list) total += b.getLimitAmount();
                        }
                        lastTotalBudget = total;
                        updateUnassignedHint();
                    });
        }

        binding.textGreeting.setText(greetingByTime());

        authRepo.observeProfile().observe(getViewLifecycleOwner(), p -> {
            if (p == null || binding == null) return;
            String name = p.getDisplayName();
            if (name == null || name.trim().isEmpty()) {
                String email = p.getEmail();
                name = (email != null && email.contains("@"))
                        ? email.substring(0, email.indexOf('@'))
                        : getString(R.string.user);
            }
            binding.textUserName.setText(name);
            binding.textAvatarInitial.setText(
                    name.substring(0, 1).toUpperCase(Locale.getDefault()));
        });
    }

    /** Loi chao theo thoi diem trong ngay. */
    private String greetingByTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11) return getString(R.string.greeting_morning);
        if (hour < 13) return getString(R.string.greeting_noon);
        if (hour < 18) return getString(R.string.greeting_afternoon);
        return getString(R.string.greeting_evening);
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

        lastMonthIncome = (long) s.monthIncome;
        updateUnassignedHint();

        binding.textTodayExpense.setText(MoneyFormat.format(s.todayExpense));

        if (s.topCategoryName != null && !s.topCategoryName.isEmpty()) {
            binding.textTopCategory.setVisibility(View.VISIBLE);
            binding.textTopCategory.setText(s.topCategoryName);
        } else {
            binding.textTopCategory.setVisibility(View.GONE);
        }
    }

    /** Nhắc nếu thu nhập tháng còn chưa được phân bổ (giao việc) hết. */
    private void updateUnassignedHint() {
        if (binding == null) return;
        long unassigned = lastMonthIncome - lastTotalBudget;
        if (unassigned > 0) {
            binding.textUnassignedHint.setText(
                    getString(R.string.home_unassigned_hint, MoneyFormat.formatLong(unassigned)));
            binding.textUnassignedHint.setVisibility(View.VISIBLE);
        } else {
            binding.textUnassignedHint.setVisibility(View.GONE);
        }
    }

    private void bindInsights(FinancialInsights fi) {
        if (fi == null) return;
        bindBudgetShortcut(fi);
    }

    /** Loi tat ngan sach tren Trang chu — chi hien khi da dat ngan sach. */
    private void bindBudgetShortcut(FinancialInsights fi) {
        if (Double.isNaN(fi.budgetUsageRate) || fi.budgetUsageRate <= 0 || fi.expenseAmount <= 0) {
            binding.cardBudgetShortcut.setVisibility(View.GONE);
            return;
        }
        binding.cardBudgetShortcut.setVisibility(View.VISIBLE);

        long spent = fi.expenseAmount;
        long limit = Math.round(spent / fi.budgetUsageRate);
        int pct = (int) Math.round(fi.budgetUsageRate * 100);

        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        binding.textBudgetShortcutTitle.setText(
                getString(R.string.budget_shortcut_title, String.valueOf(month)));

        binding.progressBudgetShortcut.setProgress(Math.max(0, Math.min(100, pct)));

        int barColor = pct >= 100
                ? ContextCompat.getColor(requireContext(), R.color.expense_red)
                : pct >= 80
                        ? ContextCompat.getColor(requireContext(), R.color.warning)
                        : ContextCompat.getColor(requireContext(), R.color.income_green);
        binding.progressBudgetShortcut.setIndicatorColor(barColor);

        binding.textBudgetSpent.setText(
                getString(R.string.budget_spent_value, MoneyFormat.format(spent)));
        binding.textBudgetLimit.setText(
                getString(R.string.budget_limit_value, MoneyFormat.format(limit)));
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
