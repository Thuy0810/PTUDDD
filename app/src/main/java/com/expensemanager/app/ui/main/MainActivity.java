package com.expensemanager.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.GoalRepository;
import com.expensemanager.app.databinding.ActivityMainBinding;
import com.expensemanager.app.ui.transaction.AddTransactionActivity;
import com.expensemanager.app.util.MoneyFormat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navController = navHost.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (navController != null) {
                if (id == R.id.nav_home) navController.navigate(R.id.homeFragment);
                else if (id == R.id.nav_budget) navController.navigate(R.id.budgetFragment);
                else if (id == R.id.nav_report) navController.navigate(R.id.reportFragment);
                else if (id == R.id.nav_profile) navController.navigate(R.id.profileFragment);
                else return false;
            }
            return true;
        });

        binding.fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddTransactionActivity.class)));

        checkOverdueGoals();
    }

    private void checkOverdueGoals() {
        String uid = new AuthRepository().getUid();
        if (uid == null) return;

        new GoalRepository().observeOverdue(uid).observe(this, (List<SavingsGoal> overdue) -> {
            if (overdue == null || overdue.isEmpty()) return;

            StringBuilder sb = new StringBuilder();
            for (SavingsGoal g : overdue) {
                double shortfall = g.getTargetAmount() - g.getSavedAmount();
                sb.append("• ").append(g.getTitle())
                        .append(": thiếu ").append(MoneyFormat.format(shortfall))
                        .append("\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Cảnh báo mục tiêu quá hạn")
                    .setMessage("Bạn có " + overdue.size() + " mục tiêu đang quá hạn:\n\n" + sb)
                    .setPositiveButton("Đóng", null)
                    .show();
        });
    }
}
