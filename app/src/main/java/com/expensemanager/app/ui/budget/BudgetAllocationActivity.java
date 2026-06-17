package com.expensemanager.app.ui.budget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.GoalRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.databinding.ActivityBudgetAllocationBinding;
import com.expensemanager.app.databinding.ItemBudgetAllocSimpleBinding;
import com.expensemanager.app.domain.usecase.BudgetService;
import com.expensemanager.app.domain.usecase.GoalService;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyInputFormatter;
import com.google.android.material.progressindicator.LinearProgressIndicator;

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
    private final GoalRepository goalRepo = new GoalRepository();
    private final GoalService goalService = new GoalService();

    private List<SavingsGoal> savingsGoals = new ArrayList<>();

    private List<Category> expenseCategories = new ArrayList<>();
    private Map<String, Long> allocatedMap = new HashMap<>();
    private Map<String, Long> spentMap = new HashMap<>();
    // Cuốn chiếu (rollover) từ tháng liền trước: catId -> (prevAllocated - prevSpent)
    private Map<String, Long> rolloverMap = new HashMap<>();
    private Map<String, Long> prevAllocatedMap = new HashMap<>();
    private Map<String, Long> prevSpentMap = new HashMap<>();
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

    // Tháng liền trước — dùng để tính cuốn chiếu
    private LiveData<List<Transaction>> prevTxLiveData;
    private LiveData<List<Budget>> prevBudgetLiveData;
    private Observer<List<Transaction>> prevTxObserver;
    private Observer<List<Budget>> prevBudgetObserver;

    // Mục tiêu tiết kiệm
    private LiveData<List<SavingsGoal>> goalLiveData;
    private Observer<List<SavingsGoal>> goalObserver;

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
        if (prevTxLiveData != null && prevTxObserver != null) {
            prevTxLiveData.removeObserver(prevTxObserver);
        }
        if (prevBudgetLiveData != null && prevBudgetObserver != null) {
            prevBudgetLiveData.removeObserver(prevBudgetObserver);
        }
        if (goalLiveData != null && goalObserver != null) {
            goalLiveData.removeObserver(goalObserver);
        }

        // -1 = NGƯỜI DÙNG CHƯA đặt thu nhập dự kiến thủ công.
        // KHÔNG gán = monthlyIncome ở đây, vì giao dịch chưa tải xong (monthlyIncome còn 0)
        // sẽ khoá cứng thu nhập = 0. Để -1 thì getEffectiveIncome() tự rơi về thu nhập thực.
        expectedIncomeOverride = prefs.getLong("income_" + monthKey, -1);

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

        // ----- Tháng liền trước: tính cuốn chiếu (rollover) -----
        String prevMonthKey = DateUtils.previousMonthKey(monthKey);

        prevBudgetObserver = list -> {
            prevAllocatedMap = new HashMap<>();
            if (list != null) {
                for (Budget b : list) {
                    if (b.getCategoryId() != null) {
                        prevAllocatedMap.put(b.getCategoryId(), b.getAllocatedAmount());
                    }
                }
            }
            recomputeRollover();
            updateUI();
        };
        prevBudgetLiveData = budgetRepo.observeMonth(uid, prevMonthKey);
        prevBudgetLiveData.observe(this, prevBudgetObserver);

        prevTxObserver = list -> {
            prevSpentMap = new HashMap<>();
            if (list != null) {
                for (Transaction t : list) {
                    if (Transaction.TYPE_EXPENSE.equals(t.getType())
                            && t.getCategoryId() != null) {
                        Long cur = prevSpentMap.get(t.getCategoryId());
                        prevSpentMap.put(t.getCategoryId(),
                                (cur != null ? cur : 0L) + t.getAmount());
                    }
                }
            }
            recomputeRollover();
            updateUI();
        };
        prevTxLiveData = txRepo.observeMonth(uid, prevMonthKey);
        prevTxLiveData.observe(this, prevTxObserver);

        // ----- Mục tiêu tiết kiệm (phân bổ tiền để dành) -----
        goalObserver = list -> {
            savingsGoals = new ArrayList<>();
            if (list != null) {
                for (SavingsGoal g : list) {
                    if (!g.isArchived() && !g.isCompleted()) savingsGoals.add(g);
                }
            }
            updateGoalItems();
        };
        goalLiveData = goalRepo.observeAll(uid);
        goalLiveData.observe(this, goalObserver);
    }

    /**
     * Tính lại bản đồ cuốn chiếu từ dữ liệu tháng trước:
     * rollover(c) = prevAllocated(c) - prevSpent(c). Chỉ giữ giá trị khác 0.
     */
    private void recomputeRollover() {
        rolloverMap = new HashMap<>();
        java.util.Set<String> catIds = new java.util.HashSet<>();
        catIds.addAll(prevAllocatedMap.keySet());
        catIds.addAll(prevSpentMap.keySet());
        for (String catId : catIds) {
            long prevAlloc = prevAllocatedMap.containsKey(catId) ? prevAllocatedMap.get(catId) : 0L;
            long prevSpent = prevSpentMap.containsKey(catId) ? prevSpentMap.get(catId) : 0L;
            long roll = BudgetService.categoryRollover(prevAlloc, prevSpent);
            if (roll != 0L) rolloverMap.put(catId, roll);
        }
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
        BudgetService.BudgetPool pool = BudgetService.pool(effectiveIncome, totalAllocated);

        Log.d("BudgetAlloc", "updateUI: allocatedMap size=" + allocatedMap.size()
                + ", totalAllocated=" + totalAllocated + ", effectiveIncome=" + effectiveIncome
                + ", toBeBudgeted=" + pool.toBeBudgeted
                + ", expenseCategories=" + expenseCategories.size());

        binding.textAllocated.setText(MoneyFormat.formatLong(totalAllocated));
        binding.textExpectedIncome.setText(MoneyFormat.formatLong(effectiveIncome));

        // Zero-Based Budgeting: nếu phân bổ vượt thu nhập thì đổi nhãn thành
        // "Vượt phân bổ" và hiển thị số DƯƠNG (phần vượt), thay vì số âm khó hiểu.
        if (pool.isOverBudgeted()) {
            binding.textUnallocatedLabel.setText(getString(R.string.s1_over_budgeted));
            binding.textUnallocated.setText(MoneyFormat.formatLong(-pool.toBeBudgeted));
            binding.cardUnallocated.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.expense_red));
        } else {
            binding.textUnallocatedLabel.setText(getString(R.string.s1_unallocated_money));
            binding.textUnallocated.setText(MoneyFormat.formatLong(pool.toBeBudgeted));
            binding.cardUnallocated.setCardBackgroundColor(ContextCompat.getColor(this,
                    pool.isBalanced() ? R.color.income_green : R.color.saving_blue));
        }

        // Tổng cuốn chiếu từ tháng trước.
        long totalRollover = 0L;
        for (Long r : rolloverMap.values()) totalRollover += r;
        if (totalRollover != 0L) {
            String sign = totalRollover > 0 ? "+" : "";
            binding.textRolloverTotal.setText(getString(R.string.budget_rollover_in,
                    sign + MoneyFormat.formatLong(totalRollover)));
            binding.textRolloverTotal.setTextColor(ContextCompat.getColor(this,
                    totalRollover >= 0 ? R.color.income_green : R.color.expense_red));
            binding.textRolloverTotal.setVisibility(View.VISIBLE);
        } else {
            binding.textRolloverTotal.setVisibility(View.GONE);
        }

        updateEssentialItems();
        updateGoalItems();
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
        long rollover = rolloverMap.containsKey(cat.getId()) ? rolloverMap.get(cat.getId()) : 0L;

        // ZBB: phong bì danh mục = phân bổ tháng này + cuốn chiếu từ tháng trước.
        BudgetService.Envelope env = BudgetService.envelope(allocated, rollover, spent);
        long available = env.available;
        long remaining = Math.max(0L, env.remaining);
        long deficit = Math.max(0L, -env.remaining);
        int pct = env.usagePercent;

        ItemBudgetAllocSimpleBinding card = ItemBudgetAllocSimpleBinding.inflate(
                LayoutInflater.from(this), binding.layoutEssentialItems, false);

        card.textCategoryName.setText(cat.getName());

        // Cuốn chiếu (rollover)
        if (rollover != 0L) {
            String sign = rollover > 0 ? "+" : "";
            card.textRollover.setText(getString(R.string.budget_rollover_in,
                    sign + MoneyFormat.format(rollover)));
            card.textRollover.setTextColor(ContextCompat.getColor(this,
                    rollover >= 0 ? R.color.income_green : R.color.expense_red));
            card.textRollover.setVisibility(View.VISIBLE);
        } else {
            card.textRollover.setVisibility(View.GONE);
        }

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

        // Spent + remaining (mẫu số là tiền KHẢ DỤNG = phân bổ + cuốn chiếu)
        card.textSpent.setText(getString(R.string.budget_spent) + ": " + MoneyFormat.format(spent)
                + " / " + MoneyFormat.format(available));

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

    /**
     * Bù ngân sách cho một danh mục đang bội chi: lấy tiền từ phần CHƯA PHÂN BỔ
     * hoặc TRỪ từ một danh mục khác còn dư, rồi cộng vào danh mục đích.
     */
    private void showReallocationDialog(Category target, long deficitAmount) {
        if (uid == null) return;

        long totalAllocated = 0L;
        for (Long v : allocatedMap.values()) totalAllocated += v;
        final long unallocatedAvail = Math.max(0L, getEffectiveIncome() - totalAllocated);

        // Các danh mục khác còn dư (available - spent > 0) để làm nguồn bù.
        final List<Category> sourceCats = new ArrayList<>();
        final List<Long> sourceSpare = new ArrayList<>();
        for (Category c : expenseCategories) {
            if (c.getId() == null || c.getId().equals(target.getId())) continue;
            long alloc = allocatedMap.containsKey(c.getId()) ? allocatedMap.get(c.getId()) : 0L;
            if (alloc <= 0) continue;
            long roll = rolloverMap.containsKey(c.getId()) ? rolloverMap.get(c.getId()) : 0L;
            long spent = spentMap.containsKey(c.getId()) ? spentMap.get(c.getId()) : 0L;
            long spare = (alloc + roll) - spent;
            if (spare > 0) { sourceCats.add(c); sourceSpare.add(spare); }
        }

        if (unallocatedAvail <= 0 && sourceCats.isEmpty()) {
            Toast.makeText(this, getString(R.string.reallocation_no_source), Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_budget_reallocation, null);
        TextView textTargetCategory = view.findViewById(R.id.textTargetCategory);
        TextView textDeficitAmount = view.findViewById(R.id.textDeficitAmount);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroupSource);
        RadioButton radioUnallocated = view.findViewById(R.id.radioUnallocated);
        RadioButton radioOtherBudget = view.findViewById(R.id.radioOtherBudget);
        Spinner spinnerSource = view.findViewById(R.id.spinnerSourceBudget);
        EditText editAmount = view.findViewById(R.id.editAmount);
        TextView previewUnalloc = view.findViewById(R.id.textPreviewUnallocated);
        TextView previewSource = view.findViewById(R.id.textPreviewSource);
        TextView previewTarget = view.findViewById(R.id.textPreviewTarget);

        textTargetCategory.setText(target.getName());
        textDeficitAmount.setText(MoneyFormat.formatLong(deficitAmount));

        radioUnallocated.setText(getString(R.string.reallocation_unallocated,
                MoneyFormat.formatLong(unallocatedAvail)));
        radioUnallocated.setEnabled(unallocatedAvail > 0);
        radioOtherBudget.setText(getString(R.string.reallocation_other_category));
        radioOtherBudget.setEnabled(!sourceCats.isEmpty());

        // Spinner nguồn = các danh mục khác còn dư.
        List<String> sourceLabels = new ArrayList<>();
        for (int i = 0; i < sourceCats.size(); i++) {
            sourceLabels.add(getString(R.string.reallocation_from_budget,
                    sourceCats.get(i).getName(), MoneyFormat.formatLong(sourceSpare.get(i))));
        }
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sourceLabels);
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(sourceAdapter);

        MoneyInputFormatter.attach(editAmount);
        editAmount.setText(MoneyInputFormatter.format(deficitAmount));

        long targetAlloc = allocatedMap.containsKey(target.getId())
                ? allocatedMap.get(target.getId()) : 0L;

        // Hàm cập nhật xem trước.
        Runnable refreshPreview = () -> {
            long amount = MoneyInputFormatter.getRawValue(editAmount);
            boolean fromUnalloc = radioUnallocated.isChecked();
            previewTarget.setText(getString(R.string.reallocation_change, target.getName(),
                    MoneyFormat.formatLong(targetAlloc),
                    MoneyFormat.formatLong(targetAlloc + amount)));
            if (fromUnalloc) {
                previewUnalloc.setVisibility(View.VISIBLE);
                previewSource.setVisibility(View.GONE);
                previewUnalloc.setText(getString(R.string.reallocation_change,
                        getString(R.string.s1_unallocated_money),
                        MoneyFormat.formatLong(unallocatedAvail),
                        MoneyFormat.formatLong(unallocatedAvail - amount)));
            } else {
                previewUnalloc.setVisibility(View.GONE);
                int pos = spinnerSource.getSelectedItemPosition();
                if (pos >= 0 && pos < sourceCats.size()) {
                    Category src = sourceCats.get(pos);
                    long srcAlloc = allocatedMap.containsKey(src.getId())
                            ? allocatedMap.get(src.getId()) : 0L;
                    previewSource.setVisibility(View.VISIBLE);
                    previewSource.setText(getString(R.string.reallocation_change, src.getName(),
                            MoneyFormat.formatLong(srcAlloc),
                            MoneyFormat.formatLong(srcAlloc - amount)));
                } else {
                    previewSource.setVisibility(View.GONE);
                }
            }
        };

        radioGroup.setOnCheckedChangeListener((g, checkedId) -> {
            spinnerSource.setVisibility(
                    checkedId == R.id.radioOtherBudget ? View.VISIBLE : View.GONE);
            refreshPreview.run();
        });
        spinnerSource.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                refreshPreview.run();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        editAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refreshPreview.run(); }
        });

        // Mặc định chọn nguồn khả dụng.
        if (unallocatedAvail > 0) {
            radioUnallocated.setChecked(true);
            spinnerSource.setVisibility(View.GONE);
        } else {
            radioOtherBudget.setChecked(true);
            spinnerSource.setVisibility(View.VISIBLE);
        }
        refreshPreview.run();

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reallocation_title))
                .setView(view)
                .setPositiveButton(getString(R.string.reallocation_confirm), (d, w) -> {
                    long amount = MoneyInputFormatter.getRawValue(editAmount);
                    if (amount <= 0) {
                        Toast.makeText(this, getString(R.string.error_invalid_amount),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (radioUnallocated.isChecked()) {
                        if (amount > unallocatedAvail) {
                            Toast.makeText(this, getString(R.string.reallocation_exceed_source),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        applyReallocation(target, targetAlloc + amount, null, 0L, amount);
                    } else {
                        int pos = spinnerSource.getSelectedItemPosition();
                        if (pos < 0 || pos >= sourceCats.size()) return;
                        if (amount > sourceSpare.get(pos)) {
                            Toast.makeText(this, getString(R.string.reallocation_exceed_source),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Category src = sourceCats.get(pos);
                        long srcAlloc = allocatedMap.containsKey(src.getId())
                                ? allocatedMap.get(src.getId()) : 0L;
                        applyReallocation(target, targetAlloc + amount, src, srcAlloc - amount, amount);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /** Lưu phân bổ mới cho danh mục đích (và danh mục nguồn nếu có). */
    private void applyReallocation(Category target, long newTargetAlloc,
                                   Category source, long newSourceAlloc, long amount) {
        if (uid == null) return;

        Budget tb = new Budget();
        tb.setScope(Budget.SCOPE_CATEGORY);
        tb.setCategoryId(target.getId());
        tb.setMonth(getMonthKey());
        tb.setAllocatedAmount(newTargetAlloc);
        budgetRepo.addOrUpdate(uid, tb);
        allocatedMap.put(target.getId(), newTargetAlloc);

        if (source != null) {
            Budget sb = new Budget();
            sb.setScope(Budget.SCOPE_CATEGORY);
            sb.setCategoryId(source.getId());
            sb.setMonth(getMonthKey());
            sb.setAllocatedAmount(Math.max(0L, newSourceAlloc));
            budgetRepo.addOrUpdate(uid, sb);
            allocatedMap.put(source.getId(), Math.max(0L, newSourceAlloc));
        }

        updateUI();
        Toast.makeText(this, getString(R.string.reallocation_success,
                MoneyFormat.formatLong(amount)), Toast.LENGTH_SHORT).show();
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

    /** Tạo MỚI một danh mục chi (nhập tên + chọn nhóm), không phải chọn danh mục có sẵn. */
    private void showAddCategoryDialog() {
        if (uid == null) return;

        final EditText input = new EditText(this);
        input.setHint(getString(R.string.category_name));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

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

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 16);
        container.addView(input);
        container.addView(radioView);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_category))
                .setView(container)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.j2_enter_category_name),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Category c = new Category();
                    c.setName(name);
                    c.setType(Category.TYPE_EXPENSE);
                    c.setGroup(groupValues[selectedGroup[0]]);
                    categoryRepo.add(uid, c);
                    Toast.makeText(this, getString(R.string.j1_category_created, name),
                            Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton(getString(R.string.cancel), null)
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

    // ----- Mục tiêu tiết kiệm -----

    private void updateGoalItems() {
        if (binding == null) return;
        binding.layoutGoalItems.removeAllViews();

        if (savingsGoals.isEmpty()) {
            binding.textNoGoals.setVisibility(View.VISIBLE);
            return;
        }
        binding.textNoGoals.setVisibility(View.GONE);

        for (SavingsGoal g : savingsGoals) {
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_budget_goal, binding.layoutGoalItems, false);

            ((android.widget.TextView) row.findViewById(R.id.textGoalTitle)).setText(g.getTitle());

            int pct = Math.round(g.getProgress() * 100f);
            android.widget.TextView percent = row.findViewById(R.id.textGoalPercent);
            percent.setText(pct + "%");

            LinearProgressIndicator bar = row.findViewById(R.id.progressGoal);
            bar.setProgress(Math.max(0, Math.min(100, pct)));

            ((android.widget.TextView) row.findViewById(R.id.textGoalAmounts)).setText(
                    getString(R.string.dash_goal_progress_value,
                            MoneyFormat.formatLong(g.getSavedAmount()),
                            MoneyFormat.formatLong(g.getTargetAmount())));

            row.findViewById(R.id.btnAllocateGoal)
                    .setOnClickListener(v -> showGoalContributeDialog(g));

            binding.layoutGoalItems.addView(row);
        }
    }

    /** Phân bổ (đóng góp) tiền vào mục tiêu, lấy từ ví nguồn đã gắn của mục tiêu. */
    private void showGoalContributeDialog(SavingsGoal g) {
        if (uid == null) return;
        if (g.getWalletId() == null || g.getWalletId().isEmpty()) {
            Toast.makeText(this, getString(R.string.budget_goal_no_wallet), Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint(getString(R.string.j1_allocation_amount_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(input);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.budget_goal_allocate_to, g.getTitle()))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    long amount = MoneyInputFormatter.getRawValue(input);
                    if (amount <= 0) {
                        Toast.makeText(this, getString(R.string.error_invalid_amount),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    goalService.contributeToGoal(uid, g.getId(), amount, g.getWalletId())
                            .addOnSuccessListener(unused -> Toast.makeText(this,
                                    getString(R.string.budget_goal_contributed,
                                            MoneyFormat.formatLong(amount)),
                                    Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    getString(R.string.j1_save_error, e.getMessage()),
                                    Toast.LENGTH_LONG).show());
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
        if (prevTxLiveData != null && prevTxObserver != null) {
            prevTxLiveData.removeObserver(prevTxObserver);
        }
        if (prevBudgetLiveData != null && prevBudgetObserver != null) {
            prevBudgetLiveData.removeObserver(prevBudgetObserver);
        }
        if (goalLiveData != null && goalObserver != null) {
            goalLiveData.removeObserver(goalObserver);
        }
        binding = null;
    }
}
