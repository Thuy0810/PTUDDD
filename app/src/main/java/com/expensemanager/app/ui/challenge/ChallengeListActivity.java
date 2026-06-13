package com.expensemanager.app.ui.challenge;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.RecurringRule;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.RecurringRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.ActivityChallengeListBinding;
import com.expensemanager.app.ui.adapter.RecurringAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final RecurringRepository recurringRepo = new RecurringRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private RecurringAdapter adapter;
    private Map<String, Category> categoryMap = new HashMap<>();
    private List<Category> categories = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.recurring);
        }

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        RecyclerView rv = findViewById(R.id.recyclerRecurring);
        adapter = new RecurringAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        categoryRepo.observeAll(uid).observe(this, list -> {
            categories = list != null ? list : new ArrayList<>();
            categoryMap.clear();
            for (Category c : categories) categoryMap.put(c.getId(), c);
            adapter.setCategoryMap(categoryMap);
        });

        new WalletRepository().observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
        });

        recurringRepo.observeAll(uid).observe(this, list -> {
            adapter.setItems(list != null ? list : new ArrayList<>());
            View empty = findViewById(R.id.textEmpty);
            if (empty != null) empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        adapter.setOnToggle((r, enabled) -> {
            r.setEnabled(enabled);
            recurringRepo.update(uid, r);
        });

        adapter.setOnItemClick(r -> showEditDialog(uid, r));
        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddDialog(uid));
    }

    private void showAddDialog(String uid) {
        if (categories.isEmpty()) {
            Toast.makeText(this, "Chưa có danh mục", Toast.LENGTH_SHORT).show();
            return;
        }
        if (wallets.isEmpty()) {
            Toast.makeText(this, "Chưa có ví", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_recurring, null);
        EditText editNote = view.findViewById(R.id.editRecurringNote);
        EditText editAmount = view.findViewById(R.id.editRecurringAmount);
        Spinner spinnerCat = view.findViewById(R.id.spinnerRecurringCategory);
        Spinner spinnerWallet = view.findViewById(R.id.spinnerRecurringWallet);
        Spinner spinnerDay = view.findViewById(R.id.spinnerRecurringDay);

        List<String> catNames = new ArrayList<>();
        for (Category c : categories) catNames.add(c.getName());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, catNames);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCat.setAdapter(catAdapter);

        List<String> walletNames = new ArrayList<>();
        for (Wallet w : wallets) walletNames.add(w.getName());
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, walletNames);
        walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWallet.setAdapter(walletAdapter);

        String[] days = new String[31];
        for (int i = 0; i < 31; i++) days[i] = "Ngày " + (i + 1);
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, java.util.Arrays.asList(days));
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Giao dịch định kỳ mới")
                .setView(view)
                .setPositiveButton("Tạo", (d, w) -> {
                    String note = editNote.getText().toString().trim();
                    String amountStr = editAmount.getText().toString().trim().replace(",", "");
                    double amount;
                    try { amount = Double.parseDouble(amountStr); } catch (Exception e) {
                        Toast.makeText(this, "Nhập số tiền hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amount <= 0) {
                        Toast.makeText(this, "Số tiền > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int catPos = spinnerCat.getSelectedItemPosition();
                    int walletPos = spinnerWallet.getSelectedItemPosition();
                    int dayPos = spinnerDay.getSelectedItemPosition();
                    Category cat = categories.get(catPos);
                    Wallet wallet = wallets.get(walletPos);

                    RecurringRule r = new RecurringRule();
                    r.setType(Transaction.TYPE_EXPENSE);
                    r.setAmount(amount);
                    r.setCategoryId(cat.getId());
                    r.setWalletId(wallet.getId());
                    r.setNote(note);
                    r.setDayOfMonth(dayPos + 1);
                    r.setEnabled(true);
                    recurringRepo.add(uid, r);
                    Toast.makeText(this, "Đã tạo giao dịch định kỳ!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditDialog(String uid, RecurringRule r) {
        EditText input = new EditText(this);
        input.setHint("Cập nhật số tiền");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((long) r.getAmount()));

        new AlertDialog.Builder(this)
                .setTitle("Sửa: " + r.getNote())
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        r.setAmount(Double.parseDouble(input.getText().toString().replace(",", "")));
                        recurringRepo.update(uid, r);
                        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Số không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Xóa", (d, w) -> {
                    recurringRepo.delete(uid, r.getId());
                    Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
