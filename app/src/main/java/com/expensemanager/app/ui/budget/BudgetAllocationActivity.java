package com.expensemanager.app.ui.budget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.databinding.ActivityBudgetAllocationBinding;
import com.expensemanager.app.databinding.ItemBudgetAllocSimpleBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetAllocationActivity extends AppCompatActivity {

    public static final String EXTRA_MONTH_KEY = "extra_month_key";

    private ActivityBudgetAllocationBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final TransactionRepository txRepo = new TransactionRepository();

    private List<Category> expenseCategories = new ArrayList<>();
    private Map<String, Double> allocatedMap = new HashMap<>();
    private boolean isNextMonth = false;
    private String selectedMonthKey = null;
    private double monthlyIncome = 0;
    private double expectedIncomeOverride = -1;
    private String uid = null;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBudgetAllocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        prefs = getSharedPreferences("budget_" + uid, Context.MODE_PRIVATE);

        selectedMonthKey = getIntent().getStringExtra(EXTRA_MONTH_KEY);
        if (selectedMonthKey != null) {
            isNextMonth = false;
        }

        setupClickListeners();
        loadData();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSettings.setOnClickListener(v -> {
            if (uid != null) {
                BudgetSettingsDialog dialog = new BudgetSettingsDialog(this, uid);
                dialog.show();
            }
        });

        binding.btnSave.setOnClickListener(v -> saveAllAllocations());

        updateTabsUI();

        binding.tabCurrent.setOnClickListener(v -> {
            isNextMonth = false;
            selectedMonthKey = null;
            updateTabsUI();
            loadData();
        });

        binding.tabNext.setOnClickListener(v -> {
            isNextMonth = true;
            selectedMonthKey = null;
            updateTabsUI();
            loadData();
        });

        binding.btnAddEssential.setOnClickListener(v -> showAddCategoryDialog());
        binding.btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());

        binding.btnEditExpectedIncome.setOnClickListener(v -> showExpectedIncomeDialog());
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
        if (uid == null) return;

        String monthKey = getMonthKey();

        expectedIncomeOverride = prefs.getLong("income_" + monthKey, -1);
        if (expectedIncomeOverride == -1) expectedIncomeOverride = monthlyIncome;

        txRepo.observeMonth(uid, monthKey).observe(this, list -> {
            monthlyIncome = 0;
            if (list != null) {
                for (Transaction t : list) {
                    if (Transaction.TYPE_INCOME.equals(t.getType())) {
                        monthlyIncome += t.getAmount();
                    }
                }
            }
            updateUI();
        });

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
        if (selectedMonthKey != null) {
            return selectedMonthKey;
        }
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

        double effectiveIncome = getEffectiveIncome();
        double remaining = effectiveIncome - totalAllocated;
        if (remaining < 0) remaining = 0;

        binding.textAllocated.setText(MoneyFormat.format(totalAllocated));
        binding.textUnallocated.setText(MoneyFormat.format(remaining));
        binding.textExpectedIncome.setText(MoneyFormat.format(effectiveIncome));

        updateEssentialItems();
    }

    private double getEffectiveIncome() {
        if (expectedIncomeOverride >= 0) return expectedIncomeOverride;
        return monthlyIncome;
    }

    private void showExpectedIncomeDialog() {
        if (uid == null) return;
        String monthKey = getMonthKey();

        EditText input = new EditText(this);
        input.setHint("Ví dụ: 15000000");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (expectedIncomeOverride >= 0) {
            input.setText(String.valueOf((long) expectedIncomeOverride));
        }

        new AlertDialog.Builder(this)
                .setTitle("Thu nhập dự kiến")
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    String text = input.getText().toString().trim().replace(",", "");
                    try {
                        double val = Double.parseDouble(text);
                        if (val < 0) throw new Exception();
                        expectedIncomeOverride = val;
                        prefs.edit().putLong("income_" + monthKey, (long) val).apply();
                        updateUI();
                    } catch (Exception e) {
                        Toast.makeText(this, "Số không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Xóa", (d, w) -> {
                    expectedIncomeOverride = -1;
                    prefs.edit().remove("income_" + monthKey).apply();
                    updateUI();
                })
                .show();
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
        if (uid == null) return;

        Budget b = new Budget();
        b.setScope(Budget.SCOPE_CATEGORY);
        b.setCategoryId(cat.getId());
        b.setMonth(monthKey);
        b.setLimitAmount(amount);
        budgetRepo.addOrUpdate(uid, b);

        allocatedMap.put(cat.getId(), amount);
        Toast.makeText(this, "Da luu", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void saveAllAllocations() {
        if (uid == null) return;

        String monthKey = getMonthKey();
        for (Map.Entry<String, Double> entry : allocatedMap.entrySet()) {
            String catId = entry.getKey();
            Double amount = entry.getValue();
            if (catId != null && amount != null) {
                Budget b = new Budget();
                b.setScope(Budget.SCOPE_CATEGORY);
                b.setCategoryId(catId);
                b.setMonth(monthKey);
                b.setLimitAmount(amount);
                budgetRepo.addOrUpdate(uid, b);
            }
        }
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
        if (uid == null) return;

        final EditText input = new EditText(this);
        input.setHint("Tên nhóm");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        final String[] groupLabels = {"Thiết yếu", "Nhu cầu", "Khoản muốn có", "Khác"};
        final String[] groupValues = {"essential", "need", "want", "other"};
        final int[] selectedGroup = {3};

        View radioView = getLayoutInflater().inflate(R.layout.dialog_group_picker, null);
        RadioButton rb0 = radioView.findViewById(R.id.rbEssential);
        RadioButton rb1 = radioView.findViewById(R.id.rbNeed);
        RadioButton rb2 = radioView.findViewById(R.id.rbWant);
        RadioButton rb3 = radioView.findViewById(R.id.rbOther);

        rb3.setChecked(true);
        rb0.setOnClickListener(v -> selectedGroup[0] = 0);
        rb1.setOnClickListener(v -> selectedGroup[0] = 1);
        rb2.setOnClickListener(v -> selectedGroup[0] = 2);
        rb3.setOnClickListener(v -> selectedGroup[0] = 3);

        // Combine the EditText and RadioGroup in a vertical layout
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 16);
        container.addView(input);
        container.addView(radioView);

        new AlertDialog.Builder(this)
                .setTitle("Tạo nhóm")
                .setView(container)
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Nhập tên nhóm", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Category group = new Category();
                    group.setName(name);
                    group.setType(Category.TYPE_EXPENSE);
                    group.setGroup(groupValues[selectedGroup[0]]);
                    new CategoryRepository().add(uid, group);
                    Toast.makeText(this, "Đã tạo nhóm: " + name, Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton("Hủy", null)
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
