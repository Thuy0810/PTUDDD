package com.expensemanager.app.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivityMainBinding;
import com.expensemanager.app.ui.transaction.AddTransactionActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
            if (id == R.id.nav_placeholder) return false;
            if (navController != null) {
                if (id == R.id.nav_home) navController.navigate(R.id.homeFragment);
                else if (id == R.id.nav_budget) navController.navigate(R.id.budgetFragment);
                else if (id == R.id.nav_report) navController.navigate(R.id.reportFragment);
                else if (id == R.id.nav_profile) navController.navigate(R.id.profileFragment);
            }
            return true;
        });

        binding.fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddTransactionActivity.class)));
    }
}
