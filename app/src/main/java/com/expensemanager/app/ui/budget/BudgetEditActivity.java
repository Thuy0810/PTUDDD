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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.databinding.ActivityBudgetEditBinding;
import com.expensemanager.app.databinding.ItemBudgetAllocationBinding;
import com.expensemanager.app.util.MoneyFormat;

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
    private Map<String, Double> categoryBudgets = new HashMap<>();
    private double totalBudget = 0;
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
        binding.recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
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
            totalBudget = 0;
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
        binding.textTotalBudget.setText(MoneyFormat.format(totalBudget));
    }

    private void updateRecyclerView() {
        binding.recyclerCategories.removeAllViews();

        for (Category cat : expenseCategories) {
            View itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_budget_allocation, binding.recyclerCategories, false);

            ItemBudgetAllocationBinding itemBinding = ItemBudgetAllocationBinding.bind(itemView);

            double amount = categoryBudgets.containsKey(cat.getId())
                    ? categoryBudgets.get(cat.getId()) : 0;
            int progress = totalBudget > 0 ? (int) (amount / totalBudget * 100) : 0;
            progress = Math.min(progress, 100);

            itemBinding.textCategoryName.setText(cat.getName());
            itemBinding.textAllocatedAmount.setText(MoneyFormat.format(amount));
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

    private void showEditDialog(Category cat, double currentAmount) {
        EditText input = new EditText(this);
        input.setHint("Số tiền phân bổ");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (currentAmount > 0) {
            input.setText(String.valueOf((long) currentAmount));
        }

        new AlertDialog.Builder(this)
                .setTitle("Phân bổ: " + cat.getName())
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString().trim());
                        if (amount < 0) throw new Exception();
                        saveCategoryBudget(cat, amount);
                    } catch (Exception e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveCategoryBudget(Category cat, double amount) {
        String uid = authRepo.getUid();
        if (uid == null) return;

        // check if budget exists for this category
        Budget existing = null;
        for (Budget b : budgets) {
            if (cat.getId().equals(b.getCategoryId())) {
                existing = b;
                break;
            }
        }

        if (existing != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("budgets").document(existing.getId())
                    .update("limitAmount", amount)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Budget b = new Budget();
            b.setScope(Budget.SCOPE_CATEGORY);
            b.setCategoryId(cat.getId());
            b.setMonth(monthKey);
            b.setLimitAmount(amount);
            budgetRepo.addOrUpdate(uid, b);
        }

        // reload
        loadData();
    }

    private void showCreateGroupDialog() {
        EditText input = new EditText(this);
        input.setHint("Tên nhóm");

        new AlertDialog.Builder(this)
                .setTitle("Tạo nhóm danh mục")
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

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa ngân sách")
                .setMessage("Bạn có chắc muốn xóa tất cả ngân sách tháng này?")
                .setPositiveButton("Xóa", (d, w) -> {
                    String uid = authRepo.getUid();
                    if (uid != null) {
                        for (Budget b : budgets) {
                            budgetRepo.delete(uid, b.getId());
                        }
                        Toast.makeText(this, "Đã xóa ngân sách", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveAll() {
        Toast.makeText(this, "Đã lưu ngân sách", Toast.LENGTH_SHORT).show();
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
