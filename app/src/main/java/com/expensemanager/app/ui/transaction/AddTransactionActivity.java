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
            getSupportActionBar().setTitle("Giao dich");
        }

        editTxId = getIntent().getStringExtra(EXTRA_TX_ID);
        if (editTxId != null) {
            binding.btnDelete.setVisibility(android.view.View.VISIBLE);
            getSupportActionBar().setTitle("Sua giao dich");
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
                QuickParseUtil.ParseResult r = QuickParseUtil.parse(binding.editQuick.getText().toString());
                if (r.amount != null) binding.editAmount.setText(String.valueOf(r.amount.longValue()));
                if (r.note != null) binding.editNote.setText(r.note);
                if (r.date != null) parsedDate = new Timestamp(r.date);
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

    private void tryLoadTransaction() {
        if (editTxId != null && categoriesLoaded && walletsLoaded) {
            loadTransaction(authRepo.getUid(), editTxId);
        }
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

                    setupCategorySpinner(t.getType());
                    selectCategoryById(t.getCategoryId());
                    selectWalletById(t.getWalletId());
                });
    }

    private void save() {
        String uid = authRepo.getUid();
        if (uid == null) return;

        String amountStr = binding.editAmount.getText() != null
                ? binding.editAmount.getText().toString().trim() : "0";
        double amount;
        try { amount = Double.parseDouble(amountStr.replace(",", "")); }
        catch (Exception e) {
            Toast.makeText(this, "So tien khong hop le", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            Toast.makeText(this, "Nhap so tien > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Category cat = (Category) binding.spinnerCategory.getSelectedItem();
        Wallet wallet = (Wallet) binding.spinnerWallet.getSelectedItem();
        if (cat == null) {
            Toast.makeText(this, "Chon danh muc", Toast.LENGTH_SHORT).show();
            return;
        }
        if (wallet == null) {
            Toast.makeText(this, "Chon vi", Toast.LENGTH_SHORT).show();
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
            txRepo.updateAtomic(uid, originalTransaction, t, walletRepo,
                    originalTransaction.getWalletId(), wallet.getId());
            Toast.makeText(this, "Da luu", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Timestamp txDate = parsedDate != null ? parsedDate : Timestamp.now();
            t.setDate(txDate);
            txRepo.addAtomic(uid, t, walletRepo, wallet.getId());
            Toast.makeText(this, "Da luu", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Xoa giao dich")
                .setMessage("Ban co that su muon xoa giao dich nay?")
                .setPositiveButton("Xoa", (d, w) -> deleteTransaction())
                .setNegativeButton("Huy", null)
                .show();
    }

    private void deleteTransaction() {
        String uid = authRepo.getUid();
        if (uid == null || editTxId == null) return;

        if (originalTransaction != null) {
            String walletId = originalTransaction.getWalletId();
            txRepo.deleteAtomic(uid, originalTransaction, walletRepo, walletId);
        }

        Toast.makeText(this, "Da xoa", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
