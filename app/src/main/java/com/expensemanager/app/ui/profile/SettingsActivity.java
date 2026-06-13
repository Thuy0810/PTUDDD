package com.expensemanager.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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
            getSupportActionBar().setTitle("Cài đặt");
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
            input.setHint("Mật khẩu mới");
            input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new AlertDialog.Builder(this)
                    .setTitle("Đổi mật khẩu")
                    .setView(input)
                    .setPositiveButton("Lưu", (d, w) -> {
                        String pw = input.getText().toString();
                        if (pw.length() < 6) {
                            Toast.makeText(this, "Mật khẩu >= 6 ký tự", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        authRepo.changePassword(pw)
                                .addOnSuccessListener(x ->
                                        Toast.makeText(this, "Đã đổi mật khẩu", Toast.LENGTH_SHORT).show())
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
                    () -> Toast.makeText(this, "Đã xuất", Toast.LENGTH_SHORT).show(),
                    () -> Toast.makeText(this, "Lỗi xuất", Toast.LENGTH_SHORT).show());
        });

        binding.layoutCurrencySymbol.setOnClickListener(v -> showCurrencySymbolPicker());
        binding.layoutCurrencyPosition.setOnClickListener(v -> showCurrencyPositionPicker());
        binding.layoutCurrencyLocale.setOnClickListener(v -> showCurrencyLocalePicker());
    }

    private void updateCurrencyDisplay() {
        binding.textCurrencySymbol.setText(PrefsHelper.getCurrencySymbol(this));
        binding.textCurrencyPosition.setText(
                PrefsHelper.isCurrencySymbolBefore(this) ? "Trước số" : "Sau số");
        String loc = PrefsHelper.getCurrencyLocale(this);
        binding.textCurrencyLocale.setText(getLocaleDisplayName(loc));
    }

    private String getLocaleDisplayName(String localeTag) {
        switch (localeTag) {
            case "vi_VN": return "Việt Nam (1.234.567)";
            case "en_US": return "US (1,234,567.00)";
            case "ja_JP": return "Nhật Bản (1,234,567)";
            case "de_DE": return "Đức (1.234.567,00)";
            default: return localeTag;
        }
    }

    private void showCurrencySymbolPicker() {
        String[] symbols = {"đ", "$", "€", "¥", "£", "₫", "₹", "₩", "R$", "₽"};
        int current = java.util.Arrays.asList(symbols).indexOf(
                PrefsHelper.getCurrencySymbol(this));
        if (current < 0) current = 0;

        new AlertDialog.Builder(this)
                .setTitle("Ký hiệu tiền tệ")
                .setSingleChoiceItems(symbols, current, (d, which) -> {
                    PrefsHelper.setCurrencySymbol(this, symbols[which]);
                    MoneyFormat.applySettings(
                            symbols[which],
                            PrefsHelper.isCurrencySymbolBefore(this),
                            PrefsHelper.getCurrencyDecimals(this),
                            parseLocale(PrefsHelper.getCurrencyLocale(this)));
                    updateCurrencyDisplay();
                    d.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCurrencyPositionPicker() {
        String[] labels = {"Trước số (đ 1.000)", "Sau số (1.000 đ)"};
        boolean before = PrefsHelper.isCurrencySymbolBefore(this);

        new AlertDialog.Builder(this)
                .setTitle("Vị trí ký hiệu")
                .setSingleChoiceItems(labels, before ? 0 : 1, (d, which) -> {
                    boolean symbolBefore = (which == 0);
                    PrefsHelper.setCurrencyPosition(this, symbolBefore);
                    MoneyFormat.applySettings(
                            PrefsHelper.getCurrencySymbol(this),
                            symbolBefore,
                            PrefsHelper.getCurrencyDecimals(this),
                            parseLocale(PrefsHelper.getCurrencyLocale(this)));
                    updateCurrencyDisplay();
                    d.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCurrencyLocalePicker() {
        String[] locales = {"vi_VN", "en_US", "ja_JP", "de_DE"};
        String[] labels = {"Việt Nam (1.234.567 đ)", "US (USD 1,234.56)", "Nhật Bản (¥1,234)", "Đức (1.234,56 €)"};
        String current = PrefsHelper.getCurrencyLocale(this);
        int idx = java.util.Arrays.asList(locales).indexOf(current);
        if (idx < 0) idx = 0;

        new AlertDialog.Builder(this)
                .setTitle("Định dạng số")
                .setSingleChoiceItems(labels, idx, (d, which) -> {
                    PrefsHelper.setCurrencyLocale(this, locales[which]);
                    MoneyFormat.applySettings(
                            PrefsHelper.getCurrencySymbol(this),
                            PrefsHelper.isCurrencySymbolBefore(this),
                            PrefsHelper.getCurrencyDecimals(this),
                            parseLocale(locales[which]));
                    updateCurrencyDisplay();
                    d.dismiss();
                })
                .setNegativeButton("Hủy", null)
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
