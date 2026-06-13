package com.expensemanager.app.ui.budget;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.databinding.ActivityBudgetAllocationBinding;
import com.expensemanager.app.databinding.ItemBudgetAllocSimpleBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetAllocationActivity extends AppCompatActivity {

    private ActivityBudgetAllocationBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();

    private List<Category> expenseCategories = new ArrayList<>();
    private Map<String, Double> allocatedMap = new HashMap<>();
    private boolean isNextMonth = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBudgetAllocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
        loadData();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSettings.setOnClickListener(v -> {
            String uid = authRepo.getUid();
            if (uid != null) {
                BudgetSettingsDialog dialog = new BudgetSettingsDialog(this, uid);
                dialog.show();
            }
        });

        binding.btnSave.setOnClickListener(v -> saveAllAllocations());

        updateTabsUI();

        binding.tabCurrent.setOnClickListener(v -> {
            isNextMonth = false;
            updateTabsUI();
            loadData();
        });

        binding.tabNext.setOnClickListener(v -> {
            isNextMonth = true;
            updateTabsUI();
            loadData();
        });

        binding.btnAddEssential.setOnClickListener(v -> showAddCategoryDialog());
        binding.btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());
    }

    private void updateTabsUI() {
        String currentLabel = getMonthLabel(false);
        String nextLabel = getMonthLabel(true);

        if (isNextMonth) {
            binding.tabCurrent.setBackgroundResource(R.drawable.bg_chip_inactive);
            binding.tabCurrent.setTextColor(getColor(R.color.text_secondary));
            binding.tabCurrent.setTypeface(null, android.graphics.Typeface.NORMAL);

            binding.tabNext.setBackgroundResource(R.drawable.bg_tab_active);
            binding.tabNext.setTextColor(getColor(R.color.text_primary));
            binding.tabNext.setTypeface(null, android.graphics.Typeface.BOLD);
            binding.tabNext.setText(nextLabel);

            binding.tabCurrent.setText(currentLabel);
        } else {
            binding.tabCurrent.setBackgroundResource(R.drawable.bg_tab_active);
            binding.tabCurrent.setTextColor(getColor(R.color.text_primary));
            binding.tabCurrent.setTypeface(null, android.graphics.Typeface.BOLD);
            binding.tabCurrent.setText(currentLabel);

            binding.tabNext.setBackgroundResource(R.drawable.bg_chip_inactive);
            binding.tabNext.setTextColor(getColor(R.color.text_secondary));
            binding.tabNext.setTypeface(null, android.graphics.Typeface.NORMAL);
            binding.tabNext.setText(nextLabel);
        }
    }

    private String getMonthLabel(boolean next) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (next) cal.add(java.util.Calendar.MONTH, 1);
        int month = cal.get(java.util.Calendar.MONTH) + 1;
        int year = cal.get(java.util.Calendar.YEAR);
        return "Thang " + month + "/" + year;
    }

    private void loadData() {
        String uid = authRepo.getUid();
        if (uid == null) return;

        String monthKey = getMonthKey();

        categoryRepo.observeAll(uid).observe(this, list -> {
            expenseCategories = new ArrayList<>();
            if (list != null) {
                for (Category c : list) {
                    if (Category.TYPE_EXPENSE.equals(c.getType())) {
                        expenseCategories.add(c);
                    }
                }
            }
            updateUI();
        });

        budgetRepo.observeMonth(uid, monthKey).observe(this, list -> {
            allocatedMap = new HashMap<>();
            if (list != null) {
                for (Budget b : list) {
                    if (b.getCategoryId() != null) {
                        allocatedMap.put(b.getCategoryId(), b.getLimitAmount());
                    }
                }
            }
            updateUI();
        });
    }

    private String getMonthKey() {
        if (isNextMonth) {
            return DateUtils.nextMonthKey();
        }
        return DateUtils.currentMonthKey();
    }

    private void updateUI() {
        double totalAllocated = 0;
        for (Double d : allocatedMap.values()) {
            totalAllocated += d;
        }

        double totalIncome = getMonthlyIncome();
        double remaining = totalIncome - totalAllocated;
        if (remaining < 0) remaining = 0;

        binding.textAllocated.setText(MoneyFormat.format(totalAllocated));
        binding.textUnallocated.setText(MoneyFormat.format(remaining));

        updateEssentialItems();
    }

    private double getMonthlyIncome() {
        return 10000000;
    }

    private void updateEssentialItems() {
        binding.layoutEssentialItems.removeAllViews();

        for (Category cat : expenseCategories) {
            double allocated = allocatedMap.containsKey(cat.getId())
                    ? allocatedMap.get(cat.getId()) : 0;

            addEssentialItem(cat, allocated);
        }

        if (expenseCategories.isEmpty()) {
            addPlaceholderItem("Chua co danh muc chi tieu", 0);
        }
    }

    private void addPlaceholderItem(String name, double allocated) {
        ItemBudgetAllocSimpleBinding card = ItemBudgetAllocSimpleBinding.inflate(
                LayoutInflater.from(this), binding.layoutEssentialItems, false);

        card.textCategoryName.setText(name);
        card.textCategoryIcon.setText("📦");
        card.textAllocated.setText(MoneyFormat.format(allocated));
        card.textAmount.setText(MoneyFormat.format(allocated) + " / --");

        binding.layoutEssentialItems.addView(card.getRoot());
    }

    private void addEssentialItem(Category cat, double allocated) {
        ItemBudgetAllocSimpleBinding card = ItemBudgetAllocSimpleBinding.inflate(
                LayoutInflater.from(this), binding.layoutEssentialItems, false);

        card.textCategoryName.setText(cat.getName());
        card.textCategoryIcon.setText(getCategoryEmoji(cat.getIconKey()));
        card.textAllocated.setText(MoneyFormat.format(allocated));
        card.textAmount.setText(MoneyFormat.format(allocated) + " / --");

        if (cat.getColorHex() != null) {
            try {
                int color = Color.parseColor(cat.getColorHex());
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(color);
                card.viewCategoryBg.setBackground(bg);
            } catch (Exception ignored) {}
        }

        card.getRoot().setOnClickListener(v -> showEditDialog(cat, allocated));

        binding.layoutEssentialItems.addView(card.getRoot());
    }

    private void showEditDialog(Category cat, double currentAmount) {
        EditText input = new EditText(this);
        input.setHint("So tien phan bo");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (currentAmount > 0) {
            input.setText(String.valueOf((long) currentAmount));
        }

        String monthKey = getMonthKey();

        new AlertDialog.Builder(this)
                .setTitle("Phan bo: " + cat.getName())
                .setView(input)
                .setPositiveButton("Luu", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString().trim());
                        if (amount < 0) throw new Exception();
                        saveBudget(cat, amount, monthKey);
                    } catch (Exception e) {
                        Toast.makeText(this, "So tien khong hop le", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Huy", null)
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

        allocatedMap.put(cat.getId(), amount);
        Toast.makeText(this, "Da luu", Toast.LENGTH_SHORT).show();
    }

    private void saveAllAllocations() {
        Toast.makeText(this, "Da luu tat ca phan bo", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showAddCategoryDialog() {
        if (expenseCategories.isEmpty()) {
            Toast.makeText(this, "Chua co danh muc", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[expenseCategories.size()];
        for (int i = 0; i < expenseCategories.size(); i++) {
            names[i] = expenseCategories.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chon danh muc")
                .setItems(names, (d, which) -> {
                    showEditDialog(expenseCategories.get(which), 0);
                })
                .show();
    }

    private void showCreateGroupDialog() {
        EditText input = new EditText(this);
        input.setHint("Ten nhom");

        new AlertDialog.Builder(this)
                .setTitle("Tao nhom")
                .setView(input)
                .setPositiveButton("Tao", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Toast.makeText(this, "Da tao nhom: " + name, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Huy", null)
                .show();
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
            case "home": return "🏠";
            case "rent": return "🏠";
            default: return "📦";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
