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
import com.expensemanager.app.ui.transaction.QuickAddActivity;
import com.expensemanager.app.util.MoneyFormat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;
import com.expensemanager.app.worker.RecurringTransactionWorker;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NavController navController;

    // Cac tab cua bottom nav tuy bien
    private final int[] navContainers = {R.id.nav_home, R.id.nav_budget, R.id.nav_report, R.id.nav_profile};
    private final int[] navIcons = {R.id.nav_home_icon, R.id.nav_budget_icon, R.id.nav_report_icon, R.id.nav_profile_icon};
    private final int[] navLabels = {R.id.nav_home_label, R.id.nav_budget_label, R.id.nav_report_label, R.id.nav_profile_label};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            navController = navHost.getNavController();
        }

        // Bottom nav tuy bien: 4 tab + FAB cam noi giua
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            if (navController != null) navController.navigate(R.id.homeFragment);
            setActiveTab(0);
        });
        findViewById(R.id.nav_budget).setOnClickListener(v -> {
            if (navController != null) navController.navigate(R.id.budgetFragment);
            setActiveTab(1);
        });
        findViewById(R.id.nav_report).setOnClickListener(v -> {
            if (navController != null) navController.navigate(R.id.reportFragment);
            setActiveTab(2);
        });
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            if (navController != null) navController.navigate(R.id.profileFragment);
            setActiveTab(3);
        });
        findViewById(R.id.nav_add).setOnClickListener(v -> showAddModeSheet());

        // Dong bo tab sang theo dich den hien tai
        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, args) -> {
                int dest = destination.getId();
                if (dest == R.id.homeFragment) setActiveTab(0);
                else if (dest == R.id.budgetFragment) setActiveTab(1);
                else if (dest == R.id.reportFragment) setActiveTab(2);
                else if (dest == R.id.profileFragment) setActiveTab(3);
            });
        }
        setActiveTab(0);

        // Lên lịch worker cho giao dịch định kỳ
        RecurringTransactionWorker.schedule(this);

        // Chạy bù khi mở app
        String uid = new AuthRepository().getUid();
        if (uid != null) {
            new com.expensemanager.app.data.repository.RecurringRepository().catchUp(uid);
        }

        checkOverdueGoals();
    }

    /** To mau tab dang chon (xanh) va cac tab con lai (xam nhat). */
    private void setActiveTab(int active) {
        int blue = ContextCompat.getColor(this, R.color.brand_blue);
        int faint = ContextCompat.getColor(this, R.color.brand_faint);
        for (int i = 0; i < navContainers.length; i++) {
            boolean on = (i == active);
            ImageView icon = findViewById(navIcons[i]);
            TextView label = findViewById(navLabels[i]);
            if (icon != null) icon.setColorFilter(on ? blue : faint);
            if (label != null) {
                label.setTextColor(on ? blue : faint);
                label.setTypeface(null, on ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }

    /** Hiển thị bottom sheet chọn cách thêm giao dịch: nhập thường hoặc nhập nhanh. */
    private void showAddModeSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_add, null);
        sheet.setContentView(view);

        view.findViewById(R.id.optionNormal).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, AddTransactionActivity.class));
        });
        view.findViewById(R.id.optionQuick).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, QuickAddActivity.class));
        });

        sheet.show();
    }

    private void checkOverdueGoals() {
        String uid = new AuthRepository().getUid();
        if (uid == null) return;

        new GoalRepository().observeOverdue(uid).observe(this, (List<SavingsGoal> overdue) -> {
            if (overdue == null || overdue.isEmpty()) return;

            StringBuilder sb = new StringBuilder();
            for (SavingsGoal g : overdue) {
                long shortfall = g.getTargetAmount() - g.getSavedAmount();
                sb.append(getString(R.string.j1_overdue_goal_line,
                        g.getTitle(), MoneyFormat.format(shortfall)))
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
