package com.expensemanager.app.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.GoalRepository;
import com.expensemanager.app.ui.transaction.AddTransactionActivity;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.worker.RecurringTransactionWorker;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navController = navHost.getNavController();
        }

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav
                = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    if (navController != null) navController.navigate(R.id.homeFragment);
                    return true;
                } else if (id == R.id.nav_budget) {
                    if (navController != null) navController.navigate(R.id.budgetFragment);
                    return true;
                } else if (id == R.id.nav_add) {
                    startActivity(new Intent(this, AddTransactionActivity.class));
                    return true;
                } else if (id == R.id.nav_report) {
                    if (navController != null) navController.navigate(R.id.reportFragment);
                    return true;
                } else if (id == R.id.nav_profile) {
                    if (navController != null) navController.navigate(R.id.profileFragment);
                    return true;
                }
                return false;
            });
        }

        // Lên lịch worker cho giao dịch định kỳ
        RecurringTransactionWorker.schedule(this);

        // Chạy bù khi mở app
        String uid = new AuthRepository().getUid();
        if (uid != null) {
            new com.expensemanager.app.data.repository.RecurringRepository().catchUp(uid);
        }

        checkOverdueGoals();
    }

    private void checkOverdueGoals() {
        String uid = new AuthRepository().getUid();
        if (uid == null) return;

        new GoalRepository().observeOverdue(uid).observe(this, (List<SavingsGoal> overdue) -> {
            if (overdue == null || overdue.isEmpty()) return;

            StringBuilder sb = new StringBuilder();
            for (SavingsGoal g : overdue) {
                long shortfall = g.getTargetAmount() - g.getSavedAmount();
                sb.append("• ").append(g.getTitle())
                        .append(": thiếu ").append(MoneyFormat.format(shortfall))
                        .append("\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.alert_overdue_goals_title)
                    .setMessage(getString(R.string.alert_overdue_goals_body,
                            overdue.size(), sb))
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        });
    }
}
