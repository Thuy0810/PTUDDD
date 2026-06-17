package com.expensemanager.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivityRegisterBinding;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.ui.auth.LoginActivity;
import com.expensemanager.app.util.PrefsHelper;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private final AuthRepository authRepo = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ((android.widget.TextView) findViewById(R.id.textHeaderTitle)).setText(R.string.register);
        findViewById(R.id.btnHeaderBack).setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v -> register());
        binding.textLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        String name = binding.editName.getText() != null
                ? binding.editName.getText().toString().trim() : "";
        String email = binding.editEmail.getText() != null
                ? binding.editEmail.getText().toString().trim() : "";
        String password = binding.editPassword.getText() != null
                ? binding.editPassword.getText().toString() : "";
        String confirm = binding.editConfirmPassword.getText() != null
                ? binding.editConfirmPassword.getText().toString() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.j2_enter_full_name), Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.j2_password_min_6), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnRegister.setEnabled(false);
        binding.btnRegister.setText(getString(R.string.j2_registering));

        authRepo.register(email, password, name)
                .addOnSuccessListener(v -> {
                    authRepo.logout();
                    PrefsHelper.setPendingLogout(this, true);
                    Toast.makeText(this, getString(R.string.j2_register_success_login), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    binding.btnRegister.setEnabled(true);
                    binding.btnRegister.setText(getString(R.string.register));
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
