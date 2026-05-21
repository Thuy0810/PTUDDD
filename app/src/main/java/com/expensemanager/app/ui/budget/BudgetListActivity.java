package com.expensemanager.app.ui.budget;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.List;

public class BudgetListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView listView = new ListView(this);
        setContentView(listView);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.budgets);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        budgetRepo.observeMonth(uid, DateUtils.currentMonthKey()).observe(this, list -> {
            List<String> lines = new ArrayList<>();
            if (list != null) {
                for (Budget b : list) {
                    lines.add(Budget.SCOPE_MONTHLY.equals(b.getScope())
                            ? "Tháng: " + MoneyFormat.format(b.getLimitAmount())
                            : "Danh mục: " + MoneyFormat.format(b.getLimitAmount()));
                }
            }
            adapter.clear();
            adapter.addAll(lines);
        });

        findViewById(android.R.id.content).setOnClickListener(v -> showAddDialog(uid));
        listView.setOnItemClickListener((p, v, pos, id) -> showAddDialog(uid));
    }

    private void showAddDialog(String uid) {
        EditText input = new EditText(this);
        input.setHint("Giới hạn chi tháng (VNĐ)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("Đặt ngân sách tháng")
                .setView(input)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        double limit = Double.parseDouble(input.getText().toString().trim());
                        Budget b = new Budget();
                        b.setScope(Budget.SCOPE_MONTHLY);
                        b.setMonth(DateUtils.currentMonthKey());
                        b.setLimitAmount(limit);
                        budgetRepo.add(uid, b);
                        Toast.makeText(this, "Đã lưu ngân sách", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
