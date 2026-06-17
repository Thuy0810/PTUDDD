package com.expensemanager.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.databinding.ActivitySplashBinding;
import com.expensemanager.app.ui.main.MainActivity;
import com.expensemanager.app.ui.security.LockActivity;
import com.expensemanager.app.util.PrefsHelper;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, 1200);
    }

    private void routeNext() {
        if (PrefsHelper.isPendingLogout(this)) {
            PrefsHelper.clearPendingLogout(this);
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            AuthRepository auth = new AuthRepository();
            if (auth.getCurrentUser() == null) {
                if (!PrefsHelper.isOnboardingDone(this)) {
                    startActivity(new Intent(this, OnboardingActivity.class));
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                }
            } else if (PrefsHelper.isPinEnabled(this)) {
                startActivity(new Intent(this, LockActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
