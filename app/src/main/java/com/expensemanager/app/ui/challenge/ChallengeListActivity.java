package com.expensemanager.app.ui.challenge;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.expensemanager.app.databinding.ActivityChallengeListBinding;
import com.expensemanager.app.ui.adapter.RecurringAdapter;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyValueParser;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChallengeListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final RecurringRepository recurringRepo = new RecurringRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private ActivityChallengeListBinding binding;
    private RecurringAdapter adapter;
    private List<Category> categories = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChallengeListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.recurring);
        }

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        RecyclerView rv = binding.recyclerRecurring;
        adapter = new RecurringAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        categoryRepo.observeAll(uid).observe(this, list -> {
            categories = list != null ? list : new ArrayList<>();
            java.util.Map<String, Category> map = new java.util.HashMap<>();
            for (Category c : categories) map.put(c.getId(), c);
            adapter.setCategoryMap(map);
        });

        new WalletRepository().observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
        });

        recurringRepo.observeAll(uid).observe(this, list -> {
            adapter.setItems(list != null ? list : new ArrayList<>());
            binding.textEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        adapter.setOnToggle((r, enabled) -> {
            r.setEnabled(enabled);
            recurringRepo.update(uid, r);
        });

        adapter.setOnItemClick(r -> showEditDialog(uid, r));
        binding.fabAdd.setOnClickListener(v -> showAddDialog(uid));
    }

    private void showAddDialog(String uid) {
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
        EditText editNote = view.findViewById(R.id.editRecurringNote);
        EditText editAmount = view.findViewById(R.id.editRecurringAmount);
        Spinner spinnerCat = view.findViewById(R.id.spinnerRecurringCategory);
        Spinner spinnerWallet = view.findViewById(R.id.spinnerRecurringWallet);
        Spinner spinnerCycle = view.findViewById(R.id.spinnerRecurringCycle);
        Spinner spinnerDay = view.findViewById(R.id.spinnerRecurringDay);
        Spinner spinnerMonth = view.findViewById(R.id.spinnerRecurringMonth);
        TextView labelDay = view.findViewById(R.id.labelRecurringDay);
        TextView labelMonth = view.findViewById(R.id.labelRecurringMonth);
        Button btnDateStart = view.findViewById(R.id.btnRecurringDateStart);
        Button btnDateEnd = view.findViewById(R.id.btnRecurringDateEnd);

        List<String> catNames = new ArrayList<>();
        for (Category c : categories) catNames.add(c.getName());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, catNames);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCat.setAdapter(catAdapter);

        List<String> walletNames = new ArrayList<>();
        for (Wallet w : wallets) walletNames.add(w.getName());
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, walletNames);
        walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWallet.setAdapter(walletAdapter);

        String[] cycleLabels = {"Ngày", "Tuần", "Tháng", "Năm"};
        ArrayAdapter<String> cycleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, java.util.Arrays.asList(cycleLabels));
        cycleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCycle.setAdapter(cycleAdapter);

        String[] monthNames = {
                "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
                "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
                "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        };
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, java.util.Arrays.asList(monthNames));
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        final Timestamp[] dateStart = {new Timestamp(DateUtils.nowVietnam())};
        final Timestamp[] dateEnd = {null};
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy",
                Locale.forLanguageTag("vi-VN"));

        btnDateStart.setText(dateFormat.format(dateStart[0].toDate()));

        btnDateStart.setOnClickListener(v -> {
            java.util.Calendar c = DateUtils.newCalendar();
            new DatePickerDialog(this, (dp, year, month, day) -> {
                c.set(year, month, day, 0, 0, 0);
                dateStart[0] = new Timestamp(c.getTime());
                btnDateStart.setText(dateFormat.format(c.getTime()));
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
               c.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        btnDateEnd.setOnClickListener(v -> {
            java.util.Calendar c = DateUtils.newCalendar();
            new DatePickerDialog(this, (dp, year, month, day) -> {
                c.set(year, month, day, 23, 59, 59);
                dateEnd[0] = new Timestamp(c.getTime());
                btnDateEnd.setText(dateFormat.format(c.getTime()));
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
               c.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        final String[] dayLabels = buildDayLabels(RecurringRule.CYCLE_MONTHLY);
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, java.util.Arrays.asList(dayLabels));
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);

        spinnerCycle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String cycle = getCycleTypeFromPosition(pos);
                String[] labels = buildDayLabels(cycle);
                dayAdapter.clear();
                dayAdapter.addAll(labels);
                dayAdapter.notifyDataSetChanged();

                labelDay.setText(getDayLabelForCycle(cycle));
                labelDay.setVisibility(View.VISIBLE);
                spinnerDay.setVisibility(View.VISIBLE);

                labelMonth.setVisibility(View.GONE);
                spinnerMonth.setVisibility(View.GONE);
                if (RecurringRule.CYCLE_YEARLY.equals(cycle)) {
                    labelMonth.setVisibility(View.VISIBLE);
                    spinnerMonth.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_recurring)
                .setView(view)
                .setPositiveButton(R.string.create, (d, w) -> {
                    String note = editNote.getText().toString().trim();
                    Long amount = MoneyValueParser.tryParseStrict(
                            editAmount.getText().toString().trim());
                    if (amount == null) {
                        Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int catPos = spinnerCat.getSelectedItemPosition();
                    int walletPos = spinnerWallet.getSelectedItemPosition();
                    int cyclePos = spinnerCycle.getSelectedItemPosition();
                    int dayPos = spinnerDay.getSelectedItemPosition();
                    int monthPos = spinnerMonth.getSelectedItemPosition();

                    if (catPos < 0 || catPos >= categories.size()
                            || walletPos < 0 || walletPos >= wallets.size()) {
                        Toast.makeText(this, R.string.error_select_category_wallet,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Category cat = categories.get(catPos);
                    Wallet wallet = wallets.get(walletPos);

                    String cycleType = getCycleTypeFromPosition(cyclePos);
                    int dayOfMonth = 0, dayOfWeek = 0, monthOfYear = 0;

                    switch (cycleType) {
                        case RecurringRule.CYCLE_DAILY:
                            break;
                        case RecurringRule.CYCLE_WEEKLY:
                            dayOfWeek = dayPos + 1;
                            break;
                        case RecurringRule.CYCLE_MONTHLY:
                            dayOfMonth = dayPos + 1;
                            break;
                        case RecurringRule.CYCLE_YEARLY:
                            dayOfMonth = dayPos + 1;
                            monthOfYear = monthPos + 1;
                            break;
                    }

                    RecurringRule r = new RecurringRule();
                    r.setType(Transaction.TYPE_EXPENSE);
                    r.setAmount(amount);
                    r.setCategoryId(cat.getId());
                    r.setWalletId(wallet.getId());
                    r.setNote(note);
                    r.setCycleType(cycleType);
                    r.setDayOfMonth(dayOfMonth);
                    r.setDayOfWeek(dayOfWeek);
                    r.setMonthOfYear(monthOfYear);
                    r.setDateStart(dateStart[0]);
                    r.setDateEnd(dateEnd[0]);
                    r.setEnabled(true);
                    r.setNextRun(RecurringRepository.calculateNextRun(r));
                    recurringRepo.add(uid, r);
                    Toast.makeText(this, R.string.success_recurring_created,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getCycleTypeFromPosition(int pos) {
        switch (pos) {
            case 0: return RecurringRule.CYCLE_DAILY;
            case 1: return RecurringRule.CYCLE_WEEKLY;
            case 3: return RecurringRule.CYCLE_YEARLY;
            default: return RecurringRule.CYCLE_MONTHLY;
        }
    }

    private String getDayLabelForCycle(String cycle) {
        switch (cycle) {
            case RecurringRule.CYCLE_WEEKLY: return "Thứ trong tuần";
            case RecurringRule.CYCLE_YEARLY: return "Ngày trong tháng";
            default: return "Ngày trong tháng";
        }
    }

    private String[] buildDayLabels(String cycle) {
        switch (cycle) {
            case RecurringRule.CYCLE_WEEKLY:
                return new String[]{
                        "Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4",
                        "Thứ 5", "Thứ 6", "Thứ 7"
                };
            case RecurringRule.CYCLE_YEARLY:
                String[] days = new String[31];
                for (int i = 0; i < 31; i++) days[i] = "Ngày " + (i + 1);
                return days;
            case RecurringRule.CYCLE_MONTHLY:
            default:
                String[] monthly = new String[32];
                for (int i = 0; i < 31; i++) monthly[i] = "Ngày " + (i + 1);
                monthly[31] = "Ngày cuối tháng";
                return monthly;
        }
    }

    private void showEditDialog(String uid, RecurringRule r) {
        EditText input = new EditText(this);
        input.setHint(R.string.hint_amount);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(r.getAmount()));

        new AlertDialog.Builder(this)
                .setTitle(r.getNote())
                .setView(input)
                .setPositiveButton(R.string.save, (d, w) -> {
                    Long amount = MoneyValueParser.tryParseStrict(
                            input.getText().toString().trim());
                    if (amount == null) {
                        Toast.makeText(this, R.string.error_invalid_amount,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    r.setAmount(amount);
                    recurringRepo.update(uid, r);
                    Toast.makeText(this, R.string.success_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete, (d, w) -> {
                    recurringRepo.delete(uid, r.getId());
                    Toast.makeText(this, R.string.success_deleted, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
