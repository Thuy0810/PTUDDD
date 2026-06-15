package com.expensemanager.app.ui.transaction;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.databinding.FragmentTransactionListBinding;
import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.ui.adapter.TransactionAdapter;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyInputFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TransactionListFragment extends Fragment {
    private FragmentTransactionListBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private final CategoryRepository catRepo = new CategoryRepository();
    private TransactionAdapter adapter;
    private List<Transaction> allTxs = new ArrayList<>();
    private Map<String, Category> catMap;
    private List<Wallet> wallets = new ArrayList<>();
    private Map<String, Wallet> walletMap;

    private SharedPreferences prefs;
    private String uid;
    private int selectedSort = 0; // 0=moi nhat, 1=lon nhat

    // Date range state
    private String selectedMonthKey;
    private DateRangeMode dateMode = DateRangeMode.MONTH;

    private enum DateRangeMode { MONTH, RANGE }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        uid = authRepo.getUid();
        if (uid == null) return;

        prefs = requireContext().getSharedPreferences("txlist_" + uid, Context.MODE_PRIVATE);
        selectedSort = prefs.getInt("sort", 0);

        adapter = new TransactionAdapter();
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTransactions.setAdapter(adapter);

        adapter.setOnItemClick(t -> {
            Intent i = new Intent(requireContext(), AddTransactionActivity.class);
            i.putExtra(AddTransactionActivity.EXTRA_TX_ID, t.getId());
            startActivity(i);
        });

        catRepo.observeAll(uid).observe(getViewLifecycleOwner(), cats -> {
            catMap = CategoryRepository.toMap(cats);
            adapter.setCategoryMap(catMap);
        });

        new WalletRepository().observeAll(uid).observe(getViewLifecycleOwner(), list -> {
            wallets = list != null ? list : new ArrayList<>();
            walletMap = new java.util.HashMap<>();
            for (Wallet w : wallets) {
                if (w.getId() != null) walletMap.put(w.getId(), w);
            }
            adapter.setWalletMap(walletMap);
            setupWalletSpinner();
        });

        setupDateRangeButton();
        setupSortSpinner();
        setupTypeFilter();
        setupAmountFilter();
        setupSearch();

        loadTransactions();
    }

    private void loadTransactions() {
        if (dateMode == DateRangeMode.RANGE) {
            txRepo.observeRange(uid, parseDateRangeStart(selectedMonthKey),
                    parseDateRangeEnd(selectedMonthKey))
                    .observe(getViewLifecycleOwner(), list -> {
                        allTxs = list != null ? list : new ArrayList<>();
                        applyFilter();
                    });
        } else {
            txRepo.observeMonth(uid, selectedMonthKey != null ? selectedMonthKey : DateUtils.currentMonthKey())
                    .observe(getViewLifecycleOwner(), list -> {
                        allTxs = list != null ? list : new ArrayList<>();
                        applyFilter();
                    });
        }
    }

    private java.util.Date parseDateRangeStart(String key) {
        try {
            String[] parts = key.split("_");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            Calendar c = Calendar.getInstance();
            c.set(y, m - 1, d, 0, 0, 0);
            return c.getTime();
        } catch (Exception e) {
            return new java.util.Date();
        }
    }

    private java.util.Date parseDateRangeEnd(String key) {
        try {
            String[] parts = key.split("_");
            int y = Integer.parseInt(parts[3]);
            int m = Integer.parseInt(parts[4]);
            int d = Integer.parseInt(parts[5]);
            Calendar c = Calendar.getInstance();
            c.set(y, m - 1, d, 23, 59, 59);
            return c.getTime();
        } catch (Exception e) {
            return new java.util.Date();
        }
    }

    private void setupDateRangeButton() {
        selectedMonthKey = DateUtils.currentMonthKey();
        binding.btnDateRange.setText(getString(R.string.j2_this_month));

        binding.btnDateRange.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        Calendar startCal = Calendar.getInstance();
                        startCal.set(year, month, 1, 0, 0, 0);
                        Calendar endCal = Calendar.getInstance();
                        endCal.set(year, month, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                        selectedMonthKey = String.format("%04d_%02d_%02d_%04d_%02d_%02d",
                                year, month + 1, 1,
                                year, month + 1, endCal.get(Calendar.DAY_OF_MONTH));
                        dateMode = DateRangeMode.RANGE;
                        binding.btnDateRange.setText(getString(R.string.j2_from_day, day, (month + 1)));
                        loadTransactions();
                    }, Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH), 1);
            dpd.show();
        });

        binding.btnDateRange.setOnLongClickListener(v -> {
            selectedMonthKey = DateUtils.currentMonthKey();
            dateMode = DateRangeMode.MONTH;
            binding.btnDateRange.setText(getString(R.string.j2_this_month));
            loadTransactions();
            return true;
        });
    }

    private void setupSortSpinner() {
        String[] sortLabels = getResources().getStringArray(R.array.sort_labels);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, java.util.Arrays.asList(sortLabels));
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSort.setAdapter(adapter2);
        binding.spinnerSort.setSelection(selectedSort);

        binding.spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedSort = pos;
                prefs.edit().putInt("sort", pos).apply();
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupWalletSpinner() {
        List<String> names = new ArrayList<>();
        names.add(getString(R.string.all_wallets));
        for (Wallet w : wallets) names.add(w.getName());

        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, names);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerWallet.setAdapter(adapter3);

        binding.spinnerWallet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupTypeFilter() {
        binding.radioGroupType.setOnCheckedChangeListener((group, checkedId) -> applyFilter());
    }

    private void setupAmountFilter() {
        MoneyInputFormatter.attach(binding.editMinAmount);
        MoneyInputFormatter.attach(binding.editMaxAmount);
        TextWatcher amountWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        binding.editMinAmount.addTextChangedListener(amountWatcher);
        binding.editMaxAmount.addTextChangedListener(amountWatcher);
    }

    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        String query = binding.editSearch != null && binding.editSearch.getText() != null
                ? binding.editSearch.getText().toString() : "";
        List<Transaction> filtered = TransactionRepository.filterBySearch(allTxs, query);

        // Type filter
        String typeFilter = getSelectedType();
        if (!"all".equals(typeFilter)) {
            List<Transaction> typed = new ArrayList<>();
            for (Transaction t : filtered) {
                if (typeFilter.equals(t.getType())) typed.add(t);
            }
            filtered = typed;
        }

        // Wallet filter
        int walletPos = binding.spinnerWallet.getSelectedItemPosition();
        if (walletPos > 0) {
            int idx = walletPos - 1;
            if (idx < wallets.size()) {
                String walletId = wallets.get(idx).getId();
                List<Transaction> walletFiltered = new ArrayList<>();
                for (Transaction t : filtered) {
                    if (walletId.equals(t.getWalletId())) {
                        walletFiltered.add(t);
                    }
                }
                filtered = walletFiltered;
            }
        }

        // Amount range filter
        String minStr = binding.editMinAmount != null && binding.editMinAmount.getText() != null
                ? binding.editMinAmount.getText().toString() : "";
        String maxStr = binding.editMaxAmount != null && binding.editMaxAmount.getText() != null
                ? binding.editMaxAmount.getText().toString() : "";
        if (!minStr.isEmpty() || !maxStr.isEmpty()) {
            long min = minStr.isEmpty() ? 0L : MoneyInputFormatter.getRawValue(binding.editMinAmount);
            long max = maxStr.isEmpty() ? Long.MAX_VALUE : MoneyInputFormatter.getRawValue(binding.editMaxAmount);
            List<Transaction> amountFiltered = new ArrayList<>();
            for (Transaction t : filtered) {
                long amt = t.getAmount();
                if (amt >= min && amt <= max) amountFiltered.add(t);
            }
            filtered = amountFiltered;
        }

        // Sort
        if (selectedSort == 1) {
            Collections.sort(filtered, (a, b) -> Long.compare(b.getAmount(), a.getAmount()));
        }

        adapter.setItems(filtered);
        binding.textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getSelectedType() {
        int checked = binding.radioGroupType.getCheckedRadioButtonId();
        if (checked == R.id.radioIncome) return Transaction.TYPE_INCOME;
        if (checked == R.id.radioExpense) return Transaction.TYPE_EXPENSE;
        return "all";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
