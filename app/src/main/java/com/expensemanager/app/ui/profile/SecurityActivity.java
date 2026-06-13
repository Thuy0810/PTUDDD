package com.expensemanager.app.ui.profile;

import android.os.Bundle;
import android.widget.Toast;

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
                Toast.makeText(this, "Bật khóa bảo mật", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Tắt khóa bảo mật", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
