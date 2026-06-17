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
import com.expensemanager.app.util.LocaleHelper;
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
        ((android.widget.TextView) findViewById(R.id.textHeaderTitle)).setText(R.string.settings);
        findViewById(R.id.btnHeaderBack).setOnClickListener(v -> finish());

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
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            AlertDialog d = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.change_password))
                    .setView(input)
                    .setPositiveButton(getString(R.string.save), null)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create();
            d.show();
            // Override để không tự đóng khi mật khẩu chưa hợp lệ
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                String pw = input.getText().toString();
                if (pw.length() < 6) {
                    Toast.makeText(this, getString(R.string.j3_password_min_length), Toast.LENGTH_SHORT).show();
                    return;
                }
                btn.setEnabled(false);
                authRepo.changePassword(pw)
                        .addOnSuccessListener(x -> {
                            Toast.makeText(this, getString(R.string.j3_password_changed), Toast.LENGTH_SHORT).show();
                            d.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            btn.setEnabled(true);
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
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

        updateLanguageDisplay();
        binding.layoutLanguage.setOnClickListener(v -> showLanguagePicker());
    }

    private void updateLanguageDisplay() {
        boolean en = LocaleHelper.ENGLISH.equals(LocaleHelper.getLanguage());
        binding.textLanguage.setText(getString(en
                ? R.string.language_english
                : R.string.language_vietnamese));
    }

    private void showLanguagePicker() {
        final String[] tags = { LocaleHelper.VIETNAMESE, LocaleHelper.ENGLISH };
        String[] labels = {
                getString(R.string.language_vietnamese),
                getString(R.string.language_english)
        };
        int checked = LocaleHelper.ENGLISH.equals(LocaleHelper.getLanguage()) ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_language)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (!tags[which].equals(LocaleHelper.getLanguage())) {
                        LocaleHelper.setLanguage(tags[which]);
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static final String[] CURRENCY_CODES = {"VND", "USD", "EUR"};
    private static final String[] LOCALE_TAGS = {"vi_VN", "en_US"};

    private void updateCurrencyDisplay() {
        binding.textCurrencySymbol.setText(
                PrefsHelper.getCurrencySymbol(this).toUpperCase(java.util.Locale.ROOT));
        binding.textCurrencyPosition.setText(getString(PrefsHelper.isCurrencySymbolBefore(this)
                ? R.string.s5_pos_before : R.string.s5_pos_after));
        boolean comma = "en_US".equals(PrefsHelper.getCurrencyLocale(this));
        binding.textCurrencyLocale.setText(getString(comma
                ? R.string.s5_fmt_comma : R.string.s5_fmt_dot));
    }

    /** Áp dụng toàn bộ cài đặt tiền tệ hiện lưu vào MoneyFormat + cập nhật hiển thị. */
    private void applyCurrency() {
        MoneyFormat.applySettings(
                PrefsHelper.getCurrencySymbol(this),
                PrefsHelper.isCurrencySymbolBefore(this),
                PrefsHelper.getCurrencyDecimals(this),
                parseLocale(PrefsHelper.getCurrencyLocale(this)));
        updateCurrencyDisplay();
    }

    private void showCurrencySymbolPicker() {
        String[] labels = {"VND (₫)", "USD ($)", "EUR (€)"};
        int current = java.util.Arrays.asList(CURRENCY_CODES).indexOf(
                PrefsHelper.getCurrencySymbol(this).toUpperCase(java.util.Locale.ROOT));
        if (current < 0) current = 0;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j3_currency_symbol_title))
                .setSingleChoiceItems(labels, current, (d, which) -> {
                    PrefsHelper.setCurrencySymbol(this, CURRENCY_CODES[which]);
                    applyCurrency();
                    d.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showCurrencyPositionPicker() {
        String[] labels = {getString(R.string.s5_pos_before), getString(R.string.s5_pos_after)};
        int current = PrefsHelper.isCurrencySymbolBefore(this) ? 0 : 1;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j3_currency_position_title))
                .setSingleChoiceItems(labels, current, (d, which) -> {
                    PrefsHelper.setCurrencyPosition(this, which == 0);
                    applyCurrency();
                    d.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showCurrencyLocalePicker() {
        String[] labels = {getString(R.string.s5_fmt_dot), getString(R.string.s5_fmt_comma)};
        int current = "en_US".equals(PrefsHelper.getCurrencyLocale(this)) ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j3_number_format_title))
                .setSingleChoiceItems(labels, current, (d, which) -> {
                    PrefsHelper.setCurrencyLocale(this, LOCALE_TAGS[which]);
                    applyCurrency();
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
