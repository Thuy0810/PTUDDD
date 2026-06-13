package com.expensemanager.app.ui.goal;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.GoalRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.ActivityGoalListBinding;
import com.expensemanager.app.ui.adapter.GoalAdapter;
import com.expensemanager.app.util.DateUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final GoalRepository goalRepo = new GoalRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private GoalAdapter adapter;
    private List<SavingsGoal> goals = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private Map<String, Wallet> walletMap = new HashMap<>();
    private ActivityGoalListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoalListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.goals);
        }

        RecyclerView rv = findViewById(R.id.recyclerGoals);
        adapter = new GoalAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        walletRepo.observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
            walletMap.clear();
            for (Wallet w : wallets) {
                if (w.getId() != null) walletMap.put(w.getId(), w);
            }
            adapter.setWalletMap(walletMap);
        });

        goalRepo.observeAll(uid).observe(this, list -> {
            goals = list != null ? list : new ArrayList<>();
            adapter.setItems(goals);
            binding.textEmpty.setVisibility(goals.isEmpty() ? View.VISIBLE : View.GONE);
        });

        adapter.setOnItemClick(g -> showUpdateDialog(uid, g));
        binding.fabAdd.setOnClickListener(v -> showAddDialog(uid));
    }

    private void showAddDialog(String uid) {
        if (wallets.isEmpty()) {
            Toast.makeText(this, "Chưa có ví. Vui lòng tạo ví trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_goal, null);
        EditText editTitle = view.findViewById(R.id.editGoalTitle);
        EditText editTarget = view.findViewById(R.id.editGoalTarget);
        EditText editDeadline = view.findViewById(R.id.editGoalDeadline);
        Spinner spinnerWallet = view.findViewById(R.id.spinnerGoalWallet);

        List<String> walletNames = new ArrayList<>();
        for (Wallet w : wallets) walletNames.add(w.getName());
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, walletNames);
        walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWallet.setAdapter(walletAdapter);

        final Calendar[] selectedCal = {Calendar.getInstance()};
        selectedCal[0].add(Calendar.MONTH, 1);
        editDeadline.setText(DateUtils.formatDisplay(selectedCal[0].getTime()));

        editDeadline.setOnClickListener(v -> {
            new DatePickerDialog(this, (dp, year, month, day) -> {
                selectedCal[0].set(year, month, day, 0, 0, 0);
                editDeadline.setText(DateUtils.formatDisplay(selectedCal[0].getTime()));
            }, selectedCal[0].get(Calendar.YEAR), selectedCal[0].get(Calendar.MONTH),
               selectedCal[0].get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Mục tiêu tiết kiệm mới")
                .setView(view)
                .setPositiveButton("Tạo", (d, w) -> {
                    String title = editTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Nhập tên mục tiêu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double target = Double.parseDouble(editTarget.getText().toString()
                                .trim().replace(",", ""));
                        if (target <= 0) throw new Exception();
                        int walletPos = spinnerWallet.getSelectedItemPosition();
                        Wallet wallet = wallets.get(walletPos);

                        SavingsGoal g = new SavingsGoal();
                        g.setTitle(title);
                        g.setTargetAmount(target);
                        g.setSavedAmount(0);
                        g.setWalletId(wallet.getId());
                        g.setCompleted(false);
                        g.setDeadline(new Timestamp(selectedCal[0].getTime()));
                        goalRepo.add(uid, g);
                        Toast.makeText(this, "Đã tạo mục tiêu!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Nhập số tiền hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showUpdateDialog(String uid, SavingsGoal g) {
        if (wallets.isEmpty()) {
            Toast.makeText(this, "Không có ví nào", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_goal, null);
        EditText editTitle = view.findViewById(R.id.editGoalTitle);
        EditText editTarget = view.findViewById(R.id.editGoalTarget);
        Spinner spinnerWallet = view.findViewById(R.id.spinnerGoalWallet);
        view.findViewById(R.id.editGoalDeadline).setEnabled(false);

        editTitle.setText(g.getTitle());
        editTitle.setEnabled(false);
        editTarget.setText(String.valueOf((long) g.getTargetAmount()));
        editTarget.setEnabled(false);

        List<String> walletNames = new ArrayList<>();
        for (Wallet w : wallets) walletNames.add(w.getName());
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, walletNames);
        walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWallet.setAdapter(walletAdapter);

        if (g.getWalletId() != null) {
            for (int i = 0; i < wallets.size(); i++) {
                if (g.getWalletId().equals(wallets.get(i).getId())) {
                    spinnerWallet.setSelection(i);
                    break;
                }
            }
        }

        EditText editContribute = new EditText(this);
        editContribute.setHint("Số tiền đóng góp thêm");
        editContribute.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 16);
        container.addView(editTitle);
        container.addView(editTarget);

        android.widget.TextView label = new android.widget.TextView(this);
        label.setText("Chọn ví nguồn:");
        label.setTextSize(13);
        label.setTextColor(getResources().getColor(R.color.text_secondary, null));
        label.setPadding(0, 16, 0, 4);
        container.addView(label);
        container.addView(spinnerWallet);

        android.widget.TextView label2 = new android.widget.TextView(this);
        label2.setText("Đóng góp thêm:");
        label2.setTextSize(13);
        label2.setTextColor(getResources().getColor(R.color.text_secondary, null));
        label2.setPadding(0, 16, 0, 4);
        container.addView(label2);
        container.addView(editContribute);

        new AlertDialog.Builder(this)
                .setTitle(g.getTitle())
                .setView(container)
                .setPositiveButton("Lưu", (d, which) -> {
                    String contribStr = editContribute.getText().toString().trim().replace(",", "");
                    if (contribStr.isEmpty()) {
                        Toast.makeText(this, "Nhập số tiền", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double contribute = Double.parseDouble(contribStr);
                        if (contribute <= 0) throw new Exception();
                        int walletPos = spinnerWallet.getSelectedItemPosition();
                        Wallet wallet = wallets.get(walletPos);

                        deductFromWallet(uid, wallet.getId(), contribute);
                        goalRepo.addContribution(uid, g.getId(), contribute, wallet.getId());

                        double newSaved = g.getSavedAmount() + contribute;
                        g.setSavedAmount(newSaved);
                        g.setCompleted(newSaved >= g.getTargetAmount());
                        goalRepo.update(uid, g);

                        Toast.makeText(this, "Đã cập nhật!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Số không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Xóa mục tiêu", (d, which) -> {
                    goalRepo.delete(uid, g.getId());
                    Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void deductFromWallet(String uid, String walletId, double amount) {
        if (walletId == null || amount <= 0) return;
        Wallet wallet = walletMap.get(walletId);
        if (wallet == null) return;

        double newBalance = wallet.getCurrentBalance() - amount;
        wallet.setCurrentBalance(newBalance);
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentBalance", newBalance);
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("wallets").document(walletId)
                .update(updates);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
