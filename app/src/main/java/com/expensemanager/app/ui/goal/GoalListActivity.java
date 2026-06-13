package com.expensemanager.app.ui.goal;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.GoalRepository;
import com.expensemanager.app.databinding.ActivityGoalListBinding;
import com.expensemanager.app.ui.adapter.GoalAdapter;
import com.expensemanager.app.util.DateUtils;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GoalListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final GoalRepository goalRepo = new GoalRepository();
    private GoalAdapter adapter;
    private List<SavingsGoal> goals = new ArrayList<>();
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

        goalRepo.observeAll(uid).observe(this, list -> {
            goals = list != null ? list : new ArrayList<>();
            adapter.setItems(goals);
            binding.textEmpty.setVisibility(goals.isEmpty() ? View.VISIBLE : View.GONE);
        });

        adapter.setOnItemClick(g -> showUpdateDialog(uid, g));
        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddDialog(uid));
    }

    private void showAddDialog(String uid) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_goal, null);
        EditText editTitle = view.findViewById(R.id.editGoalTitle);
        EditText editTarget = view.findViewById(R.id.editGoalTarget);
        EditText editDeadline = view.findViewById(R.id.editGoalDeadline);

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
                        double target = Double.parseDouble(editTarget.getText().toString().trim().replace(",", ""));
                        if (target <= 0) throw new Exception();
                        SavingsGoal g = new SavingsGoal();
                        g.setTitle(title);
                        g.setTargetAmount(target);
                        g.setSavedAmount(0);
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
        EditText input = new EditText(this);
        input.setHint("Cập nhật số tiền đã tiết kiệm");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((long) g.getSavedAmount()));

        new AlertDialog.Builder(this)
                .setTitle(g.getTitle())
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        double saved = Double.parseDouble(input.getText().toString().trim().replace(",", ""));
                        g.setSavedAmount(saved);
                        g.setCompleted(saved >= g.getTargetAmount());
                        goalRepo.update(uid, g);
                        Toast.makeText(this, "Đã cập nhật!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Số không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Xóa", (d, w) -> {
                    goalRepo.delete(uid, g.getId());
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
