package com.expensemanager.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.databinding.ActivityOnboardingBinding;
import com.expensemanager.app.util.PrefsHelper;

/**
 * Man hinh gioi thieu (Onboarding) - chi hien thi lan dau mo app.
 * Sau khi xem xong, danh dau da hoan tat va chuyen sang man Dang nhap.
 */
public class OnboardingActivity extends AppCompatActivity {
    private ActivityOnboardingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnStart.setOnClickListener(v -> finishOnboarding());
        binding.btnHaveAccount.setOnClickListener(v -> finishOnboarding());
    }

    private void finishOnboarding() {
        PrefsHelper.setOnboardingDone(this, true);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
