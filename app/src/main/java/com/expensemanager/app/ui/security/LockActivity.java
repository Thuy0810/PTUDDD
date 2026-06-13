package com.expensemanager.app.ui.security;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.expensemanager.app.databinding.ActivityLockBinding;
import com.expensemanager.app.ui.main.MainActivity;
import com.expensemanager.app.util.PrefsHelper;

import java.util.concurrent.Executor;

public class LockActivity extends AppCompatActivity {
    private ActivityLockBinding binding;
    private final Executor executor = ContextCompat.getMainExecutor(this);
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        updateLockStatus();
        updateAttemptsDisplay();

        binding.btnUnlock.setOnClickListener(v -> attemptUnlock());
        binding.btnBiometric.setOnClickListener(v -> showBiometric());
        autoShowBiometric();
    }

    private void updateLockStatus() {
        if (PrefsHelper.isPinLockedOut(this)) {
            long remainingMs = PrefsHelper.getLockRemainingMs(this);
            long seconds = remainingMs / 1000;
            binding.textLockStatus.setText(
                    "Tài khoản tạm khóa. Thử lại sau " + (seconds / 60 + 1) + " phút.");
            binding.editPin.setEnabled(false);
            binding.btnUnlock.setEnabled(false);
            scheduleUnlockCheck(remainingMs);
        } else {
            binding.textLockStatus.setText("Nhập mã PIN để mở app");
            binding.editPin.setEnabled(true);
            binding.btnUnlock.setEnabled(true);
        }
    }

    private void scheduleUnlockCheck(long delayMs) {
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                PrefsHelper.clearPinLockout(this);
                PrefsHelper.resetPinFailCount(this);
                updateLockStatus();
                updateAttemptsDisplay();
            }
        }, delayMs + 1000);
    }

    private void updateAttemptsDisplay() {
        int remaining = PrefsHelper.getRemainingAttempts(this);
        if (remaining < 5) {
            binding.textAttempts.setVisibility(android.view.View.VISIBLE);
            binding.textAttempts.setText("Còn " + remaining + " lần thử");
        } else {
            binding.textAttempts.setVisibility(android.view.View.GONE);
        }
    }

    private void autoShowBiometric() {
        boolean biometricAvailable = BiometricManager.from(this)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
        boolean biometricEnabled = PrefsHelper.isBiometricEnabled(this)
                && !PrefsHelper.isPinLockedOut(this);

        binding.btnBiometric.setVisibility(
                biometricAvailable && biometricEnabled ? android.view.View.VISIBLE : android.view.View.GONE);

        if (biometricAvailable && biometricEnabled) {
            showBiometric();
        }
    }

    private void showBiometric() {
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        PrefsHelper.resetPinFailCount(LockActivity.this);
                        openMain();
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Mở khóa")
                .setSubtitle("Xác thực vân tay")
                .setNegativeButtonText("Hủy")
                .build();
        prompt.authenticate(info);
    }

    private void attemptUnlock() {
        if (PrefsHelper.isPinLockedOut(this)) {
            Toast.makeText(this, "Tài khoản tạm khóa", Toast.LENGTH_SHORT).show();
            return;
        }

        String pin = binding.editPin.getText() != null
                ? binding.editPin.getText().toString() : "";

        if (PrefsHelper.verifyPin(this, pin)) {
            PrefsHelper.resetPinFailCount(this);
            PrefsHelper.clearPinLockout(this);
            openMain();
        } else {
            PrefsHelper.incrementPinFailCount(this);
            updateAttemptsDisplay();

            if (PrefsHelper.getRemainingAttempts(this) <= 0) {
                PrefsHelper.setPinLockout(this);
                Toast.makeText(this, "Đã nhập sai quá nhiều lần. Khóa 5 phút.", Toast.LENGTH_LONG).show();
                updateLockStatus();
            } else {
                Toast.makeText(this, "PIN sai. Còn " + PrefsHelper.getRemainingAttempts(this) + " lần thử.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLockStatus();
        updateAttemptsDisplay();
    }
}
