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
import com.expensemanager.app.util.MoneyInputFormatter;

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
        MoneyInputFormatter.attach(editBalance);

        String[] types = getResources().getStringArray(R.array.wallet_type_labels);
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
                Toast.makeText(this, getString(R.string.j2_enter_wallet_name), Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = spinnerType.getSelectedItemPosition();
            String typeKey = typeKeys[pos];
            long balance = MoneyInputFormatter.getRawValue(editBalance);
            Wallet wallet = new Wallet(null, name, typeKey, balance);
            wallet.setCurrentBalance(balance);
            view.findViewById(R.id.btnCreate).setEnabled(false);
            walletRepo.add(uid, wallet)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, getString(R.string.j2_wallet_added), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        view.findViewById(R.id.btnCreate).setEnabled(true);
                        Toast.makeText(this, getString(R.string.j2_cannot_add, e.getMessage()), Toast.LENGTH_LONG).show();
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
        MoneyInputFormatter.attach(editBalance);

        editName.setText(wallet.getName());
        if (wallet.getCurrentBalance() > 0) {
            editBalance.setText(MoneyInputFormatter.format(wallet.getCurrentBalance()));
        }
        editBalance.setVisibility(View.GONE);
        view.<com.google.android.material.button.MaterialButton>findViewById(R.id.btnCreate).setText(getString(R.string.save));

        String[] types = getResources().getStringArray(R.array.wallet_type_labels);
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

        final AlertDialog[] dialogRef = new AlertDialog[1];
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_wallet)
                .setView(view)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    // Nếu ví đang có giao dịch thì chỉ cho phép archive thay vì xoá (ràng buộc 6).
                    walletRepo.countTransactions(uid, wallet.getId())
                            .addOnSuccessListener(count -> {
                                if (count > 0L) {
                                    showArchiveConfirm(uid, wallet, dialogRef[0]);
                                } else {
                                    showDeleteConfirm(uid, wallet);
                                }
                            })
                            .addOnFailureListener(e -> showDeleteConfirm(uid, wallet));
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialogRef[0] = dialog;

        view.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.j2_enter_wallet_name), Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = spinnerType.getSelectedItemPosition();
            wallet.setName(name);
            wallet.setType(typeKeys[pos]);
            view.findViewById(R.id.btnCreate).setEnabled(false);
            walletRepo.update(uid, wallet)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, getString(R.string.j2_wallet_updated), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        view.findViewById(R.id.btnCreate).setEnabled(true);
                        Toast.makeText(this, getString(R.string.j2_cannot_save, e.getMessage()), Toast.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    private void showArchiveConfirm(String uid, Wallet wallet, AlertDialog parent) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j2_archive_wallet_title))
                .setMessage(getString(R.string.j2_archive_wallet_message, wallet.getName()))
                .setPositiveButton(getString(R.string.j2_archive), (dl, wl) -> {
                    walletRepo.archive(uid, wallet.getId())
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, getString(R.string.j2_wallet_archived), Toast.LENGTH_SHORT).show();
                                parent.dismiss();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            getString(R.string.j2_error_generic, e.getMessage()),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDeleteConfirm(String uid, Wallet wallet) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j2_delete_wallet_title))
                .setMessage(getString(R.string.j2_delete_wallet_message, wallet.getName()))
                .setPositiveButton(getString(R.string.delete), (dl, wl) -> {
                    walletRepo.delete(uid, wallet.getId())
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, getString(R.string.j2_wallet_deleted), Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            getString(R.string.j2_cannot_delete, e.getMessage()),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
