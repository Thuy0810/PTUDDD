package com.expensemanager.app.ui.profile;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivitySecurityBinding;
import com.expensemanager.app.util.PrefsHelper;

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

        binding.switchPin.setChecked(PrefsHelper.isPinEnabled(this));
        binding.switchPin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showPinSetupDialog();
            } else {
                PrefsHelper.setPinEnabled(this, false, null);
                Toast.makeText(this, "Da tat khoa bao mat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPinSetupDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhap ma PIN 4-6 so");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Dat ma PIN")
                .setView(input)
                .setPositiveButton("Xac nhan", (d, w) -> {
                    String pin = input.getText().toString().trim();
                    if (pin.length() >= 4 && pin.length() <= 6) {
                        String hash = PrefsHelper.hashPin(this, pin);
                        PrefsHelper.setPinEnabled(this, true, hash);
                        Toast.makeText(this, "Da bat khoa bao mat", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "PIN phai 4-6 so", Toast.LENGTH_SHORT).show();
                        binding.switchPin.setChecked(false);
                    }
                })
                .setNegativeButton("Huy", (d, w) -> binding.switchPin.setChecked(false))
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
