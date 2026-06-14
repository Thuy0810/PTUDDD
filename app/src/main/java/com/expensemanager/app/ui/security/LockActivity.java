package com.expensemanager.app.ui.security;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivityLockBinding;
import com.expensemanager.app.ui.main.MainActivity;
import com.expensemanager.app.util.PrefsHelper;

import java.util.concurrent.Executor;

public class LockActivity extends AppCompatActivity {
    private static final String TAG = "LockActivity";

    private ActivityLockBinding binding;
    private Executor executor;
    private BiometricPrompt currentPrompt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingUnlockCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        executor = ContextCompat.getMainExecutor(this);

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
            long minutes = (remainingMs / 1000) / 60 + 1;
            binding.textLockStatus.setText(
                    getString(R.string.pin_locked_message, minutes));
            binding.editPin.setEnabled(false);
            binding.btnUnlock.setEnabled(false);
            binding.btnBiometric.setVisibility(View.GONE);
            scheduleUnlockCheck(remainingMs);
        } else {
            binding.textLockStatus.setText(R.string.pin_enter_prompt);
            binding.editPin.setEnabled(true);
            binding.btnUnlock.setEnabled(true);
        }
    }

    private void cancelScheduledUnlockCheck() {
        if (pendingUnlockCheck != null) {
            handler.removeCallbacks(pendingUnlockCheck);
            pendingUnlockCheck = null;
        }
    }

    private void scheduleUnlockCheck(long delayMs) {
        cancelScheduledUnlockCheck();
        pendingUnlockCheck = () -> {
            if (!isFinishing() && !isDestroyed) {
                PrefsHelper.clearPinLockout(LockActivity.this);
                PrefsHelper.resetPinFailCount(LockActivity.this);
                updateLockStatus();
                updateAttemptsDisplay();
            }
            pendingUnlockCheck = null;
        };
        handler.postDelayed(pendingUnlockCheck, delayMs + 1000);
    }

    private void updateAttemptsDisplay() {
        int remaining = PrefsHelper.getRemainingAttempts(this);
        if (remaining < 5) {
            binding.textAttempts.setVisibility(View.VISIBLE);
            binding.textAttempts.setText(getString(R.string.attempts_remaining, remaining));
        } else {
            binding.textAttempts.setVisibility(View.GONE);
        }
    }

    private void autoShowBiometric() {
        if (isFinishing() || isDestroyed) return;

        boolean biometricAvailable;
        try {
            biometricAvailable = BiometricManager.from(this)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            Log.w(TAG, "BiometricManager check failed", e);
            biometricAvailable = false;
        }

        boolean biometricEnabled = PrefsHelper.isBiometricEnabled(this)
                && !PrefsHelper.isPinLockedOut(this);

        binding.btnBiometric.setVisibility(
                biometricAvailable && biometricEnabled ? View.VISIBLE : View.GONE);

        if (biometricAvailable && biometricEnabled) {
            // Post to ensure view is laid out
            binding.btnBiometric.postDelayed(this::showBiometric, 300);
        }
    }

    private void showBiometric() {
        if (isFinishing() || isDestroyed) return;
        if (currentPrompt != null) {
            // Prevent multiple prompts
            return;
        }

        try {
            BiometricPrompt.AuthenticationCallback callback =
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                @NonNull BiometricPrompt.AuthenticationResult result) {
                            currentPrompt = null;
                            PrefsHelper.resetPinFailCount(LockActivity.this);
                            openMain();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode,
                                @NonNull CharSequence errString) {
                            currentPrompt = null;
                            // Không crash khi user hủy hoặc biometric không khả dụng
                            Log.d(TAG, "Biometric error: " + errorCode + " — " + errString);
                            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                    || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                    || errorCode == BiometricPrompt.ERROR_CANCELED) {
                                // User hủy — quay về nhập PIN
                                return;
                            }
                            if (errorCode == BiometricPrompt.ERROR_LOCKOUT
                                    || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                                Toast.makeText(LockActivity.this,
                                        R.string.biometric_locked, Toast.LENGTH_SHORT).show();
                                binding.btnBiometric.setVisibility(View.GONE);
                                binding.editPin.requestFocus();
                            }
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            Log.d(TAG, "Biometric authentication failed");
                        }
                    };

            currentPrompt = new BiometricPrompt(this, executor, callback);

            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.biometric_title))
                    .setSubtitle(getString(R.string.biometric_subtitle))
                    .setNegativeButtonText(getString(R.string.cancel))
                    .build();

            currentPrompt.authenticate(info);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show biometric prompt", e);
            currentPrompt = null;
            binding.btnBiometric.setVisibility(View.GONE);
        }
    }

    private void attemptUnlock() {
        if (isFinishing() || isDestroyed) return;

        if (PrefsHelper.isPinLockedOut(this)) {
            Toast.makeText(this, R.string.pin_locked, Toast.LENGTH_SHORT).show();
            return;
        }

        String pin = binding.editPin.getText() != null
                ? binding.editPin.getText().toString() : "";

        if (pin.isEmpty()) {
            binding.editPinLayout.setError(getString(R.string.pin_empty));
            return;
        }

        if (PrefsHelper.verifyPin(this, pin)) {
            PrefsHelper.resetPinFailCount(this);
            PrefsHelper.clearPinLockout(this);
            openMain();
        } else {
            PrefsHelper.incrementPinFailCount(this);
            updateAttemptsDisplay();

            if (PrefsHelper.getRemainingAttempts(this) <= 0) {
                PrefsHelper.setPinLockout(this);
                Toast.makeText(this, R.string.pin_too_many_attempts, Toast.LENGTH_LONG).show();
                updateLockStatus();
            } else {
                Toast.makeText(this,
                        getString(R.string.pin_wrong_attempts,
                                PrefsHelper.getRemainingAttempts(this)),
                        Toast.LENGTH_SHORT).show();
                binding.editPin.setText("");
                binding.editPin.requestFocus();
            }
        }
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Dùng flag thay vì isDestroyed để hỗ trợ API < 17
    private boolean isDestroyed = false;

    @Override
    protected void onResume() {
        super.onResume();
        isDestroyed = false;
        updateLockStatus();
        updateAttemptsDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isDestroyed = true;
        // Cancel biometric prompt when going to background
        if (currentPrompt != null) {
            try {
                currentPrompt.cancelAuthentication();
            } catch (Exception ignored) {
            }
            currentPrompt = null;
        }
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        cancelScheduledUnlockCheck();
        handler.removeCallbacksAndMessages(null);
        if (currentPrompt != null) {
            try {
                currentPrompt.cancelAuthentication();
            } catch (Exception ignored) {
            }
            currentPrompt = null;
        }
        binding = null;
        executor = null;
        super.onDestroy();
    }
}
