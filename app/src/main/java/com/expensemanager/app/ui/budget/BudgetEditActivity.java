package com.expensemanager.app.ui.budget;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.databinding.ActivityBudgetEditBinding;
import com.expensemanager.app.databinding.ItemBudgetAllocationBinding;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyInputFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetEditActivity extends AppCompatActivity {

    private ActivityBudgetEditBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();

    private List<Category> expenseCategories = new ArrayList<>();
    private List<Budget> budgets = new ArrayList<>();
    private Map<String, Long> categoryBudgets = new HashMap<>();
    private long totalBudget = 0L;
    private String monthKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBudgetEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        monthKey = getIntent().getStringExtra("monthKey");
        if (monthKey == null) monthKey = "";

        setupRecyclerView();
        setupClickListeners();
        loadData();
    }

    private void setupRecyclerView() {
        // recyclerCategories là LinearLayout (vertical); item được inflate + addView trực tiếp.
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirm());
        binding.btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());
        binding.btnSave.setOnClickListener(v -> saveAll());
    }

    private void loadData() {
        String uid = authRepo.getUid();
        if (uid == null) return;

        categoryRepo.observeAll(uid).observe(this, list -> {
            expenseCategories = new ArrayList<>();
            if (list != null) {
                for (Category c : list) {
                    if (Category.TYPE_EXPENSE.equals(c.getType())) {
                        expenseCategories.add(c);
                    }
                }
            }
            updateTotalBudget();
            updateRecyclerView();
        });

        budgetRepo.observeMonth(uid, monthKey).observe(this, list -> {
            budgets = list != null ? list : new ArrayList<>();
            categoryBudgets = new HashMap<>();
            totalBudget = 0L;
            for (Budget b : budgets) {
                if (b.getCategoryId() != null) {
                    categoryBudgets.put(b.getCategoryId(), b.getLimitAmount());
                    totalBudget += b.getLimitAmount();
                }
            }
            updateTotalBudget();
            updateRecyclerView();
        });
    }

    private void updateTotalBudget() {
        binding.textTotalBudget.setText(MoneyFormat.formatLong(totalBudget));
    }

    private void updateRecyclerView() {
        binding.recyclerCategories.removeAllViews();

        for (Category cat : expenseCategories) {
            View itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_budget_allocation, binding.recyclerCategories, false);

            ItemBudgetAllocationBinding itemBinding = ItemBudgetAllocationBinding.bind(itemView);

            long amount = categoryBudgets.containsKey(cat.getId())
                    ? categoryBudgets.get(cat.getId()) : 0L;
            int progress = totalBudget > 0 ? (int) (amount * 100 / totalBudget) : 0;
            progress = Math.min(progress, 100);

            itemBinding.textCategoryName.setText(cat.getName());
            itemBinding.textAllocatedAmount.setText(MoneyFormat.formatLong(amount));
            itemBinding.textPercent.setText(progress + "%");
            itemBinding.progressBar.setProgress(progress);

            if (cat.getIconKey() != null) {
                itemBinding.textCategoryIcon.setText(getCategoryEmoji(cat.getIconKey()));
            }

            try {
                int color = Color.parseColor(cat.getColorHex());
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(color);
                itemBinding.viewCategoryBg.setBackground(bg);
            } catch (Exception ignored) {}

            itemBinding.btnEdit.setOnClickListener(v -> showEditDialog(cat, amount));
            itemBinding.getRoot().setOnClickListener(v -> showEditDialog(cat, amount));

            binding.recyclerCategories.addView(itemView);
        }
    }

    private void showEditDialog(Category cat, long currentAmount) {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.j1_allocation_amount_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(input);
        if (currentAmount > 0) {
            input.setText(MoneyInputFormatter.format(currentAmount));
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j1_allocate_to, cat.getName()))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    long parsed = MoneyInputFormatter.getRawValue(input);
                    if (parsed <= 0) {
                        Toast.makeText(this, getString(R.string.error_invalid_amount),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveCategoryBudget(cat, parsed);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void saveCategoryBudget(Category cat, long amount) {
        String uid = authRepo.getUid();
        if (uid == null) return;

        Budget existing = null;
        for (Budget b : budgets) {
            if (cat.getId().equals(b.getCategoryId())) {
                existing = b;
                break;
            }
        }

        if (existing != null) {
            budgetRepo.updateLimitAmount(uid, existing.getId(), amount,
                    a -> Toast.makeText(this, getString(R.string.j1_updated), Toast.LENGTH_SHORT).show(),
                    null);
        } else {
            Budget b = new Budget();
            b.setScope(Budget.SCOPE_CATEGORY);
            b.setCategoryId(cat.getId());
            b.setMonth(monthKey);
            b.setLimitAmount(amount);
            budgetRepo.addOrUpdate(uid, b);
        }
    }

    private void showCreateGroupDialog() {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.j1_group_name_hint));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j1_create_category_group))
                .setView(input)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.j1_group_created, name), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j1_delete_budget_title))
                .setMessage(getString(R.string.j1_delete_all_budget_confirm))
                .setPositiveButton(getString(R.string.delete), (d, w) -> {
                    String uid = authRepo.getUid();
                    if (uid != null) {
                        for (Budget b : budgets) {
                            budgetRepo.delete(uid, b.getId());
                        }
                        Toast.makeText(this, getString(R.string.j1_budget_deleted), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void saveAll() {
        Toast.makeText(this, getString(R.string.j1_budget_saved), Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getCategoryEmoji(String iconKey) {
        if (iconKey == null) return "📦";
        switch (iconKey) {
            case "food": return "🍔";
            case "transport": return "🚌";
            case "shopping": return "🛍️";
            case "bills": return "📄";
            case "education": return "📚";
            case "entertainment": return "🎮";
            case "health": return "💊";
            case "family": return "👨‍👩‍👧";
            case "saving": return "💰";
            default: return "📦";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
