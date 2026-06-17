package com.expensemanager.app.ui.goal;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.expensemanager.app.domain.usecase.GoalService;
import com.expensemanager.app.ui.adapter.GoalAdapter;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyInputFormatter;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final GoalRepository goalRepo = new GoalRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private final GoalService goalService = new GoalService();
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

        ((android.widget.TextView) findViewById(R.id.textHeaderTitle)).setText(R.string.goals);
        findViewById(R.id.btnHeaderBack).setOnClickListener(v -> finish());

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
            Toast.makeText(this, getString(R.string.error_no_wallet), Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_goal, null);
        EditText editTitle = view.findViewById(R.id.editGoalTitle);
        EditText editTarget = view.findViewById(R.id.editGoalTarget);
        EditText editDeadline = view.findViewById(R.id.editGoalDeadline);
        Spinner spinnerWallet = view.findViewById(R.id.spinnerGoalWallet);
        MoneyInputFormatter.attach(editTarget);

        List<String> walletNames = new ArrayList<>();
        for (Wallet w : wallets) walletNames.add(w.getName());
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, walletNames);
        walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWallet.setAdapter(walletAdapter);

        final Calendar[] selectedCal = {DateUtils.newCalendar()};
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
                .setTitle(getString(R.string.j3_new_goal_title))
                .setView(view)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    String title = editTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, getString(R.string.j3_enter_goal_name), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long target = MoneyInputFormatter.getRawValue(editTarget);
                    if (target <= 0) {
                        Toast.makeText(this, getString(R.string.j3_enter_valid_amount), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int walletPos = spinnerWallet.getSelectedItemPosition();
                    Wallet wallet = wallets.get(walletPos);

                    SavingsGoal g = new SavingsGoal();
                    g.setTitle(title);
                    g.setTargetAmount(target);
                    g.setSavedAmount(0L);
                    g.setWalletId(wallet.getId());
                    g.setCompleted(false);
                    g.setDeadline(new Timestamp(selectedCal[0].getTime()));
                    goalRepo.add(uid, g);
                    Toast.makeText(this, getString(R.string.j3_goal_created), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showUpdateDialog(String uid, SavingsGoal g) {
        if (wallets.isEmpty()) {
            Toast.makeText(this, getString(R.string.j3_no_wallets), Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo view mới hoàn toàn — KHÔNG tái dùng view đã inflate từ dialog_goal
        // (gắn lại view đã có cha sẽ gây crash "child already has a parent").
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 16);

        // Dòng tiến độ (chỉ đọc): đã để dành / mục tiêu
        android.widget.TextView info = new android.widget.TextView(this);
        info.setText(getString(R.string.dash_goal_progress_value,
                MoneyFormat.formatLong(g.getSavedAmount()),
                MoneyFormat.formatLong(g.getTargetAmount())));
        info.setTextColor(getResources().getColor(R.color.text_primary, null));
        info.setPadding(0, 0, 0, 8);
        container.addView(info);

        // Nhãn + spinner chọn ví nguồn
        android.widget.TextView label = new android.widget.TextView(this);
        label.setText(getString(R.string.j3_select_source_wallet));
        label.setTextSize(13);
        label.setTextColor(getResources().getColor(R.color.text_secondary, null));
        label.setPadding(0, 16, 0, 4);
        container.addView(label);

        Spinner spinnerWallet = new Spinner(this);
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
        container.addView(spinnerWallet);

        // Nhãn + ô nhập số tiền đóng góp
        android.widget.TextView label2 = new android.widget.TextView(this);
        label2.setText(getString(R.string.j3_contribute_more));
        label2.setTextSize(13);
        label2.setTextColor(getResources().getColor(R.color.text_secondary, null));
        label2.setPadding(0, 16, 0, 4);
        container.addView(label2);

        EditText editContribute = new EditText(this);
        editContribute.setHint(getString(R.string.j3_contribute_amount_hint));
        editContribute.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(editContribute);
        container.addView(editContribute);

        // Nút: sửa thẳng số đã tiết kiệm + chuyển sang mục tiêu khác.
        final android.app.AlertDialog[] parentRef = new android.app.AlertDialog[1];

        android.widget.Button btnEditSaved = new android.widget.Button(this);
        btnEditSaved.setText(getString(R.string.edit_saved_amount));
        btnEditSaved.setOnClickListener(v -> {
            if (parentRef[0] != null) parentRef[0].dismiss();
            showEditSavedDialog(uid, g);
        });
        container.addView(btnEditSaved);

        android.widget.Button btnTransfer = new android.widget.Button(this);
        btnTransfer.setText(getString(R.string.transfer_to_goal));
        btnTransfer.setOnClickListener(v -> {
            if (parentRef[0] != null) parentRef[0].dismiss();
            showGoalTransferDialog(uid, g);
        });
        container.addView(btnTransfer);

        parentRef[0] = new AlertDialog.Builder(this)
                .setTitle(g.getTitle())
                .setView(container)
                .setPositiveButton(getString(R.string.save), (d, which) -> {
                    long contribute = MoneyInputFormatter.getRawValue(editContribute);
                    if (contribute <= 0) {
                        Toast.makeText(this, getString(R.string.j3_invalid_number), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int walletPos = spinnerWallet.getSelectedItemPosition();
                    Wallet wallet = wallets.get(walletPos);

                    // Sử dụng service atomic — không tự trừ ví trong Activity.
                    goalService.contributeToGoal(uid, g.getId(), contribute, wallet.getId())
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, getString(R.string.j3_updated), Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, getString(R.string.j3_error_prefix, e.getMessage()),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .setNeutralButton(getString(R.string.j3_delete_goal), (d, which) -> {
                    goalRepo.delete(uid, g.getId());
                    Toast.makeText(this, getString(R.string.success_delete), Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /** Sửa thẳng số tiền đã tiết kiệm của mục tiêu (không đụng tới ví). */
    private void showEditSavedDialog(String uid, SavingsGoal g) {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 0);

        EditText editSaved = new EditText(this);
        editSaved.setHint(getString(R.string.edit_saved_amount_hint));
        editSaved.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(editSaved);
        editSaved.setText(MoneyInputFormatter.format(g.getSavedAmount()));
        container.addView(editSaved);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.edit_saved_amount))
                .setView(container)
                .setPositiveButton(getString(R.string.save), (d, which) -> {
                    long newSaved = MoneyInputFormatter.getRawValue(editSaved);
                    if (newSaved < 0) {
                        Toast.makeText(this, getString(R.string.j3_invalid_number), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    goalRepo.updateSavedAmount(uid, g.getId(), newSaved)
                            .addOnSuccessListener(unused -> Toast.makeText(this,
                                    getString(R.string.saved_amount_updated), Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    getString(R.string.j3_error_prefix, e.getMessage()),
                                    Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /** Chuyển số đã tiết kiệm sang mục tiêu khác (atomic, không ảnh hưởng ví). */
    private void showGoalTransferDialog(String uid, SavingsGoal fromGoal) {
        // Mục tiêu đích: tất cả mục tiêu khác chưa lưu trữ.
        List<SavingsGoal> others = new ArrayList<>();
        for (SavingsGoal g : goals) {
            if (g.getId() != null && !g.getId().equals(fromGoal.getId()) && !g.isArchived()) {
                others.add(g);
            }
        }
        if (others.isEmpty()) {
            Toast.makeText(this, getString(R.string.need_two_goals), Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 0);

        android.widget.TextView info = new android.widget.TextView(this);
        info.setText(getString(R.string.dash_goal_progress_value,
                MoneyFormat.formatLong(fromGoal.getSavedAmount()),
                MoneyFormat.formatLong(fromGoal.getTargetAmount())));
        info.setTextColor(getResources().getColor(R.color.text_primary, null));
        info.setPadding(0, 0, 0, 8);
        container.addView(info);

        android.widget.TextView lblTo = new android.widget.TextView(this);
        lblTo.setText(getString(R.string.transfer_to_goal_label));
        lblTo.setTextSize(13);
        lblTo.setTextColor(getResources().getColor(R.color.text_secondary, null));
        lblTo.setPadding(0, 8, 0, 4);
        container.addView(lblTo);

        Spinner spTo = new Spinner(this);
        List<String> names = new ArrayList<>();
        for (SavingsGoal g : others) names.add(g.getTitle());
        ArrayAdapter<String> aTo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        aTo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTo.setAdapter(aTo);
        container.addView(spTo);

        android.widget.TextView lblAmt = new android.widget.TextView(this);
        lblAmt.setText(getString(R.string.transfer_amount));
        lblAmt.setTextSize(13);
        lblAmt.setTextColor(getResources().getColor(R.color.text_secondary, null));
        lblAmt.setPadding(0, 16, 0, 4);
        container.addView(lblAmt);

        EditText editAmount = new EditText(this);
        editAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(editAmount);
        container.addView(editAmount);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.transfer_between_goals))
                .setView(container)
                .setPositiveButton(getString(R.string.transfer_money), (d, which) -> {
                    long amount = MoneyInputFormatter.getRawValue(editAmount);
                    if (amount <= 0) {
                        Toast.makeText(this, getString(R.string.j3_invalid_number), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (fromGoal.getSavedAmount() < amount) {
                        Toast.makeText(this, getString(R.string.reallocation_exceed_source),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    SavingsGoal to = others.get(spTo.getSelectedItemPosition());
                    goalRepo.transferBetweenGoals(uid, fromGoal.getId(), to.getId(), amount)
                            .addOnSuccessListener(unused -> Toast.makeText(this,
                                    getString(R.string.transfer_success, MoneyFormat.formatLong(amount)),
                                    Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    getString(R.string.j3_error_prefix, e.getMessage()),
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
