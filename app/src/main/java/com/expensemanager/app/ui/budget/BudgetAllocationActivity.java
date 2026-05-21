package com.expensemanager.app.ui.budget;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetAllocationActivity extends AppCompatActivity {

    private ActivityBudgetAllocationBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();

    private List<Category> expenseCategories = new ArrayList<>();
    private Map<String, Double> allocatedMap = new HashMap<>();
    private boolean isNextMonth = false;
    private int currentYear, currentMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBudgetAllocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH) + 1;

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

        binding.btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
            finish();
        });

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
        Calendar cal = Calendar.getInstance();
        if (next) cal.add(Calendar.MONTH, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("'T'w第'w'", Locale.CHINESE);
        int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String[] weekdays = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        return "T" + dayOfWeek + " (" + weekdays[dayOfWeek - 1] + ")";
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
        // Calculate total allocated
        double totalAllocated = 0;
        for (Double d : allocatedMap.values()) {
            totalAllocated += d;
        }

        // Assume remaining = total budget - allocated (total budget from income or preset)
        double remaining = 0; // This would come from monthly income or budget setting
        for (Category cat : expenseCategories) {
            remaining += allocatedMap.containsKey(cat.getId()) ? allocatedMap.get(cat.getId()) : 0;
        }

        binding.textAllocated.setText(MoneyFormat.format(totalAllocated));
        binding.textUnallocated.setText(MoneyFormat.format(remaining));

        // Show essential categories (those with group = "essential" or hardcoded ones)
        updateEssentialItems();
    }

    private void updateEssentialItems() {
        binding.layoutEssentialItems.removeAllViews();

        // Show all categories as essential items (or filter by group if available)
        for (Category cat : expenseCategories) {
            double allocated = allocatedMap.containsKey(cat.getId())
                    ? allocatedMap.get(cat.getId()) : 0;

            addEssentialItem(cat, allocated);
        }

        if (expenseCategories.isEmpty()) {
            // Add placeholder items like in the image
            addPlaceholderItem("Thuê nhà", 0);
            addPlaceholderItem("Điện thoại internet", 0);
            addPlaceholderItem("Hóa đơn", 0);
        }
    }

    private void addPlaceholderItem(String name, double allocated) {
        ItemBudgetAllocSimpleBinding card = ItemBudgetAllocSimpleBinding.inflate(
                LayoutInflater.from(this), binding.layoutEssentialItems, false);

        card.textCategoryName.setText(name);
        card.textCategoryIcon.setText("📦");
        card.textAllocated.setText(MoneyFormat.format(allocated));
        card.textAmount.setText(MoneyFormat.format(allocated) + " / --");

        card.getRoot().setOnClickListener(v -> {
            // Show edit dialog for placeholder
        });

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
        input.setHint("Số tiền phân bổ");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (currentAmount > 0) {
            input.setText(String.valueOf((long) currentAmount));
        }

        String monthKey = getMonthKey();

        new AlertDialog.Builder(this)
                .setTitle("Phân bổ: " + cat.getName())
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString().trim());
                        if (amount < 0) throw new Exception();
                        saveBudget(cat, amount, monthKey);
                    } catch (Exception e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
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
        budgetRepo.add(uid, b);
        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
    }

    private void showAddCategoryDialog() {
        String[] names = new String[expenseCategories.size()];
        for (int i = 0; i < expenseCategories.size(); i++) {
            names[i] = expenseCategories.get(i).getName();
        }

        if (names.length == 0) {
            Toast.makeText(this, "Chưa có danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn danh mục")
                .setItems(names, (d, which) -> {
                    showEditDialog(expenseCategories.get(which), 0);
                })
                .show();
    }

    private void showCreateGroupDialog() {
        EditText input = new EditText(this);
        input.setHint("Tên nhóm");

        new AlertDialog.Builder(this)
                .setTitle("Tạo nhóm")
                .setView(input)
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Toast.makeText(this, "Đã tạo nhóm: " + name, Toast.LENGTH_SHORT).show();
                    }
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
