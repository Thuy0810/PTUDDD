package com.expensemanager.app.ui.transaction;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.databinding.ActivityAddTransactionBinding;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.util.CategorySuggester;
import com.expensemanager.app.util.QuickParseUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Giao dịch");
        }

        editTxId = getIntent().getStringExtra(EXTRA_TX_ID);
        if (editTxId != null) {
            binding.btnDelete.setVisibility(android.view.View.VISIBLE);
            getSupportActionBar().setTitle("Sửa giao dịch");
        }

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        new CategoryRepository().observeAll(uid).observe(this, list -> {
            categories = list != null ? list : new ArrayList<>();
            setupCategorySpinner(Transaction.TYPE_EXPENSE);
        });
        new WalletRepository().observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
            walletMap.clear();
            for (Wallet w : wallets) {
                if (w.getId() != null) walletMap.put(w.getId(), w);
            }
            setupWalletSpinner();
            if (editTxId != null) {
                loadTransaction(uid, editTxId);
            }
        });

        binding.radioType.setOnCheckedChangeListener((g, id) -> {
            boolean income = binding.radioIncome.isChecked();
            setupCategorySpinner(income ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE);
        });

        binding.editQuick.setOnFocusChangeListener((v, has) -> {
            if (!has && binding.editQuick.getText() != null) {
                QuickParseUtil.ParseResult r = QuickParseUtil.parse(binding.editQuick.getText().toString());
                if (r.amount != null) binding.editAmount.setText(String.valueOf(r.amount.longValue()));
                if (r.note != null) binding.editNote.setText(r.note);
                String catId = CategorySuggester.suggestCategoryId(r.note != null ? r.note : "");
                if (catId != null) selectCategory(catId);
            }
        });

        binding.editNote.setOnFocusChangeListener((v, has) -> {
            if (!has && binding.editNote.getText() != null) {
                String catId = CategorySuggester.suggestCategoryId(binding.editNote.getText().toString());
                if (catId != null) selectCategory(catId);
            }
        });

        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirm());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private void setupCategorySpinner(String type) {
        List<Category> filtered = new ArrayList<>();
        for (Category c : categories) {
            if (type.equals(c.getType())) filtered.add(c);
        }
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filtered);
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

    private void loadTransaction(String uid, String txId) {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("transactions")
                .document(txId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Transaction t = doc.toObject(Transaction.class);
                    if (t == null) return;

                    originalTransaction = t;

                    if (Transaction.TYPE_INCOME.equals(t.getType())) {
                        binding.radioIncome.setChecked(true);
                    } else {
                        binding.radioExpense.setChecked(true);
                    }
                    binding.editAmount.setText(String.valueOf((long) t.getAmount()));
                    binding.editNote.setText(t.getNote() != null ? t.getNote() : "");
                    selectWalletById(t.getWalletId());
                    selectCategoryById(t.getCategoryId());

                    setupCategorySpinner(t.getType());
                    selectWalletById(t.getWalletId());
                });
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

    private void save() {
        String uid = authRepo.getUid();
        if (uid == null) return;

        String amountStr = binding.editAmount.getText() != null
                ? binding.editAmount.getText().toString().trim() : "0";
        double amount;
        try { amount = Double.parseDouble(amountStr.replace(",", "")); }
        catch (Exception e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            Toast.makeText(this, "Nhập số tiền > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Category cat = (Category) binding.spinnerCategory.getSelectedItem();
        Wallet wallet = (Wallet) binding.spinnerWallet.getSelectedItem();
        if (cat == null) {
            Toast.makeText(this, "Chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }
        if (wallet == null) {
            Toast.makeText(this, "Chọn ví", Toast.LENGTH_SHORT).show();
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
        t.setDate(Timestamp.now());

        if (editTxId != null && originalTransaction != null) {
            t.setId(editTxId);
            txRepo.update(uid, t);
            updateWalletBalanceAfterEdit(uid);
        } else if (editTxId != null) {
            t.setId(editTxId);
            txRepo.update(uid, t);
            updateWalletBalance(uid, wallet, amount, isIncome);
        } else {
            txRepo.add(uid, t);
            updateWalletBalance(uid, wallet, amount, isIncome);
        }

        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateWalletBalance(String uid, Wallet wallet, double amount, boolean isIncome) {
        double newBalance = wallet.getCurrentBalance();
        if (isIncome) {
            newBalance += amount;
        } else {
            newBalance -= amount;
        }
        wallet.setCurrentBalance(newBalance);
        walletRepo.update(uid, wallet);
    }

    private void updateWalletBalanceAfterEdit(String uid) {
        if (originalTransaction == null) return;

        String originalWalletId = originalTransaction.getWalletId();
        String newWalletId = originalTransaction.getWalletId();
        Wallet originalWallet = walletMap.get(originalWalletId);
        Wallet newWallet = (Wallet) binding.spinnerWallet.getSelectedItem();

        double originalAmount = originalTransaction.getAmount();
        boolean originalIsIncome = Transaction.TYPE_INCOME.equals(originalTransaction.getType());
        boolean newIsIncome = binding.radioIncome.isChecked();
        double newAmount = 0;
        try {
            newAmount = Double.parseDouble(binding.editAmount.getText().toString().replace(",", ""));
        } catch (Exception ignored) {}

        boolean sameWallet = originalWalletId != null && originalWalletId.equals(newWalletId);

        if (sameWallet) {
            double originalEffect = originalIsIncome ? originalAmount : -originalAmount;
            double newEffect = newIsIncome ? newAmount : -newAmount;
            double balanceChange = newEffect - originalEffect;

            if (originalWallet != null) {
                originalWallet.setCurrentBalance(originalWallet.getCurrentBalance() + balanceChange);
                walletRepo.update(uid, originalWallet);
            }
        } else {
            if (originalWallet != null) {
                double originalEffect = originalIsIncome ? originalAmount : -originalAmount;
                originalWallet.setCurrentBalance(originalWallet.getCurrentBalance() - originalEffect);
                walletRepo.update(uid, originalWallet);
            }

            if (newWallet != null) {
                double newEffect = newIsIncome ? newAmount : -newAmount;
                newWallet.setCurrentBalance(newWallet.getCurrentBalance() + newEffect);
                walletRepo.update(uid, newWallet);
            }
        }
    }

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc muốn xóa giao dịch này?")
                .setPositiveButton("Xóa", (d, w) -> deleteTransaction())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteTransaction() {
        String uid = authRepo.getUid();
        if (uid == null || editTxId == null) return;

        Wallet wallet = (Wallet) binding.spinnerWallet.getSelectedItem();

        if (originalTransaction != null) {
            String walletId = originalTransaction.getWalletId();
            Wallet w = walletMap.get(walletId);
            double amt = originalTransaction.getAmount();
            boolean isIncome = Transaction.TYPE_INCOME.equals(originalTransaction.getType());

            if (w != null) {
                double newBalance = w.getCurrentBalance();
                if (isIncome) {
                    newBalance -= amt;
                } else {
                    newBalance += amt;
                }
                w.setCurrentBalance(newBalance);
                walletRepo.update(uid, w);
            }
        } else if (wallet != null && binding.editAmount.getText() != null) {
            double amt = 0;
            try { amt = Double.parseDouble(binding.editAmount.getText().toString().replace(",", "")); } catch (Exception ignored) {}
            if (amt > 0) {
                double newBalance = wallet.getCurrentBalance();
                if (binding.radioIncome.isChecked()) {
                    newBalance -= amt;
                } else {
                    newBalance += amt;
                }
                wallet.setCurrentBalance(newBalance);
                walletRepo.update(uid, wallet);
            }
        }

        txRepo.delete(uid, editTxId);
        Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
