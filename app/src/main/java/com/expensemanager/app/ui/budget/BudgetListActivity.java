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
import com.expensemanager.app.util.MoneyInputFormatter;

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
                            ? getString(R.string.j1_month_line, MoneyFormat.formatLong(b.getLimitAmount()))
                            : getString(R.string.j1_category_line, MoneyFormat.formatLong(b.getLimitAmount())));
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
        input.setHint(getString(R.string.j1_month_limit_vnd_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(input);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j1_set_month_budget))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    long parsed = MoneyInputFormatter.getRawValue(input);
                    if (parsed <= 0) {
                        Toast.makeText(this, getString(R.string.error_invalid_amount),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Budget b = new Budget();
                    b.setScope(Budget.SCOPE_MONTHLY);
                    b.setMonth(DateUtils.currentMonthKey());
                    b.setLimitAmount(parsed);
                    budgetRepo.addOrUpdate(uid, b);
                    Toast.makeText(this, getString(R.string.j1_budget_saved), Toast.LENGTH_SHORT).show();
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
