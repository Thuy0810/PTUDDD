package com.expensemanager.app.ui.budget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import com.expensemanager.app.util.MoneyInputFormatter;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class BudgetAllocationActivity extends AppCompatActivity {

    public static final String EXTRA_MONTH_KEY = "extra_month_key";

    private ActivityBudgetAllocationBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final TransactionRepository txRepo = new TransactionRepository();

    private List<Category> expenseCategories = new ArrayList<>();
    private Map<String, Long> allocatedMap = new HashMap<>();
    private Map<String, Long> spentMap = new HashMap<>();
    private boolean isNextMonth = false;
    private String selectedMonthKey = null;
    private long monthlyIncome = 0L;
    private long expectedIncomeOverride = -1;
    private String uid = null;
    private SharedPreferences prefs;

    private LiveData<List<Transaction>> txLiveData;
    private LiveData<List<Category>> catLiveData;
    private LiveData<List<Budget>> budgetLiveData;
    private Observer<List<Transaction>> txObserver;
    private Observer<List<Category>> catObserver;
    private Observer<List<Budget>> budgetObserver;

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

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("BudgetAlloc", "onResume: reloading data");
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
        return getString(R.string.j1_month_year_label, month, year);
    }

    private void loadData() {
        if (uid == null) return;

        String monthKey = getMonthKey();
        Log.d("BudgetAlloc", "loadData: monthKey=" + monthKey);

        // Remove old observers first
        if (txLiveData != null && txObserver != null) {
            txLiveData.removeObserver(txObserver);
        }
        if (catLiveData != null && catObserver != null) {
            catLiveData.removeObserver(catObserver);
        }
        if (budgetLiveData != null && budgetObserver != null) {
            budgetLiveData.removeObserver(budgetObserver);
        }

        expectedIncomeOverride = prefs.getLong("income_" + monthKey, -1);
        if (expectedIncomeOverride == -1) expectedIncomeOverride = monthlyIncome;

        txObserver = list -> {
            monthlyIncome = 0L;
            spentMap.clear();
            if (list != null) {
                for (Transaction t : list) {
                    if (Transaction.TYPE_INCOME.equals(t.getType())) {
                        monthlyIncome += t.getAmount();
                    } else if (Transaction.TYPE_EXPENSE.equals(t.getType())
                            && t.getCategoryId() != null) {
                        Long cur = spentMap.get(t.getCategoryId());
                        spentMap.put(t.getCategoryId(),
                                (cur != null ? cur : 0L) + t.getAmount());
                    }
                }
            }
            updateUI();
        };
        txLiveData = txRepo.observeMonth(uid, monthKey);
        txLiveData.observe(this, txObserver);

        catObserver = list -> {
            expenseCategories = new ArrayList<>();
            if (list != null) {
                for (Category c : list) {
                    if (Category.TYPE_EXPENSE.equals(c.getType())) {
                        expenseCategories.add(c);
                    }
                }
            }
            updateUI();
        };
        catLiveData = categoryRepo.observeAll(uid);
        catLiveData.observe(this, catObserver);

        budgetObserver = list -> {
            allocatedMap = new HashMap<>();
            if (list != null && !list.isEmpty()) {
                Log.d("BudgetAlloc", "observeMonth loaded " + list.size() + " budgets for monthKey=" + monthKey);
                for (Budget b : list) {
                    if (b.getCategoryId() != null) {
                        Log.d("BudgetAlloc", "  budget: categoryId=" + b.getCategoryId() + ", amount=" + b.getLimitAmount());
                        allocatedMap.put(b.getCategoryId(), b.getLimitAmount());
                    }
                }
            } else {
                Log.d("BudgetAlloc", "observeMonth: no budgets for monthKey=" + monthKey);
            }
            updateUI();
        };
        budgetLiveData = budgetRepo.observeMonth(uid, monthKey);
        budgetLiveData.observe(this, budgetObserver);
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
        long totalAllocated = 0L;
        for (Long d : allocatedMap.values()) {
            totalAllocated += d;
        }

        long effectiveIncome = getEffectiveIncome();
        long remaining = effectiveIncome - totalAllocated;
        if (remaining < 0) remaining = 0;

        Log.d("BudgetAlloc", "updateUI: allocatedMap size=" + allocatedMap.size()
                + ", totalAllocated=" + totalAllocated + ", effectiveIncome=" + effectiveIncome
                + ", remaining=" + remaining
                + ", expenseCategories=" + expenseCategories.size());

        binding.textAllocated.setText(MoneyFormat.formatLong(totalAllocated));
        binding.textUnallocated.setText(MoneyFormat.formatLong(remaining));
        binding.textExpectedIncome.setText(MoneyFormat.formatLong(effectiveIncome));

        updateEssentialItems();
    }

    private long getEffectiveIncome() {
        if (expectedIncomeOverride >= 0) return expectedIncomeOverride;
        return monthlyIncome;
    }

    private void showExpectedIncomeDialog() {
        if (uid == null) return;
        String monthKey = getMonthKey();

        EditText input = new EditText(this);
        input.setHint(getString(R.string.j1_income_example_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(input);
        if (expectedIncomeOverride >= 0) {
            input.setText(MoneyInputFormatter.format(expectedIncomeOverride));
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j1_expected_income))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    long val = MoneyInputFormatter.getRawValue(input);
                    if (val <= 0) {
                        Toast.makeText(this, getString(R.string.j1_invalid_number), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    expectedIncomeOverride = val;
                    prefs.edit().putLong("income_" + monthKey, val).apply();
                    updateUI();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .setNeutralButton(getString(R.string.delete), (d, w) -> {
                    expectedIncomeOverride = -1;
                    prefs.edit().remove("income_" + monthKey).apply();
                    updateUI();
                })
                .show();
    }

    private void updateEssentialItems() {
        binding.layoutEssentialItems.removeAllViews();
        Log.d("BudgetAlloc", "updateEssentialItems: expenseCategories=" + expenseCategories.size() + ", allocatedMap=" + allocatedMap.size());

        for (Category cat : expenseCategories) {
            long allocated = allocatedMap.containsKey(cat.getId())
                    ? allocatedMap.get(cat.getId()) : 0L;
            Log.d("BudgetAlloc", "  item: cat=" + cat.getId() + " (" + cat.getName() + "), allocated=" + allocated);

            addEssentialItem(cat, allocated);
        }

        if (expenseCategories.isEmpty()) {
            addPlaceholderItem(getString(R.string.j1_no_expense_category), 0);
        }
    }

    private void addPlaceholderItem(String name, long allocated) {
        ItemBudgetAllocSimpleBinding card = ItemBudgetAllocSimpleBinding.inflate(
                LayoutInflater.from(this), binding.layoutEssentialItems, false);
        card.textCategoryName.setText(name);
        card.textCategoryIcon.setImageResource(R.drawable.ico_other);
        card.progressBudget.setProgress(0);
        card.textStatus.setVisibility(View.GONE);
        card.textSpent.setText(getString(R.string.budget_spent) + ": 0 / --");
        card.textRemaining.setText(getString(R.string.budget_remaining) + " 0");
        card.textRemaining.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
        card.btnReallocate.setVisibility(View.GONE);
        binding.layoutEssentialItems.addView(card.getRoot());
    }

    private void addEssentialItem(Category cat, long allocated) {
        long spent = spentMap.containsKey(cat.getId()) ? spentMap.get(cat.getId()) : 0L;
        long remaining = Math.max(0L, allocated - spent);
        long deficit = Math.max(0L, spent - allocated);
        int pct = allocated > 0 ? (int) Math.min(100L * spent / allocated, 100L) : 0;

        ItemBudgetAllocSimpleBinding card = ItemBudgetAllocSimpleBinding.inflate(
                LayoutInflater.from(this), binding.layoutEssentialItems, false);

        card.textCategoryName.setText(cat.getName());

        // Progress
        card.progressBudget.setProgress(pct);

        // Status chip
        String statusText;
        int statusBgColor;
        if (deficit > 0) {
            statusText = getString(R.string.budget_exceeded_amount, MoneyFormat.format(deficit));
            statusBgColor = ContextCompat.getColor(this, R.color.expense_red);
        } else if (pct >= 100) {
            statusText = getString(R.string.budget_status_reached);
            statusBgColor = ContextCompat.getColor(this, R.color.expense_red);
        } else if (pct >= 80) {
            statusText = getString(R.string.budget_status_warning);
            statusBgColor = ContextCompat.getColor(this, R.color.warning);
        } else {
            statusText = getString(R.string.budget_status_safe);
            statusBgColor = ContextCompat.getColor(this, R.color.income_green);
        }
        card.textStatus.setText(statusText);
        card.textStatus.setBackgroundColor(statusBgColor);

        // Spent + remaining
        card.textSpent.setText(getString(R.string.budget_spent) + ": " + MoneyFormat.format(spent)
                + " / " + MoneyFormat.format(allocated));

        if (deficit > 0) {
            card.textRemaining.setText(getString(R.string.budget_exceeded_amount,
                    MoneyFormat.format(deficit)));
            card.textRemaining.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
            card.btnReallocate.setVisibility(View.VISIBLE);
            card.btnReallocate.setOnClickListener(v -> showReallocationDialog(cat, deficit));
        } else {
            card.textRemaining.setText(getString(R.string.budget_remaining) + " "
                    + MoneyFormat.format(remaining));
            card.textRemaining.setTextColor(ContextCompat.getColor(this, R.color.income_green));
            card.btnReallocate.setVisibility(View.GONE);
        }

        int catIconColor;
        try {
            catIconColor = Color.parseColor(cat.getColorHex());
        } catch (Exception e) {
            catIconColor = ContextCompat.getColor(this, R.color.primary);
        }
        com.expensemanager.app.util.CategoryIcons.apply(
                card.textCategoryIcon, card.viewCategoryBg,
                cat.getIconKey(), catIconColor, cat.getType());

        card.getRoot().setOnClickListener(v -> showEditDialog(cat, allocated));
        card.btnMenu.setOnClickListener(v -> showItemMenu(v, cat, allocated));

        binding.layoutEssentialItems.addView(card.getRoot());
    }

    private void showReallocationDialog(Category cat, long deficitAmount) {
        // TODO: implement reallocation dialog
        Toast.makeText(this, getString(R.string.reallocation_title) + " - " + cat.getName(),
                Toast.LENGTH_SHORT).show();
    }

    private void showItemMenu(View anchor, Category cat, long allocated) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_item_edit_delete, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                showEditDialog(cat, allocated);
                return true;
            } else if (id == R.id.action_delete) {
                confirmDeleteBudget(cat);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDeleteBudget(Category cat) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j1_delete_budget_title))
                .setMessage(getString(R.string.j1_delete_budget_message, cat.getName()))
                .setPositiveButton(getString(R.string.delete), (d, w) -> {
                    String monthKey = getMonthKey();
                    String docId = uid + "_" + monthKey + "_" + cat.getId();
                    budgetRepo.delete(uid, docId);
                    allocatedMap.remove(cat.getId());
                    updateUI();
                    Toast.makeText(this, getString(R.string.success_delete), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showEditDialog(Category cat, long currentAmount) {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.j1_allocation_amount_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(input);
        if (currentAmount > 0) {
            input.setText(MoneyInputFormatter.format(currentAmount));
        }

        String monthKey = getMonthKey();

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j1_allocate_to, cat.getName()))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    long amount = MoneyInputFormatter.getRawValue(input);
                    if (amount <= 0) {
                        Toast.makeText(this, getString(R.string.error_invalid_amount),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveBudget(cat, amount, monthKey);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void saveBudget(Category cat, long amount, String monthKey) {
        if (uid == null) return;
        Log.d("BudgetAlloc", "saveBudget: cat=" + cat.getId() + ", amount=" + amount + ", month=" + monthKey);

        Budget b = new Budget();
        b.setScope(Budget.SCOPE_CATEGORY);
        b.setCategoryId(cat.getId());
        b.setMonth(monthKey);
        b.setLimitAmount(amount);
        budgetRepo.addOrUpdate(uid, b,
                aVoid -> {
                    Log.d("BudgetAlloc", "saveBudget SUCCESS: cat=" + cat.getId());
                    allocatedMap.put(cat.getId(), amount);
                    updateUI();
                },
                e -> {
                    Log.e("BudgetAlloc", "saveBudget FAILED: " + e.getMessage());
                    Toast.makeText(this, getString(R.string.j1_save_error, e.getMessage()), Toast.LENGTH_LONG).show();
                });
        Toast.makeText(this, getString(R.string.j1_saving_progress), Toast.LENGTH_SHORT).show();
    }

    private void saveAllAllocations() {
        if (uid == null) return;

        String monthKey = getMonthKey();
        final int total = allocatedMap.size();
        Log.d("BudgetAlloc", "saveAllAllocations: total=" + total + ", month=" + monthKey);

        if (total == 0) {
            Toast.makeText(this, getString(R.string.j1_no_allocation_to_save), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final int[] done = {0};
        final boolean[] anyError = {false};
        for (Map.Entry<String, Long> entry : allocatedMap.entrySet()) {
            String catId = entry.getKey();
            Long amount = entry.getValue();
            if (catId == null || amount == null) continue;
            Log.d("BudgetAlloc", "  saving: catId=" + catId + ", amount=" + amount);
            Budget b = new Budget();
            b.setScope(Budget.SCOPE_CATEGORY);
            b.setCategoryId(catId);
            b.setMonth(monthKey);
            b.setLimitAmount(amount);
            budgetRepo.addOrUpdate(uid, b, aVoid -> {
                done[0]++;
                Log.d("BudgetAlloc", "  saved OK: catId=" + catId + ", done=" + done[0] + "/" + total);
                if (done[0] >= total && !anyError[0]) {
                    Toast.makeText(this, getString(R.string.j1_saved_all_allocations), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }, e -> {
                anyError[0] = true;
                done[0]++;
                Log.e("BudgetAlloc", "  save FAILED: catId=" + catId + ", error=" + e.getMessage());
                Toast.makeText(this, getString(R.string.j1_save_error, e.getMessage()), Toast.LENGTH_LONG).show();
            });
        }
    }

    private void showAddCategoryDialog() {
        if (expenseCategories.isEmpty()) {
            Toast.makeText(this, getString(R.string.j1_no_category), Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[expenseCategories.size()];
        for (int i = 0; i < expenseCategories.size(); i++) {
            names[i] = expenseCategories.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_category))
                .setItems(names, (d, which) -> {
                    showEditDialog(expenseCategories.get(which), 0);
                })
                .show();
    }

    private void showCreateGroupDialog() {
        if (uid == null) return;

        final EditText input = new EditText(this);
        input.setHint(getString(R.string.j1_group_name_hint));
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
                .setTitle(getString(R.string.j1_create_group))
                .setView(container)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.j1_enter_group_name), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Category group = new Category();
                    group.setName(name);
                    group.setType(Category.TYPE_EXPENSE);
                    group.setGroup(groupValues[selectedGroup[0]]);
                    new CategoryRepository().add(uid, group);
                    Toast.makeText(this, getString(R.string.j1_group_created, name), Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (txLiveData != null && txObserver != null) {
            txLiveData.removeObserver(txObserver);
        }
        if (catLiveData != null && catObserver != null) {
            catLiveData.removeObserver(catObserver);
        }
        if (budgetLiveData != null && budgetObserver != null) {
            budgetLiveData.removeObserver(budgetObserver);
        }
        binding = null;
    }
}
