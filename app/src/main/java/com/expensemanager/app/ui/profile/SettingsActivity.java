package com.expensemanager.app.ui.profile;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.expensemanager.app.databinding.ActivitySettingsBinding;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.util.BackupManager;
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

        binding.switchDark.setOnCheckedChangeListener((b, checked) -> {
            PrefsHelper.setDarkMode(this, checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.switchReminder.setOnCheckedChangeListener((b, checked) -> {
            PrefsHelper.setReminderEnabled(this, checked);
            if (checked) ReminderScheduler.scheduleDaily(this);
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

        binding.btnPin.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint("PIN 4-6 số");
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            new AlertDialog.Builder(this)
                    .setTitle("Đặt PIN")
                    .setView(input)
                    .setPositiveButton("Bật", (d, w) -> {
                        String pin = input.getText().toString();
                        PrefsHelper.setPinEnabled(this, true, String.valueOf(pin.hashCode()));
                        Toast.makeText(this, "Đã bật khóa PIN", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Tắt", (d, w) -> {
                        PrefsHelper.setPinEnabled(this, false, "");
                        Toast.makeText(this, "Đã tắt PIN", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        binding.btnExport.setOnClickListener(v -> {
            String uid = authRepo.getUid();
            if (uid == null) return;
            BackupManager.exportUserData(this, uid,
                    () -> Toast.makeText(this, "Đã xuất", Toast.LENGTH_SHORT).show(),
                    () -> Toast.makeText(this, "Lỗi xuất", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
