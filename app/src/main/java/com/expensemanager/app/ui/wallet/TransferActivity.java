package com.expensemanager.app.ui.wallet;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.ActivityTransferBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction as FirestoreTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferActivity extends AppCompatActivity {
    private ActivityTransferBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<Wallet> wallets = new ArrayList<>();
    private Map<String, Wallet> walletMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransferBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chuyen tien");
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
            Toast.makeText(this, "So tien khong hop le", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            Toast.makeText(this, "Nhap so tien > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Wallet fromWallet = (Wallet) binding.spinnerFrom.getSelectedItem();
        Wallet toWallet = (Wallet) binding.spinnerTo.getSelectedItem();

        if (fromWallet == null || toWallet == null) {
            Toast.makeText(this, "Chon vi nguon va vi dich", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromWallet.getId().equals(toWallet.getId())) {
            Toast.makeText(this, "Chon hai vi khac nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromWallet.getCurrentBalance() < amount) {
            Toast.makeText(this, "So du khong du", Toast.LENGTH_SHORT).show();
            return;
        }

        performTransfer(uid, amount, fromWallet, toWallet);
    }

    private void performTransfer(String uid, double amount, Wallet fromWallet, Wallet toWallet) {
        String note = binding.editNote.getText() != null
                ? binding.editNote.getText().toString().trim() : "Chuyen tien";

        binding.btnConfirm.setEnabled(false);

        db.runTransaction((FirestoreTransaction.Function<Void>) transaction -> {
            DocumentSnapshot fromSnap = transaction.get(
                    db.collection("users").document(uid).collection("wallets").document(fromWallet.getId()));
            DocumentSnapshot toSnap = transaction.get(
                    db.collection("users").document(uid).collection("wallets").document(toWallet.getId()));

            Double fromBalance = fromSnap.getDouble("currentBalance");
            Double toBalance = toSnap.getDouble("currentBalance");

            if (fromBalance == null || toBalance == null) {
                throw new FirebaseFirestoreException("Khong doc duoc so du",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            if (fromBalance < amount) {
                throw new FirebaseFirestoreException("So du khong du",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Map<String, Object> txData = new HashMap<>();
            txData.put("type", Transaction.TYPE_TRANSFER);
            txData.put("amount", amount);
            txData.put("fromWalletId", fromWallet.getId());
            txData.put("toWalletId", toWallet.getId());
            txData.put("walletId", fromWallet.getId());
            txData.put("date", FieldValue.serverTimestamp());
            txData.put("note", note);

            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document();
            transaction.set(txRef, txData);

            transaction.update(db.collection("users").document(uid).collection("wallets")
                    .document(fromWallet.getId()), "currentBalance", fromBalance - amount);
            transaction.update(db.collection("users").document(uid).collection("wallets")
                    .document(toWallet.getId()), "currentBalance", toBalance + amount);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Da chuyen " + formatAmount(amount) + " dong", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            binding.btnConfirm.setEnabled(true);
            Toast.makeText(this, "Loi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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
