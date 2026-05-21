package com.expensemanager.app.ui.profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.databinding.ActivityRegisterBinding;
import com.expensemanager.app.data.repository.AuthRepository;

public class EditProfileActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRegisterBinding binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sửa hồ sơ");
        }
        binding.btnRegister.setText("Lưu");

        authRepo.observeProfile().observe(this, p -> {
            if (p != null) binding.editName.setText(p.getDisplayName());
        });

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.editName.getText() != null
                    ? binding.editName.getText().toString().trim() : "";
            authRepo.updateDisplayName(name)
                    .addOnSuccessListener(x -> {
                        Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
