package com.expensemanager.app.ui.wallet;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/** Chuyển tiền giữa các ví */
public class TransferActivity extends AppCompatActivity {
    private List<Wallet> wallets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        EditText amount = new EditText(this);
        amount.setHint("Số tiền");
        Spinner from = new Spinner(this);
        Spinner to = new Spinner(this);
        layout.addView(amount);
        layout.addView(from);
        layout.addView(to);
        setContentView(layout);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chuyển tiền");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String uid = new AuthRepository().getUid();
        if (uid == null) { finish(); return; }

        new WalletRepository().observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
            ArrayAdapter<Wallet> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, wallets);
            from.setAdapter(adapter);
            to.setAdapter(adapter);
        });
    }

    public void saveTransfer(String uid, double amt, Wallet fromW, Wallet toW) {
        Transaction t = new Transaction();
        t.setType(Transaction.TYPE_TRANSFER);
        t.setAmount(amt);
        t.setFromWalletId(fromW.getId());
        t.setToWalletId(toW.getId());
        t.setWalletId(fromW.getId());
        t.setDate(Timestamp.now());
        t.setNote("Chuyển tiền");
        new TransactionRepository().add(uid, t);
        Toast.makeText(this, "Đã chuyển", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
