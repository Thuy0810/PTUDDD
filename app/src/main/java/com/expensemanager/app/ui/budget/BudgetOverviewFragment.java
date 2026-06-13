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
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.databinding.FragmentBudgetOverviewBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

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

    private BudgetSectionAdapter adapter;
    private List<BudgetSectionAdapter.Section> sections = new ArrayList<>();
    private Map<String, Double> allocatedMap = new HashMap<>();
    private Map<String, Double> spentMap = new HashMap<>();

    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private int selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

    // Cached all categories
    private List<Category> allCategories = new ArrayList<>();

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
            startActivity(new Intent(requireContext(), BudgetAllocationActivity.class));
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
        String label = "Tháng " + selectedMonth;
        binding.textMonth.setText(label);
    }

    private void loadData() {
        String uid = authRepo.getUid();
        if (uid == null) return;

        String monthKey = String.format("%04d-%02d", selectedYear, selectedMonth);

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
            updateSummary();
        });

        txRepo.observeMonth(uid, monthKey).observe(getViewLifecycleOwner(), txs -> {
            spentMap = new HashMap<>();
            double totalExpense = 0, totalIncome = 0;
            if (txs != null) {
                for (Transaction t : txs) {
                    String catId = t.getCategoryId();
                    if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                        totalExpense += t.getAmount();
                        if (catId != null) {
                            double prev = spentMap.containsKey(catId) ? spentMap.get(catId) : 0;
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

    private void buildSections() {
        // Group categories
        Map<String, BudgetSectionAdapter.Section> sectionMap = new HashMap<>();

        BudgetSectionAdapter.Section essential = new BudgetSectionAdapter.Section("Thiết yếu");
        BudgetSectionAdapter.Section needs = new BudgetSectionAdapter.Section("Nhu cầu");
        BudgetSectionAdapter.Section wants = new BudgetSectionAdapter.Section("Khoản muốn có");
        BudgetSectionAdapter.Section other = new BudgetSectionAdapter.Section("Khác");

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

    private void updateSummary() {
        double totalExpense = 0, totalIncome = 0;
        for (Map.Entry<String, Double> e : spentMap.entrySet()) {
            totalExpense += e.getValue();
        }
        binding.textTotalExpense.setText(MoneyFormat.format(totalExpense));
        binding.textTotalIncome.setText(MoneyFormat.format(totalIncome));

        double saved = totalIncome - totalExpense;
        binding.textTotalSaving.setText(MoneyFormat.format(Math.max(saved, 0)));

        double extraSaving = totalIncome - totalExpense;
        if (extraSaving > 0) {
            binding.textExtraSaving.setText(MoneyFormat.format(extraSaving));
        } else {
            binding.textExtraSaving.setText("0đ");
        }
    }

    private void updateSummaryWithAmounts(double totalIncome, double totalExpense) {
        binding.textTotalExpense.setText(MoneyFormat.format(totalExpense));
        binding.textTotalIncome.setText(MoneyFormat.format(totalIncome));

        double saved = totalIncome - totalExpense;
        binding.textTotalSaving.setText(MoneyFormat.format(Math.max(saved, 0)));

        double extraSaving = totalIncome - totalExpense;
        if (extraSaving > 0) {
            binding.textExtraSaving.setText(MoneyFormat.format(extraSaving));
        } else {
            binding.textExtraSaving.setText("0đ");
        }
    }

    private void showEditDialog(Category cat, double currentAmount) {
        EditText input = new EditText(requireContext());
        input.setHint("Số tiền phân bổ");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (currentAmount > 0) {
            input.setText(String.valueOf((long) currentAmount));
        }

        String monthKey = String.format("%04d-%02d", selectedYear, selectedMonth);

        new AlertDialog.Builder(requireContext())
                .setTitle("Phân bổ: " + cat.getName())
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString().trim());
                        if (amount < 0) throw new Exception();
                        saveBudget(cat, amount, monthKey);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveBudget(Category cat, double amount, String monthKey) {
        String uid = authRepo.getUid();
        if (uid == null) return;

        Budget b = new Budget();
        b.setScope(Budget.SCOPE_CATEGORY);
        b.setCategoryId(cat.getId());
        b.setMonth(monthKey);
        b.setLimitAmount(amount);
        budgetRepo.addOrUpdate(uid, b);
        Toast.makeText(requireContext(), "Da luu ngan sach", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
