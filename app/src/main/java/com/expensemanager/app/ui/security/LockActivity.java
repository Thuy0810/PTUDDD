package com.expensemanager.app.ui.security;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnUnlock.setOnClickListener(v -> {
            String pin = binding.editPin.getText() != null ? binding.editPin.getText().toString() : "";
            if (PrefsHelper.verifyPin(this, pin)) {
                openMain();
            } else {
                Toast.makeText(this, "PIN sai", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnBiometric.setOnClickListener(v -> showBiometric());
        if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometric();
        }
    }

    private void showBiometric() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
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

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
