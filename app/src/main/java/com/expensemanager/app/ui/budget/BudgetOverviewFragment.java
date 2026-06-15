package com.expensemanager.app.ui.budget;

import android.app.DatePickerDialog;
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
import com.expensemanager.app.data.model.RecurringRule;
import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.GoalRepository;
import com.expensemanager.app.data.repository.RecurringRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.FragmentBudgetOverviewBinding;
import com.expensemanager.app.ui.budget.BudgetAllocationActivity;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyInputFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetOverviewFragment extends Fragment {
    private FragmentBudgetOverviewBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private final GoalRepository goalRepo = new GoalRepository();
    private final RecurringRepository recurringRepo = new RecurringRepository();

    private BudgetSectionAdapter adapter;
    private List<BudgetSectionAdapter.Section> sections = new ArrayList<>();
    private Map<String, Long> allocatedMap = new HashMap<>();
    private Map<String, Long> spentMap = new HashMap<>();

    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private int selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

    private List<Category> allCategories = new ArrayList<>();
    private List<RecurringRule> recurringRules = new ArrayList<>();
    private List<SavingsGoal> savingsGoals = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupClickListeners();
        updateMonthLabel();
        loadData();
    }

    private void setupRecyclerView() {
        adapter = new BudgetSectionAdapter();
        binding.recyclerSections.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSections.setAdapter(adapter);

        adapter.setOnCategoryEdit((category, currentAmount) -> showEditDialog(category, currentAmount));
    }

    private void setupClickListeners() {
        binding.btnSettings.setOnClickListener(v -> {
            String uid = authRepo.getUid();
            if (uid != null) {
                BudgetSettingsDialog dialog = new BudgetSettingsDialog(requireContext(), uid);
                dialog.show();
            }
        });

        binding.layoutMonth.setOnClickListener(v -> showMonthPicker());

        binding.btnAllocate.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), BudgetAllocationActivity.class);
            i.putExtra(BudgetAllocationActivity.EXTRA_MONTH_KEY, String.format("%04d-%02d", selectedYear, selectedMonth));
            startActivity(i);
        });
    }

    private void showMonthPicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month + 1;
                    updateMonthLabel();
                    loadData();
                },
                selectedYear,
                selectedMonth - 1,
                1
        );
        dialog.show();
    }

    private void updateMonthLabel() {
        String label = getString(R.string.j1_month_label, selectedMonth);
        binding.textMonth.setText(label);
    }

    private void loadData() {
        String uid = authRepo.getUid();
        if (uid == null) return;

        String monthKey = String.format("%04d-%02d", selectedYear, selectedMonth);

        // Load wallet balance
        WalletRepository walletRepo = new WalletRepository();
        walletRepo.observeAll(uid).observe(getViewLifecycleOwner(), wallets -> {
            long totalBalance = 0L;
            if (wallets != null) {
                for (Wallet w : wallets) {
                    totalBalance += w.getCurrentBalance();
                }
            }
            adapter.setTotalBalance(totalBalance);
        });

        // Load recurring rules
        recurringRepo.observeAll(uid).observe(getViewLifecycleOwner(), list -> {
            recurringRules = list != null ? list : new ArrayList<>();
            calculateAndUpdateSummary();
        });

        // Load savings goals
        goalRepo.observeAll(uid).observe(getViewLifecycleOwner(), list -> {
            savingsGoals = list != null ? list : new ArrayList<>();
            calculateAndUpdateSummary();
        });

        categoryRepo.observeAll(uid).observe(getViewLifecycleOwner(), list -> {
            allCategories = list != null ? list : new ArrayList<>();
            buildSections();
        });

        budgetRepo.observeMonth(uid, monthKey).observe(getViewLifecycleOwner(), list -> {
            allocatedMap = new HashMap<>();
            if (list != null) {
                for (Budget b : list) {
                    if (b.getCategoryId() != null) {
                        allocatedMap.put(b.getCategoryId(), b.getLimitAmount());
                    }
                }
            }
            adapter.setAllocatedMap(allocatedMap);
            calculateAndUpdateSummary();
        });

        txRepo.observeMonth(uid, monthKey).observe(getViewLifecycleOwner(), txs -> {
            spentMap = new HashMap<>();
            long totalExpense = 0L, totalIncome = 0L;
            if (txs != null) {
                for (Transaction t : txs) {
                    String catId = t.getCategoryId();
                    if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                        totalExpense += t.getAmount();
                        if (catId != null) {
                            long prev = spentMap.containsKey(catId) ? spentMap.get(catId) : 0L;
                            spentMap.put(catId, prev + t.getAmount());
                        }
                    } else if (Transaction.TYPE_INCOME.equals(t.getType())) {
                        totalIncome += t.getAmount();
                    }
                }
            }
            adapter.setSpentMap(spentMap);
            updateSummaryWithAmounts(totalIncome, totalExpense);
        });
    }

    private void calculateAndUpdateSummary() {
        // Calculate recurring expenses for the month
        long recurringExpense = 0L;
        for (RecurringRule rule : recurringRules) {
            if (rule.isEnabled() && Transaction.TYPE_EXPENSE.equals(rule.getType())) {
                String cycle = rule.getCycleType();
                if (cycle == null) cycle = RecurringRule.CYCLE_MONTHLY;

                switch (cycle) {
                    case RecurringRule.CYCLE_DAILY:
                        recurringExpense += rule.getAmount() * 30;
                        break;
                    case RecurringRule.CYCLE_WEEKLY:
                        recurringExpense += rule.getAmount() * 4;
                        break;
                    case RecurringRule.CYCLE_MONTHLY:
                        recurringExpense += rule.getAmount();
                        break;
                    case RecurringRule.CYCLE_YEARLY:
                        recurringExpense += rule.getAmount() / 12;
                        break;
                }
            }
        }

        // Calculate savings goal contributions needed
        long savingsNeeded = 0L;
        for (SavingsGoal goal : savingsGoals) {
            if (!goal.isCompleted()) {
                long remaining = goal.getTargetAmount() - goal.getSavedAmount();
                if (remaining > 0) {
                    Calendar deadline = DateUtils.newCalendar();
                    if (goal.getDeadline() != null) {
                        deadline.setTime(goal.getDeadline().toDate());
                    }
                    int monthsRemaining = (deadline.get(Calendar.YEAR) - selectedYear) * 12
                            + (deadline.get(Calendar.MONTH) + 1 - selectedMonth);
                    if (monthsRemaining > 0) {
                        savingsNeeded += remaining / monthsRemaining;
                    } else {
                        savingsNeeded += remaining;
                    }
                }
            }
        }

        // Update UI with calculated values
        binding.textTotalExpense.setText(MoneyFormat.formatLong(recurringExpense));
        binding.textExtraSaving.setText(MoneyFormat.formatLong(savingsNeeded));
    }

    private void buildSections() {
        Map<String, BudgetSectionAdapter.Section> sectionMap = new HashMap<>();

        BudgetSectionAdapter.Section essential = new BudgetSectionAdapter.Section(getString(R.string.j1_group_essential));
        BudgetSectionAdapter.Section needs = new BudgetSectionAdapter.Section(getString(R.string.j1_group_need));
        BudgetSectionAdapter.Section wants = new BudgetSectionAdapter.Section(getString(R.string.j1_group_want));
        BudgetSectionAdapter.Section other = new BudgetSectionAdapter.Section(getString(R.string.j1_group_other));

        sectionMap.put("essential", essential);
        sectionMap.put("need", needs);
        sectionMap.put("want", wants);
        sectionMap.put("other", other);

        for (Category cat : allCategories) {
            if (!Category.TYPE_EXPENSE.equals(cat.getType())) continue;

            BudgetSectionAdapter.Section target;
            String group = cat.getGroup();
            if ("essential".equals(group)) {
                target = essential;
            } else if ("need".equals(group)) {
                target = needs;
            } else if ("want".equals(group)) {
                target = wants;
            } else {
                target = other;
            }

            BudgetSectionAdapter.CategoryItem item = new BudgetSectionAdapter.CategoryItem();
            item.category = cat;
            target.categories.add(item);
        }

        sections = new ArrayList<>();
        if (!essential.categories.isEmpty()) sections.add(essential);
        if (!needs.categories.isEmpty()) sections.add(needs);
        if (!wants.categories.isEmpty()) sections.add(wants);
        if (!other.categories.isEmpty()) sections.add(other);

        adapter.setSections(sections);
    }

    private void updateSummaryWithAmounts(long totalIncome, long totalExpense) {
        binding.textTotalExpense.setText(MoneyFormat.formatLong(totalExpense));
        binding.textTotalIncome.setText(MoneyFormat.formatLong(totalIncome));

        long saved = totalIncome - totalExpense;
        binding.textTotalSaving.setText(MoneyFormat.formatLong(Math.max(saved, 0L)));

        long extraSaving = totalIncome - totalExpense;
        if (extraSaving > 0) {
            binding.textExtraSaving.setText(MoneyFormat.formatLong(extraSaving));
        } else {
            binding.textExtraSaving.setText(MoneyFormat.formatLong(0L));
        }
    }

    private void showEditDialog(Category cat, long currentAmount) {
        EditText input = new EditText(requireContext());
        input.setHint(getString(R.string.j1_allocation_amount_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(input);
        if (currentAmount > 0) {
            input.setText(MoneyInputFormatter.format(currentAmount));
        }

        String monthKey = String.format("%04d-%02d", selectedYear, selectedMonth);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.j1_allocate_to, cat.getName()))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    long parsed = MoneyInputFormatter.getRawValue(input);
                    if (parsed <= 0) {
                        Toast.makeText(requireContext(), getString(R.string.error_invalid_amount),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveBudget(cat, parsed, monthKey);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void saveBudget(Category cat, long amount, String monthKey) {
        String uid = authRepo.getUid();
        if (uid == null) return;

        Budget b = new Budget();
        b.setScope(Budget.SCOPE_CATEGORY);
        b.setCategoryId(cat.getId());
        b.setMonth(monthKey);
        b.setLimitAmount(amount);
        budgetRepo.addOrUpdate(uid, b);
        Toast.makeText(requireContext(), getString(R.string.j1_budget_saved), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
