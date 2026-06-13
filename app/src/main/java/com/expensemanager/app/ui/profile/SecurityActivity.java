package com.expensemanager.app.ui.profile;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivitySecurityBinding;
import com.expensemanager.app.util.PrefsHelper;
import com.expensemanager.app.util.ReminderScheduler;

import java.util.Locale;

public class SecurityActivity extends AppCompatActivity {
    private ActivitySecurityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecurityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.security);
        }

        boolean pinEnabled = PrefsHelper.isPinEnabled(this);
        boolean biometricAvailable = BiometricManager.from(this)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;

        binding.switchPin.setChecked(pinEnabled);
        binding.layoutBiometric.setVisibility(
                pinEnabled && biometricAvailable ? View.VISIBLE : View.GONE);
        boolean biometricEnabled = PrefsHelper.isBiometricEnabled(this);
        binding.switchBiometric.setChecked(biometricEnabled);

        boolean reminderEnabled = PrefsHelper.isReminderEnabled(this);
        binding.switchReminder.setChecked(reminderEnabled);
        binding.layoutReminderTime.setVisibility(
                reminderEnabled ? View.VISIBLE : View.GONE);
        updateReminderTimeButton();

        binding.switchPin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showPinSetupDialog();
            } else {
                PrefsHelper.setPinEnabled(this, false, null);
                PrefsHelper.resetPinFailCount(this);
                PrefsHelper.clearPinLockout(this);
                PrefsHelper.setBiometricEnabled(this, false);
                binding.layoutBiometric.setVisibility(View.GONE);
                Toast.makeText(this, "Đã tắt khóa PIN", Toast.LENGTH_SHORT).show();
            }
        });

        binding.switchBiometric.setOnCheckedChangeListener((b, checked) -> {
            PrefsHelper.setBiometricEnabled(this, checked);
        });

        binding.switchReminder.setOnCheckedChangeListener((b, checked) -> {
            PrefsHelper.setReminderEnabled(this, checked);
            binding.layoutReminderTime.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (checked) {
                ReminderScheduler.scheduleDaily(this);
            } else {
                ReminderScheduler.cancel(this);
            }
        });

        binding.btnReminderTime.setOnClickListener(v -> {
            int hour = PrefsHelper.getReminderHour(this);
            int minute = PrefsHelper.getReminderMinute(this);
            new TimePickerDialog(this, (tp, h, m) -> {
                PrefsHelper.setReminderTime(this, h, m);
                updateReminderTimeButton();
                if (PrefsHelper.isReminderEnabled(this)) {
                    ReminderScheduler.scheduleDaily(this);
                }
            }, hour, minute, true).show();
        });
    }

    private void updateReminderTimeButton() {
        int h = PrefsHelper.getReminderHour(this);
        int m = PrefsHelper.getReminderMinute(this);
        binding.btnReminderTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
    }

    private void showPinSetupDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhập mã PIN 4-6 số");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Đặt mã PIN")
                .setView(input)
                .setPositiveButton("Xác nhận", (d, w) -> {
                    String pin = input.getText().toString().trim();
                    if (pin.length() >= 4 && pin.length() <= 6) {
                        String hash = PrefsHelper.hashPin(this, pin);
                        PrefsHelper.setPinEnabled(this, true, hash);
                        PrefsHelper.resetPinFailCount(this);
                        PrefsHelper.clearPinLockout(this);

                        boolean biometricAvailable = BiometricManager.from(this)
                                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                                == BiometricManager.BIOMETRIC_SUCCESS;
                        binding.layoutBiometric.setVisibility(
                                biometricAvailable ? View.VISIBLE : View.GONE);
                        Toast.makeText(this, "Đã bật khóa PIN", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "PIN phải 4-6 số", Toast.LENGTH_SHORT).show();
                        binding.switchPin.setChecked(false);
                    }
                })
                .setNegativeButton("Hủy", (d, w) -> binding.switchPin.setChecked(false))
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
