package com.expensemanager.app.ui.profile;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
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
import java.util.regex.Pattern;

public class SecurityActivity extends AppCompatActivity {
    private static final Pattern PIN_DIGITS_ONLY = Pattern.compile("^[0-9]{4,6}$");

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

        refreshUiState();

        binding.switchPin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // Ignore programmatic changes
            if (isChecked) {
                showPinSetupDialog();
            } else {
                showDisablePinConfirmation();
            }
        });

        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (!PrefsHelper.isPinEnabled(this)) {
                // Không cho bật biometric khi PIN chưa bật
                buttonView.setChecked(false);
                return;
            }
            PrefsHelper.setBiometricEnabled(this, isChecked);
        });

        binding.switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            PrefsHelper.setReminderEnabled(this, isChecked);
            binding.layoutReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
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

    private void refreshUiState() {
        boolean pinEnabled = PrefsHelper.isPinEnabled(this);
        binding.switchPin.setChecked(pinEnabled);

        boolean biometricAvailable = isBiometricAvailable();
        boolean biometricEnabled = PrefsHelper.isBiometricEnabled(this);

        binding.layoutBiometric.setVisibility(
                pinEnabled && biometricAvailable ? View.VISIBLE : View.GONE);
        // Disable biometric switch if PIN is not enabled
        binding.switchBiometric.setEnabled(pinEnabled && biometricAvailable);
        binding.switchBiometric.setChecked(biometricEnabled && pinEnabled);

        boolean reminderEnabled = PrefsHelper.isReminderEnabled(this);
        binding.switchReminder.setChecked(reminderEnabled);
        binding.layoutReminderTime.setVisibility(reminderEnabled ? View.VISIBLE : View.GONE);
        updateReminderTimeButton();
    }

    private boolean isBiometricAvailable() {
        try {
            return BiometricManager.from(this)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    private void showDisablePinConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.disable_pin_confirm_title)
                .setMessage(R.string.disable_pin_confirm_message)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    PrefsHelper.disablePin(this);
                    PrefsHelper.setBiometricEnabled(this, false);
                    binding.layoutBiometric.setVisibility(View.GONE);
                    binding.switchBiometric.setChecked(false);
                    binding.switchBiometric.setEnabled(false);
                    Toast.makeText(this, R.string.pin_disabled, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> {
                    // Revert switch
                    binding.switchPin.setChecked(true);
                })
                .setOnCancelListener(d -> {
                    binding.switchPin.setChecked(true);
                })
                .show();
    }

    private void showPinSetupDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.pin_setup_hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(R.string.setup_pin_title)
                .setView(input)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    String pin = input.getText().toString().trim();

                    // Validate: only digits, 4-6 chars
                    if (TextUtils.isEmpty(pin)) {
                        Toast.makeText(this, R.string.pin_empty, Toast.LENGTH_SHORT).show();
                        binding.switchPin.setChecked(false);
                        return;
                    }
                    if (!PIN_DIGITS_ONLY.matcher(pin).matches()) {
                        Toast.makeText(this, R.string.pin_invalid_format, Toast.LENGTH_SHORT).show();
                        binding.switchPin.setChecked(false);
                        return;
                    }

                    String hash = PrefsHelper.hashPin(this, pin);
                    boolean saved = savePinHash(hash);
                    if (saved) {
                        refreshUiState();
                        Toast.makeText(this, R.string.pin_enabled, Toast.LENGTH_SHORT).show();
                    } else {
                        binding.switchPin.setChecked(false);
                    }
                })
                .setNegativeButton(R.string.cancel, (d, w) -> {
                    binding.switchPin.setChecked(false);
                })
                .setOnCancelListener(d -> {
                    binding.switchPin.setChecked(false);
                })
                .show();
    }

    /** Lưu PIN hash. Trả về false nếu lưu thất bại. */
    private boolean savePinHash(String hash) {
        try {
            PrefsHelper.setPinEnabled(this, true, hash);
            PrefsHelper.resetPinFailCount(this);
            PrefsHelper.clearPinLockout(this);
            return true;
        } catch (Exception e) {
            Toast.makeText(this, R.string.pin_save_failed, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void updateReminderTimeButton() {
        int h = PrefsHelper.getReminderHour(this);
        int m = PrefsHelper.getReminderMinute(this);
        binding.btnReminderTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
