package com.expensemanager.app.ui.challenge;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Challenge;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.ChallengeRepository;
import com.expensemanager.app.databinding.ActivityChallengeListBinding;
import com.expensemanager.app.ui.adapter.ChallengeAdapter;
import com.expensemanager.app.util.MoneyInputFormatter;

import java.util.ArrayList;

public class ChallengeListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final ChallengeRepository challengeRepo = new ChallengeRepository();
    private ActivityChallengeListBinding binding;
    private ChallengeAdapter adapter;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChallengeListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ((android.widget.TextView) findViewById(R.id.textHeaderTitle)).setText(R.string.c1_title);
        findViewById(R.id.btnHeaderBack).setOnClickListener(v -> finish());

        uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        adapter = new ChallengeAdapter();
        binding.recyclerChallenges.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerChallenges.setAdapter(adapter);

        challengeRepo.observeAll(uid).observe(this, list -> {
            adapter.setItems(list != null ? list : new ArrayList<>());
            binding.textEmpty.setVisibility(
                    list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        adapter.setOnItemClick(this::showEditDialog);
        adapter.setOnMarkDay(this::markDay);
        binding.fabAdd.setOnClickListener(v -> showAddDialog());
    }

    private void markDay(Challenge c) {
        if (c.getCompletedDays() < c.getTotalDays()) {
            c.setCompletedDays(c.getCompletedDays() + 1);
            challengeRepo.update(uid, c);
            boolean done = c.getCompletedDays() >= c.getTotalDays();
            Toast.makeText(this,
                    done ? R.string.c1_finished_toast : R.string.c1_day_marked,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_challenge, null);
        EditText editTitle = view.findViewById(R.id.editChallengeTitle);
        EditText editDesc = view.findViewById(R.id.editChallengeDesc);
        EditText editTarget = view.findViewById(R.id.editChallengeTarget);
        EditText editDays = view.findViewById(R.id.editChallengeDays);
        MoneyInputFormatter.attach(editTarget);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.c1_add)
                .setView(view)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String title = editTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, R.string.c1_enter_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int days = parseInt(editDays.getText().toString());
            if (days <= 0) {
                Toast.makeText(this, R.string.c1_enter_days, Toast.LENGTH_SHORT).show();
                return;
            }
            Challenge c = new Challenge();
            c.setTitle(title);
            c.setDescription(editDesc.getText().toString().trim());
            c.setTargetSavings(MoneyInputFormatter.getRawValue(editTarget));
            c.setTotalDays(days);
            c.setCompletedDays(0);
            c.setActive(true);
            challengeRepo.add(uid, c);
            Toast.makeText(this, R.string.c1_created, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void showEditDialog(Challenge c) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_challenge, null);
        EditText editTitle = view.findViewById(R.id.editChallengeTitle);
        EditText editDesc = view.findViewById(R.id.editChallengeDesc);
        EditText editTarget = view.findViewById(R.id.editChallengeTarget);
        EditText editDays = view.findViewById(R.id.editChallengeDays);
        MoneyInputFormatter.attach(editTarget);

        editTitle.setText(c.getTitle());
        editDesc.setText(c.getDescription());
        editTarget.setText(MoneyInputFormatter.format(c.getTargetSavings()));
        editDays.setText(String.valueOf(c.getTotalDays()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(c.getTitle())
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNeutralButton(R.string.delete, (d, w) -> {
                    challengeRepo.delete(uid, c.getId());
                    Toast.makeText(this, R.string.success_delete, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String title = editTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, R.string.c1_enter_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int days = parseInt(editDays.getText().toString());
            if (days <= 0) {
                Toast.makeText(this, R.string.c1_enter_days, Toast.LENGTH_SHORT).show();
                return;
            }
            c.setTitle(title);
            c.setDescription(editDesc.getText().toString().trim());
            c.setTargetSavings(MoneyInputFormatter.getRawValue(editTarget));
            c.setTotalDays(days);
            if (c.getCompletedDays() > days) c.setCompletedDays(days);
            challengeRepo.update(uid, c);
            Toast.makeText(this, R.string.j3_updated, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
