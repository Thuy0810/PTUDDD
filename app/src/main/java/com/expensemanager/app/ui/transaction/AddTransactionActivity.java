package com.expensemanager.app.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivityAddTransactionBinding;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.util.CategorySuggester;
import com.expensemanager.app.util.MoneyInputFormatter;
import com.expensemanager.app.util.QuickParseUtil;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {
    public static final String EXTRA_TX_ID = "tx_id";

    private ActivityAddTransactionBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private List<Category> categories = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private String editTxId;
    private Wallet selectedWallet;
    private Transaction originalTransaction;
    private Map<String, Wallet> walletMap = new HashMap<>();
    private boolean categoriesLoaded = false;
    private boolean walletsLoaded = false;
    private Timestamp parsedDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.j2_transaction_title));
        }
        MoneyInputFormatter.attach(binding.editAmount);

        editTxId = getIntent().getStringExtra(EXTRA_TX_ID);
        if (editTxId != null) {
            binding.btnDelete.setVisibility(android.view.View.VISIBLE);
            getSupportActionBar().setTitle(getString(R.string.edit_transaction));
        }

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        new CategoryRepository().observeAll(uid).observe(this, list -> {
            categories = list != null ? list : new ArrayList<>();
            categoriesLoaded = true;
            setupCategorySpinner(Transaction.TYPE_EXPENSE);
            tryLoadTransaction();
        });
        new WalletRepository().observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
            walletMap.clear();
            for (Wallet w : wallets) {
                if (w.getId() != null) walletMap.put(w.getId(), w);
            }
            walletsLoaded = true;
            setupWalletSpinner();
            tryLoadTransaction();
        });

        binding.radioType.setOnCheckedChangeListener((g, id) -> {
            boolean income = binding.radioIncome.isChecked();
            setupCategorySpinner(income ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE);
        });

        binding.editQuick.setOnFocusChangeListener((v, has) -> {
            if (!has && binding.editQuick.getText() != null) {
                applyQuickParse();
            }
        });

        binding.editNote.setOnFocusChangeListener((v, has) -> {
            if (!has && binding.editNote.getText() != null) {
                String catId = CategorySuggester.suggestCategoryId(binding.editNote.getText().toString());
                if (catId != null) selectCategory(catId);
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            applyQuickParse();
            save();
        });
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirm());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private void tryLoadTransaction() {
        if (editTxId != null && categoriesLoaded && walletsLoaded) {
            loadTransaction(authRepo.getUid(), editTxId);
        }
    }

    private void applyQuickParse() {
        String text = binding.editQuick.getText() != null
                ? binding.editQuick.getText().toString() : "";
        if (text.trim().isEmpty()) return;

        QuickParseUtil.ParseResult r = QuickParseUtil.parse(text);
        if (r.amount != null) {
            binding.editAmount.setText(MoneyInputFormatter.format(r.amount.longValue()));
        }
        if (r.note != null && !r.note.isEmpty()) {
            binding.editNote.setText(r.note);
        }
        if (r.date != null) {
            java.util.Date now = new java.util.Date();
            if (!r.date.after(now)) {
                parsedDate = new Timestamp(r.date);
            }
        }
        String catId = CategorySuggester.suggestCategoryId(
                r.note != null ? r.note : "");
        if (catId != null) selectCategory(catId);
    }

    private void setupCategorySpinner(String type) {
        // Map id -> tên để dựng nhãn "Cha › Con".
        final Map<String, String> nameById = new HashMap<>();
        for (Category c : categories) {
            if (c.getId() != null) nameById.put(c.getId(), c.getName());
        }

        // Sắp xếp: mỗi danh mục cha kèm ngay sau là các danh mục con của nó.
        List<Category> ordered = new ArrayList<>();
        for (Category c : categories) {
            if (!type.equals(c.getType()) || c.isSubcategory()) continue;
            ordered.add(c);
            for (Category child : categories) {
                if (type.equals(child.getType()) && child.isSubcategory()
                        && c.getId() != null && c.getId().equals(child.getParentId())) {
                    ordered.add(child);
                }
            }
        }
        // Danh mục con mồ côi (cha bị xoá / khác loại) — vẫn cho chọn.
        for (Category c : categories) {
            if (type.equals(c.getType()) && c.isSubcategory() && !ordered.contains(c)) {
                ordered.add(c);
            }
        }

        ArrayAdapter<Category> adapter = new ArrayAdapter<Category>(this,
                android.R.layout.simple_spinner_item, ordered) {
            @NonNull
            @Override
            public android.view.View getView(int position, android.view.View convertView,
                                             @NonNull android.view.ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                setLabel(v, getItem(position));
                return v;
            }

            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView,
                                                     android.view.ViewGroup parent) {
                android.view.View v = super.getDropDownView(position, convertView, parent);
                setLabel(v, getItem(position));
                return v;
            }

            private void setLabel(android.view.View v, Category c) {
                if (!(v instanceof android.widget.TextView) || c == null) return;
                String label = c.getName();
                if (c.isSubcategory()) {
                    String p = nameById.get(c.getParentId());
                    if (p != null && !p.isEmpty()) label = p + " › " + c.getName();
                }
                ((android.widget.TextView) v).setText(label);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(adapter);
    }

    private void setupWalletSpinner() {
        ArrayAdapter<Wallet> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, wallets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerWallet.setAdapter(adapter);
    }

    private void selectCategory(String catId) {
        ArrayAdapter adapter = (ArrayAdapter) binding.spinnerCategory.getAdapter();
        if (adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            Category c = (Category) adapter.getItem(i);
            if (c != null && catId.equals(c.getId())) {
                binding.spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void selectWalletById(String walletId) {
        ArrayAdapter adapter = (ArrayAdapter) binding.spinnerWallet.getAdapter();
        if (adapter == null || walletId == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            Wallet w = (Wallet) adapter.getItem(i);
            if (w != null && walletId.equals(w.getId())) {
                binding.spinnerWallet.setSelection(i);
                break;
            }
        }
    }

    private void selectCategoryById(String catId) {
        if (catId == null) return;
        ArrayAdapter adapter = (ArrayAdapter) binding.spinnerCategory.getAdapter();
        if (adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            Category c = (Category) adapter.getItem(i);
            if (c != null && catId.equals(c.getId())) {
                binding.spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void loadTransaction(String uid, String txId) {
        txRepo.getTransactionById(uid, txId)
                .addOnSuccessListener(t -> {
                    if (t == null) return;

                    originalTransaction = t;

                    if (Transaction.TYPE_INCOME.equals(t.getType())) {
                        binding.radioIncome.setChecked(true);
                    } else {
                        binding.radioExpense.setChecked(true);
                    }
                    binding.editAmount.setText(MoneyInputFormatter.format(t.getAmount()));
                    binding.editNote.setText(t.getNote() != null ? t.getNote() : "");

                    setupCategorySpinner(t.getType());
                    selectCategoryById(t.getCategoryId());
                    selectWalletById(t.getWalletId());
                });
    }

    private void save() {
        String uid = authRepo.getUid();
        if (uid == null) return;

        long amount = MoneyInputFormatter.getRawValue(binding.editAmount);
        if (amount <= 0) {
            Toast.makeText(this, getString(R.string.j2_enter_amount_gt_zero), Toast.LENGTH_SHORT).show();
            return;
        }

        Category cat = (Category) binding.spinnerCategory.getSelectedItem();
        Wallet wallet = (Wallet) binding.spinnerWallet.getSelectedItem();
        if (cat == null) {
            Toast.makeText(this, getString(R.string.error_select_category), Toast.LENGTH_SHORT).show();
            return;
        }
        if (wallet == null) {
            Toast.makeText(this, getString(R.string.error_select_wallet), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isIncome = binding.radioIncome.isChecked();
        String type = isIncome ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;

        Transaction t = new Transaction();
        t.setType(type);
        t.setAmount(amount);
        t.setCategoryId(cat.getId());
        t.setWalletId(wallet.getId());
        t.setNote(binding.editNote.getText() != null ? binding.editNote.getText().toString() : "");

        if (editTxId != null && originalTransaction != null) {
            t.setId(editTxId);
            t.setDate(originalTransaction.getDate());
            performUpdate(uid, originalTransaction, t, wallet);
        } else {
            Timestamp txDate = parsedDate != null ? parsedDate : Timestamp.now();
            t.setDate(txDate);
            performAdd(uid, t, wallet);
        }
    }

    private void performAdd(String uid, Transaction t, Wallet wallet) {
        binding.btnSave.setEnabled(false);
        txRepo.addAtomic(uid, t, wallet.getId())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, getString(R.string.success_saved), Toast.LENGTH_SHORT).show();
                    // Vừa thêm khoản THU mới → gợi ý "giao việc cho tiền".
                    if (Transaction.TYPE_INCOME.equals(t.getType())) {
                        promptAssignMoney();
                    } else {
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, getString(R.string.j2_cannot_save, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    /** Sau khi thêm khoản thu, gợi ý phân bổ ("giao việc") cho số tiền mới. */
    private void promptAssignMoney() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.assign_money_title))
                .setMessage(getString(R.string.assign_money_prompt))
                .setPositiveButton(getString(R.string.assign_now), (d, w) -> {
                    Intent i = new Intent(this,
                            com.expensemanager.app.ui.budget.BudgetAllocationActivity.class);
                    i.putExtra(
                            com.expensemanager.app.ui.budget.BudgetAllocationActivity.EXTRA_MONTH_KEY,
                            com.expensemanager.app.util.DateUtils.currentMonthKey());
                    startActivity(i);
                    finish();
                })
                .setNegativeButton(getString(R.string.later), (d, w) -> finish())
                .setOnCancelListener(d -> finish())
                .show();
    }

    private void performUpdate(String uid, Transaction original, Transaction updated, Wallet wallet) {
        binding.btnSave.setEnabled(false);
        txRepo.updateAtomic(uid, original, updated,
                original.getWalletId(), wallet.getId())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, getString(R.string.success_saved), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, getString(R.string.j2_cannot_save, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j2_delete_transaction_title))
                .setMessage(getString(R.string.j2_delete_transaction_message))
                .setPositiveButton(getString(R.string.delete), (d, w) -> deleteTransaction())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deleteTransaction() {
        String uid = authRepo.getUid();
        if (uid == null || editTxId == null || originalTransaction == null) return;

        binding.btnDelete.setEnabled(false);
        txRepo.deleteAtomic(uid, originalTransaction, originalTransaction.getWalletId())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, getString(R.string.success_delete), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnDelete.setEnabled(true);
                    Toast.makeText(this, getString(R.string.j2_cannot_delete, e.getMessage()), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
