package com.expensemanager.app.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.ui.adapter.WalletAdapter;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyValueParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private WalletAdapter adapter;
    private List<Transaction> allTx = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private TextView textTotalBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.wallets);
        }

        textTotalBalance = findViewById(R.id.textTotalBalance);
        RecyclerView rv = findViewById(R.id.recyclerWallets);
        adapter = new WalletAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        new TransactionRepository().observeAll(uid).observe(this, txs -> {
            allTx = txs != null ? txs : new ArrayList<>();
            adapter.setBalances(allTx);
            updateTotalBalance();
        });

        walletRepo.observeAll(uid).observe(this, ws -> {
            wallets = ws != null ? ws : new ArrayList<>();
            adapter.setWallets(wallets);
            updateTotalBalance();
        });

        adapter.setOnItemClick(w -> showEditDialog(uid, w));
        rv.setOnLongClickListener(v -> {
            showAddDialog(uid);
            return true;
        });

        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            WalletPaymentDialog dialog = new WalletPaymentDialog(this, uid);
            dialog.show();
        });
    }

    private void updateTotalBalance() {
        long total = 0L;
        for (Wallet w : wallets) {
            total += w.getCurrentBalance();
        }
        textTotalBalance.setText(MoneyFormat.formatLong(total));
    }

    private void showAddDialog(String uid) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_wallet, null);
        EditText editName = view.findViewById(R.id.editWalletName);
        EditText editBalance = view.findViewById(R.id.editWalletBalance);
        Spinner spinnerType = view.findViewById(R.id.spinnerWalletType);

        String[] types = {"Tien mat", "Ngan hang", "Vi dien tu", "Tiet kiem"};
        String[] typeKeys = {"cash", "bank", "ewallet", "savings"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        view.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Nhap ten vi", Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = spinnerType.getSelectedItemPosition();
            String typeKey = typeKeys[pos];
            long balance = MoneyValueParser.tryParse(
                    editBalance.getText().toString().trim(), 0L);
            Wallet wallet = new Wallet(null, name, typeKey, balance);
            wallet.setCurrentBalance(balance);
            view.findViewById(R.id.btnCreate).setEnabled(false);
            walletRepo.add(uid, wallet)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Da them vi", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        view.findViewById(R.id.btnCreate).setEnabled(true);
                        Toast.makeText(this, "Khong the them: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
        dialog.show();
    }

    private void showEditDialog(String uid, Wallet wallet) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_wallet, null);
        EditText editName = view.findViewById(R.id.editWalletName);
        EditText editBalance = view.findViewById(R.id.editWalletBalance);
        Spinner spinnerType = view.findViewById(R.id.spinnerWalletType);

        editName.setText(wallet.getName());
        if (wallet.getCurrentBalance() > 0) {
            editBalance.setText(String.valueOf(wallet.getCurrentBalance()));
        }
        editBalance.setVisibility(View.GONE);
        view.<com.google.android.material.button.MaterialButton>findViewById(R.id.btnCreate).setText("Luu");

        String[] types = {"Tien mat", "Ngan hang", "Vi dien tu", "Tiet kiem"};
        String[] typeKeys = {"cash", "bank", "ewallet", "savings"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        for (int i = 0; i < typeKeys.length; i++) {
            if (typeKeys[i].equals(wallet.getType())) {
                spinnerType.setSelection(i);
                break;
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_wallet)
                .setView(view)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    // Nếu ví đang có giao dịch thì chỉ cho phép archive thay vì xoá (ràng buộc 6).
                    walletRepo.countTransactions(uid, wallet.getId())
                            .addOnSuccessListener(count -> {
                                if (count > 0L) {
                                    showArchiveConfirm(uid, wallet, dialog);
                                } else {
                                    showDeleteConfirm(uid, wallet);
                                }
                            })
                            .addOnFailureListener(e -> showDeleteConfirm(uid, wallet));
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        view.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Nhap ten vi", Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = spinnerType.getSelectedItemPosition();
            wallet.setName(name);
            wallet.setType(typeKeys[pos]);
            view.findViewById(R.id.btnCreate).setEnabled(false);
            walletRepo.update(uid, wallet)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Da cap nhat", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        view.findViewById(R.id.btnCreate).setEnabled(true);
                        Toast.makeText(this, "Khong the luu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    private void showArchiveConfirm(String uid, Wallet wallet, AlertDialog parent) {
        new AlertDialog.Builder(this)
                .setTitle("Luu tru vi")
                .setMessage("Vi \"" + wallet.getName()
                        + "\" dang co giao dich. Vi se duoc luu tru (khong xoa).")
                .setPositiveButton("Luu tru", (dl, wl) -> {
                    walletRepo.archive(uid, wallet.getId())
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Da luu tru vi", Toast.LENGTH_SHORT).show();
                                parent.dismiss();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Loi: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Huy", null)
                .show();
    }

    private void showDeleteConfirm(String uid, Wallet wallet) {
        new AlertDialog.Builder(this)
                .setTitle("Xoa vi")
                .setMessage("Ban co chat muon xoa vi \"" + wallet.getName() + "\"?")
                .setPositiveButton("Xoa", (dl, wl) -> {
                    walletRepo.delete(uid, wallet.getId())
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, "Da xoa vi", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Khong the xoa: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Huy", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
