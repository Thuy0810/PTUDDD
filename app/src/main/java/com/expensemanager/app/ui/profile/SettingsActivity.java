package com.expensemanager.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivitySettingsBinding;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.util.BackupManager;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.PrefsHelper;
import com.expensemanager.app.util.ReminderScheduler;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private final AuthRepository authRepo = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.settings));
        }

        binding.switchDark.setChecked(PrefsHelper.isDarkMode(this));
        binding.switchReminder.setChecked(PrefsHelper.isReminderEnabled(this));

        updateCurrencyDisplay();

        binding.switchDark.setOnCheckedChangeListener((b, checked) -> {
            PrefsHelper.setDarkMode(this, checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.switchReminder.setOnCheckedChangeListener((b, checked) -> {
            PrefsHelper.setReminderEnabled(this, checked);
            if (checked) {
                ReminderScheduler.scheduleDaily(this);
            } else {
                ReminderScheduler.cancel(this);
            }
        });

        binding.btnChangePassword.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint(getString(R.string.j3_new_password_hint));
            input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.change_password))
                    .setView(input)
                    .setPositiveButton(getString(R.string.save), (d, w) -> {
                        String pw = input.getText().toString();
                        if (pw.length() < 6) {
                            Toast.makeText(this, getString(R.string.j3_password_min_length), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        authRepo.changePassword(pw)
                                .addOnSuccessListener(x ->
                                        Toast.makeText(this, getString(R.string.j3_password_changed), Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .show();
        });

        binding.btnPin.setOnClickListener(v ->
                startActivity(new Intent(this, SecurityActivity.class)));

        binding.btnExport.setOnClickListener(v -> {
            String uid = authRepo.getUid();
            if (uid == null) return;
            BackupManager.exportUserData(this, uid,
                    () -> Toast.makeText(this, getString(R.string.j3_export_done), Toast.LENGTH_SHORT).show(),
                    () -> Toast.makeText(this, getString(R.string.j3_export_error), Toast.LENGTH_SHORT).show());
        });

        binding.layoutCurrencySymbol.setOnClickListener(v -> showCurrencySymbolPicker());
        binding.layoutCurrencyPosition.setOnClickListener(v -> showCurrencyPositionPicker());
        binding.layoutCurrencyLocale.setOnClickListener(v -> showCurrencyLocalePicker());
    }

    private void updateCurrencyDisplay() {
        binding.textCurrencySymbol.setText("vnd");
        binding.textCurrencyPosition.setText(getString(R.string.j3_currency_position_after));
        binding.textCurrencyLocale.setText(getString(R.string.j3_currency_format_comma));
    }

    private String getLocaleDisplayName(String localeTag) {
        return "Dấu phẩy (500,000 vnd)";
    }

    private void showCurrencySymbolPicker() {
        String[] symbols = {"vnd"};
        int current = java.util.Arrays.asList(symbols).indexOf(
                PrefsHelper.getCurrencySymbol(this));
        if (current < 0) current = 0;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j3_currency_symbol_title))
                .setSingleChoiceItems(symbols, current, (d, which) -> {
                    PrefsHelper.setCurrencySymbol(this, symbols[which]);
                    MoneyFormat.applySettings(
                            symbols[which],
                            false,
                            PrefsHelper.getCurrencyDecimals(this),
                            parseLocale("vi_VN"));
                    updateCurrencyDisplay();
                    d.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showCurrencyPositionPicker() {
        String[] labels = {getString(R.string.j3_currency_position_after_full)};

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j3_currency_position_title))
                .setSingleChoiceItems(labels, 0, (d, which) -> {
                    PrefsHelper.setCurrencyPosition(this, false);
                    MoneyFormat.applySettings(
                            "vnd",
                            false,
                            PrefsHelper.getCurrencyDecimals(this),
                            parseLocale("vi_VN"));
                    updateCurrencyDisplay();
                    d.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showCurrencyLocalePicker() {
        String[] locales = {"vi_VN"};
        String[] labels = {getString(R.string.j3_currency_format_comma)};

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j3_number_format_title))
                .setSingleChoiceItems(labels, 0, (d, which) -> {
                    PrefsHelper.setCurrencyLocale(this, locales[which]);
                    MoneyFormat.applySettings(
                            "vnd",
                            false,
                            PrefsHelper.getCurrencyDecimals(this),
                            parseLocale(locales[which]));
                    updateCurrencyDisplay();
                    d.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private java.util.Locale parseLocale(String localeTag) {
        if (localeTag == null || localeTag.isEmpty()) return new java.util.Locale("vi", "VN");
        String[] parts = localeTag.split("[_]");
        if (parts.length >= 2) return new java.util.Locale(parts[0], parts[1]);
        return new java.util.Locale(localeTag);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
