package com.expensemanager.app.ui.recurring;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.RecurringRule;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.RecurringRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.ActivityRecurringListBinding;
import com.expensemanager.app.ui.adapter.RecurringAdapter;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyInputFormatter;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecurringListActivity extends AppCompatActivity {
    private ActivityRecurringListBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final RecurringRepository recurringRepo = new RecurringRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private RecurringAdapter adapter;
    private List<Category> categories = new ArrayList<>();
    private List<Category> incomeCategories = new ArrayList<>();
    private List<Category> expenseCategories = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecurringListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        setupToolbar();
        setupRecyclerView();
        loadData();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.recurring);
        }
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new RecurringAdapter();
        binding.recyclerRecurring.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRecurring.setAdapter(adapter);

        adapter.setOnToggle((r, enabled) -> {
            r.setEnabled(enabled);
            recurringRepo.update(uid, r);
        });

        adapter.setOnItemClick(r -> showEditDialog(r));
        binding.fabAdd.setOnClickListener(v -> showAddDialog());
    }

    private void loadData() {
        categoryRepo.observeAll(uid).observe(this, list -> {
            categories = list != null ? list : new ArrayList<>();
            incomeCategories = new ArrayList<>();
            expenseCategories = new ArrayList<>();
            for (Category c : categories) {
                if (Category.TYPE_INCOME.equals(c.getType())) {
                    incomeCategories.add(c);
                } else if (Category.TYPE_EXPENSE.equals(c.getType())) {
                    expenseCategories.add(c);
                }
            }
            java.util.Map<String, Category> map = new java.util.HashMap<>();
            for (Category c : categories) map.put(c.getId(), c);
            adapter.setCategoryMap(map);
        });

        walletRepo.observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
        });

        recurringRepo.observeAll(uid).observe(this, list -> {
            adapter.setItems(list != null ? list : new ArrayList<>());
            binding.textEmpty.setVisibility(
                    list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showAddDialog() {
        if (categories.isEmpty()) {
            Toast.makeText(this, R.string.error_no_category, Toast.LENGTH_SHORT).show();
            return;
        }
        if (wallets.isEmpty()) {
            Toast.makeText(this, R.string.error_no_wallet, Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_recurring, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.add_recurring)
                .setView(view)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Bind UI elements
        RadioGroup radioType = view.findViewById(R.id.radioGroupType);
        View labelCategory = view.findViewById(R.id.labelRecurringDay);
        Spinner spinnerCat = view.findViewById(R.id.spinnerRecurringCategory);
        Spinner spinnerWallet = view.findViewById(R.id.spinnerRecurringWallet);
        Spinner spinnerCycle = view.findViewById(R.id.spinnerRecurringCycle);
        Spinner spinnerDay = view.findViewById(R.id.spinnerRecurringDay);
        Spinner spinnerMonth = view.findViewById(R.id.spinnerRecurringMonth);
        TextView labelDay = view.findViewById(R.id.labelRecurringDay);
        TextView labelMonth = view.findViewById(R.id.labelRecurringMonth);
        Button btnDateStart = view.findViewById(R.id.btnRecurringDateStart);
        Button btnDateEnd = view.findViewById(R.id.btnRecurringDateEnd);

        MoneyInputFormatter.attach(view.findViewById(R.id.editRecurringAmount));

        // Setup spinners
        updateCategorySpinner(spinnerCat, expenseCategories);
        setupWalletSpinner(spinnerWallet);
        setupCycleSpinner(spinnerCycle, labelDay, spinnerDay, labelMonth, spinnerMonth);

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

        final Timestamp[] dateStart = {new Timestamp(DateUtils.nowVietnam())};
        final Timestamp[] dateEnd = {null};
        btnDateStart.setText(dateFmt.format(dateStart[0].toDate()));
        btnDateStart.setOnClickListener(v -> {
            Calendar c = DateUtils.newCalendar();
            new DatePickerDialog(this, (dp, year, month, day) -> {
                c.set(year, month, day, 0, 0, 0);
                dateStart[0] = new Timestamp(c.getTime());
                btnDateStart.setText(dateFmt.format(c.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        btnDateEnd.setOnClickListener(v -> {
            Calendar c = DateUtils.newCalendar();
            new DatePickerDialog(this, (dp, year, month, day) -> {
                c.set(year, month, day, 23, 59, 59);
                dateEnd[0] = new Timestamp(c.getTime());
                btnDateEnd.setText(dateFmt.format(c.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Override positive button to handle validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!validateAndSave(view, dateStart[0], dateEnd[0])) {
                return;
            }
            dialog.dismiss();
        });
    }

    private void showEditDialog(RecurringRule rule) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_recurring, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(rule.getNote())
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete, (d, w) -> {
                    recurringRepo.delete(uid, rule.getId());
                    Toast.makeText(this, R.string.success_delete, Toast.LENGTH_SHORT).show();
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        RadioGroup radioType = view.findViewById(R.id.radioGroupType);
        Spinner spinnerCat = view.findViewById(R.id.spinnerRecurringCategory);
        Spinner spinnerWallet = view.findViewById(R.id.spinnerRecurringWallet);
        Spinner spinnerCycle = view.findViewById(R.id.spinnerRecurringCycle);
        Spinner spinnerDay = view.findViewById(R.id.spinnerRecurringDay);
        Spinner spinnerMonth = view.findViewById(R.id.spinnerRecurringMonth);
        TextView labelDay = view.findViewById(R.id.labelRecurringDay);
        TextView labelMonth = view.findViewById(R.id.labelRecurringMonth);
        Button btnDateStart = view.findViewById(R.id.btnRecurringDateStart);
        Button btnDateEnd = view.findViewById(R.id.btnRecurringDateEnd);
        com.google.android.material.textfield.TextInputEditText editAmount =
                view.findViewById(R.id.editRecurringAmount);
        com.google.android.material.textfield.TextInputEditText editNote =
                view.findViewById(R.id.editRecurringNote);

        MoneyInputFormatter.attach(editAmount);
        editNote.setText(rule.getNote());
        editAmount.setText(MoneyFormat.format(rule.getAmount()));

        // Set type
        if (rule.isIncome()) {
            radioType.check(R.id.radioIncome);
            updateCategorySpinner(spinnerCat, incomeCategories);
        } else {
            radioType.check(R.id.radioExpense);
            updateCategorySpinner(spinnerCat, expenseCategories);
        }

        setupWalletSpinner(spinnerWallet);
        // Select wallet
        for (int i = 0; i < wallets.size(); i++) {
            if (wallets.get(i).getId().equals(rule.getWalletId())) {
                spinnerWallet.setSelection(i);
                break;
            }
        }

        // Set category
        List<Category> catList = rule.isIncome() ? incomeCategories : expenseCategories;
        for (int i = 0; i < catList.size(); i++) {
            if (catList.get(i).getId().equals(rule.getCategoryId())) {
                spinnerCat.setSelection(i);
                break;
            }
        }

        setupCycleSpinner(spinnerCycle, labelDay, spinnerDay, labelMonth, spinnerMonth);

        // Select cycle
        String cycle = rule.getCycleType();
        int cyclePos = "daily".equals(cycle) ? 0 : "weekly".equals(cycle) ? 1
                : "yearly".equals(cycle) ? 3 : 2;
        spinnerCycle.setSelection(cyclePos);

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
        final Timestamp[] dateStart = {rule.getDateStart()};
        final Timestamp[] dateEnd = {rule.getDateEnd()};
        btnDateStart.setText(dateFmt.format(dateStart[0].toDate()));
        btnDateEnd.setText(dateEnd[0] != null ? dateFmt.format(dateEnd[0].toDate())
                : getString(R.string.recurring_no_end));
        btnDateStart.setOnClickListener(v -> {
            Calendar c = DateUtils.newCalendar();
            new DatePickerDialog(this, (dp, year, month, day) -> {
                c.set(year, month, day, 0, 0, 0);
                dateStart[0] = new Timestamp(c.getTime());
                btnDateStart.setText(dateFmt.format(c.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        btnDateEnd.setOnClickListener(v -> {
            Calendar c = DateUtils.newCalendar();
            new DatePickerDialog(this, (dp, year, month, day) -> {
                c.set(year, month, day, 23, 59, 59);
                dateEnd[0] = new Timestamp(c.getTime());
                btnDateEnd.setText(dateFmt.format(c.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!validateAndSave(view, dateStart[0], dateEnd[0])) {
                return;
            }
            dialog.dismiss();
        });
    }

    private void updateCategorySpinner(Spinner spinner, List<Category> catList) {
        List<String> names = new ArrayList<>();
        for (Category c : catList) names.add(c.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupWalletSpinner(Spinner spinner) {
        List<String> names = new ArrayList<>();
        for (Wallet w : wallets) names.add(w.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupCycleSpinner(Spinner spinnerCycle, TextView labelDay, Spinner spinnerDay,
                                  TextView labelMonth, Spinner spinnerMonth) {
        String[] cycleLabels = {
                getString(R.string.repeat_daily),
                getString(R.string.repeat_weekly),
                getString(R.string.repeat_monthly),
                getString(R.string.repeat_yearly)
        };
        ArrayAdapter<String> cycleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, java.util.Arrays.asList(cycleLabels));
        cycleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCycle.setAdapter(cycleAdapter);

        spinnerCycle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                labelDay.setVisibility(View.VISIBLE);
                spinnerDay.setVisibility(View.VISIBLE);
                labelMonth.setVisibility(View.GONE);
                spinnerMonth.setVisibility(View.GONE);

                String cycle = pos == 0 ? "daily" : pos == 1 ? "weekly"
                        : pos == 3 ? "yearly" : "monthly";

                if (pos == 3) {
                    labelMonth.setVisibility(View.VISIBLE);
                    spinnerMonth.setVisibility(View.VISIBLE);
                }

                String[] dayLabels = buildDayLabels(cycle);
                ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(
                        RecurringListActivity.this,
                        android.R.layout.simple_spinner_item,
                        java.util.Arrays.asList(dayLabels));
                dayAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item);
                spinnerDay.setAdapter(dayAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private String[] buildDayLabels(String cycle) {
        switch (cycle) {
            case "weekly":
                return new String[]{
                        getString(R.string.sunday), getString(R.string.monday),
                        getString(R.string.tuesday), getString(R.string.wednesday),
                        getString(R.string.thursday), getString(R.string.friday),
                        getString(R.string.saturday)
                };
            case "yearly":
                String[] days = new String[31];
                for (int i = 0; i < 31; i++) days[i] = getString(R.string.recurring_day_n, i + 1);
                return days;
            case "monthly":
            default:
                String[] monthly = new String[32];
                for (int i = 0; i < 31; i++) monthly[i] = getString(R.string.recurring_day_n, i + 1);
                monthly[31] = getString(R.string.recurring_last_day_of_month);
                return monthly;
        }
    }

    private boolean validateAndSave(View dialogView, Timestamp dateStart, Timestamp dateEnd) {
        RadioGroup radioType = dialogView.findViewById(R.id.radioGroupType);
        Spinner spinnerCat = dialogView.findViewById(R.id.spinnerRecurringCategory);
        Spinner spinnerWallet = dialogView.findViewById(R.id.spinnerRecurringWallet);
        Spinner spinnerCycle = dialogView.findViewById(R.id.spinnerRecurringCycle);
        Spinner spinnerDay = dialogView.findViewById(R.id.spinnerRecurringDay);
        Spinner spinnerMonth = dialogView.findViewById(R.id.spinnerRecurringMonth);
        com.google.android.material.textfield.TextInputEditText editNote =
                dialogView.findViewById(R.id.editRecurringNote);
        com.google.android.material.textfield.TextInputEditText editAmount =
                dialogView.findViewById(R.id.editRecurringAmount);

        long amount = MoneyInputFormatter.getRawValue(editAmount);
        if (amount <= 0) {
            Toast.makeText(this, R.string.error_empty_amount, Toast.LENGTH_SHORT).show();
            return false;
        }

        boolean isIncome = radioType.getCheckedRadioButtonId() == R.id.radioIncome;
        String type = isIncome ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;

        List<Category> catList = isIncome ? incomeCategories : expenseCategories;
        int catPos = spinnerCat.getSelectedItemPosition();
        int walletPos = spinnerWallet.getSelectedItemPosition();
        int cyclePos = spinnerCycle.getSelectedItemPosition();
        int dayPos = spinnerDay.getSelectedItemPosition();
        int monthPos = spinnerMonth.getSelectedItemPosition();

        if (catPos < 0 || catPos >= catList.size()) {
            Toast.makeText(this, R.string.error_select_category, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (walletPos < 0 || walletPos >= wallets.size()) {
            Toast.makeText(this, R.string.error_select_wallet, Toast.LENGTH_SHORT).show();
            return false;
        }

        Category cat = catList.get(catPos);
        Wallet wallet = wallets.get(walletPos);
        String note = editNote.getText().toString().trim();

        String cycleType = cyclePos == 0 ? "daily" : cyclePos == 1 ? "weekly"
                : cyclePos == 3 ? "yearly" : "monthly";

        int dayOfMonth = 0, dayOfWeek = 0, monthOfYear = 0;
        boolean useLastDay = false;

        switch (cycleType) {
            case "weekly":
                dayOfWeek = dayPos + 1;
                break;
            case "monthly":
                if (dayPos == 31) {
                    useLastDay = true;
                } else {
                    dayOfMonth = dayPos + 1;
                }
                break;
            case "yearly":
                dayOfMonth = dayPos + 1;
                monthOfYear = monthPos + 1;
                break;
        }

        RecurringRule r = new RecurringRule();
        r.setType(type);
        r.setAmount(amount);
        r.setCategoryId(cat.getId());
        r.setWalletId(wallet.getId());
        r.setNote(note);
        r.setCycleType(cycleType);
        r.setDayOfMonth(dayOfMonth);
        r.setDayOfWeek(dayOfWeek);
        r.setMonthOfYear(monthOfYear);
        r.setUseLastDayOfMonth(useLastDay);
        r.setDateStart(dateStart);
        r.setDateEnd(dateEnd);
        r.setEnabled(true);
        r.setNextRun(RecurringRepository.calculateNextRun(r));

        recurringRepo.add(uid, r,
                unused -> runOnUiThread(() -> {
                    Toast.makeText(this, R.string.success_recurring_created,
                            Toast.LENGTH_SHORT).show();
                }),
                e -> runOnUiThread(() ->
                        Toast.makeText(this,
                                R.string.error_saving_failed, Toast.LENGTH_SHORT).show())
        );

        return true;
    }
}
