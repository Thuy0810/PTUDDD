package com.expensemanager.app.ui.wallet;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.ActivityTransferBinding;
import com.expensemanager.app.domain.usecase.TransferService;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyValueParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferActivity extends AppCompatActivity {
    private ActivityTransferBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private final TransferService transferService = new TransferService();
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

        walletRepo.observeAll(uid).observe(this, list -> {
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
        long amount = MoneyValueParser.tryParseStrict(
                binding.editAmount.getText() != null
                        ? binding.editAmount.getText().toString() : "");
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

        int code = TransferService.validate(
                fromWallet.getId(), toWallet.getId(), amount);
        if (code == TransferService.ERR_SAME_WALLET) {
            Toast.makeText(this, "Chon hai vi khac nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromWallet.getCurrentBalance() < amount) {
            // Cảnh báo nhưng vẫn cho phép thực hiện — Firestore transaction sẽ kiểm tra lại
            // (ràng buộc 7.2: hiển thị cảnh báo trước, không chặn).
            Toast.makeText(this,
                    "Canh bao: so du vi nguon khong du (" +
                            MoneyFormat.formatLong(fromWallet.getCurrentBalance()) + ")",
                    Toast.LENGTH_LONG).show();
        }

        performTransfer(uid, amount, fromWallet, toWallet);
    }

    private void performTransfer(String uid, long amount, Wallet fromWallet, Wallet toWallet) {
        String note = binding.editNote.getText() != null
                ? binding.editNote.getText().toString().trim() : "Chuyen tien";

        binding.btnConfirm.setEnabled(false);

        transferService.performTransfer(uid,
                        fromWallet.getId(), toWallet.getId(),
                        amount, note, DateUtils.nowVietnam())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Da chuyen " + MoneyFormat.formatLong(amount),
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnConfirm.setEnabled(true);
                    Toast.makeText(this, "Loi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
