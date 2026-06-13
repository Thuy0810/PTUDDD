package com.expensemanager.app.ui.wallet;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.ActivityTransferBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferActivity extends AppCompatActivity {
    private ActivityTransferBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private List<Wallet> wallets = new ArrayList<>();
    private Map<String, Wallet> walletMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransferBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chuyển tiền");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        new WalletRepository().observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
            walletMap.clear();
            for (Wallet w : wallets) {
                if (w.getId() != null) walletMap.put(w.getId(), w);
            }
            ArrayAdapter<Wallet> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, wallets);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerFrom.setAdapter(adapter);
            binding.spinnerTo.setAdapter(adapter);
        });

        binding.btnConfirm.setOnClickListener(v -> confirmTransfer(uid));
    }

    private void confirmTransfer(String uid) {
        String amountStr = binding.editAmount.getText() != null
                ? binding.editAmount.getText().toString().trim() : "0";
        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(",", ""));
        } catch (Exception e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            Toast.makeText(this, "Nhập số tiền > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Wallet fromWallet = (Wallet) binding.spinnerFrom.getSelectedItem();
        Wallet toWallet = (Wallet) binding.spinnerTo.getSelectedItem();

        if (fromWallet == null || toWallet == null) {
            Toast.makeText(this, "Chọn ví nguồn và ví đích", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromWallet.getId().equals(toWallet.getId())) {
            Toast.makeText(this, "Chọn hai ví khác nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromWallet.getCurrentBalance() < amount) {
            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        performTransfer(uid, amount, fromWallet, toWallet);
    }

    private void performTransfer(String uid, double amount, Wallet fromWallet, Wallet toWallet) {
        String note = binding.editNote.getText() != null
                ? binding.editNote.getText().toString().trim() : "Chuyển tiền";

        Transaction t = new Transaction();
        t.setType(Transaction.TYPE_TRANSFER);
        t.setAmount(amount);
        t.setFromWalletId(fromWallet.getId());
        t.setToWalletId(toWallet.getId());
        t.setWalletId(fromWallet.getId());
        t.setDate(Timestamp.now());
        t.setNote(note);

        txRepo.add(uid, t);

        fromWallet.setCurrentBalance(fromWallet.getCurrentBalance() - amount);
        walletRepo.update(uid, fromWallet);

        toWallet.setCurrentBalance(toWallet.getCurrentBalance() + amount);
        walletRepo.update(uid, toWallet);

        Toast.makeText(this, "Đã chuyển " + formatAmount(amount) + " đ", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String formatAmount(double amount) {
        if (amount == (long) amount) {
            return String.format("%d", (long) amount);
        } else {
            return String.format("%.2f", amount);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
