package com.expensemanager.app.ui.budget;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.databinding.FragmentBudgetBinding;
import com.expensemanager.app.domain.usecase.BudgetService;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyInputFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetFragment extends Fragment {
    private FragmentBudgetBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private BudgetAdapter adapter;
    private List<Category> allCategories = new ArrayList<>();
    private List<Budget> currentBudgets = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String uid = authRepo.getUid();
        if (uid == null) return;

        setupRecyclerView();
        binding.textMonthLabel.setText(DateUtils.currentMonthLabel());
        setupTabs();

        observeCategories(uid);
        observeBudgets(uid);
        observeExpenses(uid);
    }

    private void setupRecyclerView() {
        adapter = new BudgetAdapter();
        binding.recyclerBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBudgets.setAdapter(adapter);

        adapter.setOnItemClick((budget, cat) -> showEditDialog(budget, cat));
        binding.recyclerBudgets.setOnLongClickListener(v -> {
            showAddDialog(null);
            return true;
        });
    }

    private void setupTabs() {
        updateTabUI(true);

        binding.tabOverview.setOnClickListener(v -> updateTabUI(true));
        binding.tabExpense.setOnClickListener(v -> updateTabUI(false));

        binding.btnAllocate.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), BudgetAllocationActivity.class);
            i.putExtra(BudgetAllocationActivity.EXTRA_MONTH_KEY, DateUtils.currentMonthKey());
            startActivity(i);
        });
    }

    private void updateTabUI(boolean showOverview) {
        if (showOverview) {
            binding.layoutOverview.setVisibility(View.VISIBLE);
            binding.layoutExpense.setVisibility(View.GONE);
            binding.tabOverview.setBackgroundResource(R.drawable.bg_tab_active);
            binding.tabOverview.setTextColor(requireContext().getColor(R.color.text_primary));
            binding.tabOverview.setTypeface(null, android.graphics.Typeface.BOLD);
            binding.tabExpense.setBackgroundResource(R.drawable.bg_chip_inactive);
            binding.tabExpense.setTextColor(requireContext().getColor(R.color.text_secondary));
            binding.tabExpense.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            binding.layoutOverview.setVisibility(View.GONE);
            binding.layoutExpense.setVisibility(View.VISIBLE);
            binding.tabExpense.setBackgroundResource(R.drawable.bg_tab_active);
            binding.tabExpense.setTextColor(requireContext().getColor(R.color.text_primary));
            binding.tabExpense.setTypeface(null, android.graphics.Typeface.BOLD);
            binding.tabOverview.setBackgroundResource(R.drawable.bg_chip_inactive);
            binding.tabOverview.setTextColor(requireContext().getColor(R.color.text_secondary));
            binding.tabOverview.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void observeCategories(String uid) {
        categoryRepo.observeAll(uid).observe(getViewLifecycleOwner(), list -> {
            allCategories = list != null ? list : new ArrayList<>();
            Map<String, Category> map = new HashMap<>();
            for (Category c : allCategories) map.put(c.getId(), c);
            adapter.setCategoryMap(map);
        });
    }

    private void observeBudgets(String uid) {
        budgetRepo.observeMonth(uid, DateUtils.currentMonthKey()).observe(getViewLifecycleOwner(), list -> {
            currentBudgets = list != null ? list : new ArrayList<>();
            adapter.setItems(currentBudgets);
            updateTotalSummary();
        });
    }

    private void observeExpenses(String uid) {
        String monthKey = DateUtils.currentMonthKey();
        txRepo.observeMonth(uid, monthKey).observe(getViewLifecycleOwner(), txs -> {
            Map<String, Long> spentMap = new HashMap<>();
            long totalSpent = 0L;
            if (txs != null) {
                for (Transaction t : txs) {
                    if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                        totalSpent += t.getAmount();
                        String catId = t.getCategoryId();
                        if (catId != null && !catId.isEmpty()) {
                            long prev = spentMap.containsKey(catId) ? spentMap.get(catId) : 0L;
                            spentMap.put(catId, prev + t.getAmount());
                        }
                    }
                }
            }
            adapter.setSpentMap(spentMap);
            updateTotalSummaryWithSpent(spentMap, totalSpent);
        });
    }

    private void updateTotalSummary() {
        long totalBudget = 0L;
        for (Budget b : currentBudgets) {
            totalBudget += b.getLimitAmount();
        }
        binding.textTotalBudget.setText(MoneyFormat.formatLong(totalBudget));
    }

    private void updateTotalSummaryWithSpent(Map<String, Long> spentMap, long totalSpent) {
        long totalBudget = 0L;
        for (Budget b : currentBudgets) {
            totalBudget += b.getLimitAmount();
        }
        long totalRemaining = totalBudget - totalSpent;
        int percent = totalBudget > 0 ? (int) (totalSpent * 100 / totalBudget) : 0;

        binding.textTotalBudget.setText(MoneyFormat.formatLong(totalBudget));
        binding.textTotalSpent.setText(MoneyFormat.formatLong(totalSpent));
        binding.textTotalRemaining.setText(
                MoneyFormat.formatLong(Math.max(totalRemaining, 0L)));
        binding.progressTotal.setProgress(Math.min(percent, 100));

        if (totalRemaining < 0) {
            binding.textTotalRemaining.setTextColor(
                    requireContext().getColor(R.color.expense_red));
        }
    }

    private void showAddDialog(Category preselected) {
        if (allCategories.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.j1_no_category), Toast.LENGTH_SHORT).show();
            return;
        }

        final Category[] selectedCat = {preselected};
        String[] names = new String[allCategories.size()];
        for (int i = 0; i < allCategories.size(); i++) {
            names[i] = allCategories.get(i).getName();
        }

        int selectedIndex = preselected != null ? allCategories.indexOf(preselected) : 0;
        final EditText inputAmount = new EditText(requireContext());
        inputAmount.setHint(getString(R.string.j1_limit_vnd_hint));
        inputAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(inputAmount);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.j1_set_budget_by_category))
                .setSingleChoiceItems(names, selectedIndex, (d, which) -> {
                    selectedCat[0] = allCategories.get(which);
                })
                .setView(inputAmount)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    if (selectedCat[0] == null) {
                        Toast.makeText(requireContext(), getString(R.string.select_category), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long parsed = MoneyInputFormatter.getRawValue(inputAmount);
                    if (parsed <= 0) {
                        Toast.makeText(requireContext(), getString(R.string.j1_enter_valid_amount), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveBudget(selectedCat[0], parsed);
                })
                .setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void showEditDialog(Budget budget, Category cat) {
        final EditText input = new EditText(requireContext());
        input.setHint(getString(R.string.j1_new_limit_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(input);
        input.setText(MoneyInputFormatter.format(budget.getLimitAmount()));

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.j1_edit_budget_for, (cat != null ? cat.getName() : "")))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    long parsed = MoneyInputFormatter.getRawValue(input);
                    if (parsed <= 0) {
                        Toast.makeText(requireContext(), getString(R.string.error_invalid_amount),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String uid = authRepo.getUid();
                    if (uid != null) {
                        budgetRepo.updateLimitAmount(uid, budget.getId(), parsed);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .setNeutralButton(getString(R.string.delete), (d, w) -> {
                    String uid = authRepo.getUid();
                    if (uid != null) budgetRepo.delete(uid, budget.getId());
                })
                .show();
    }

    private void saveBudget(Category cat, long limit) {
        String uid = authRepo.getUid();
        if (uid == null) return;

        Budget b = new Budget();
        b.setScope(Budget.SCOPE_CATEGORY);
        b.setCategoryId(cat.getId());
        b.setMonth(DateUtils.currentMonthKey());
        b.setLimitAmount(limit);
        budgetRepo.addOrUpdate(uid, b);
        Toast.makeText(requireContext(), getString(R.string.j1_budget_saved), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
