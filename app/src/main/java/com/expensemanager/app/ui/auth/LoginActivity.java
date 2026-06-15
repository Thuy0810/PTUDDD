package com.expensemanager.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivityLoginBinding;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.ui.main.MainActivity;
import com.expensemanager.app.util.PrefsHelper;
import com.expensemanager.app.util.ReminderScheduler;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private final AuthRepository authRepo = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> login());
        binding.textRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        binding.textForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        String prefillEmail = getIntent().getStringExtra("email");
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            binding.editEmail.setText(prefillEmail);
        }
    }

    private void login() {
        String email = binding.editEmail.getText() != null
                ? binding.editEmail.getText().toString().trim() : "";
        String password = binding.editPassword.getText() != null
                ? binding.editPassword.getText().toString() : "";

        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_password), Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText(getString(R.string.j2_logging_in));

        authRepo.login(email, password)
                .addOnSuccessListener(v -> {
                    PrefsHelper.clearPendingLogout(this);
                    ReminderScheduler.scheduleDaily(this);
                    String uid = authRepo.getUid();
                    if (uid != null) {
                        new com.expensemanager.app.domain.usecase.RecurringService()
                                .runOnLogin(uid);
                    }
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnLogin.setEnabled(true);
                    binding.btnLogin.setText(getString(R.string.login));
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showForgotPasswordDialog() {
        TextInputEditText editEmail = new TextInputEditText(this);
        editEmail.setHint(getString(R.string.j2_enter_your_email));
        editEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        editEmail.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_password))
                .setMessage(getString(R.string.reset_password_desc))
                .setView(editEmail)
                .setPositiveButton(getString(R.string.j2_send), (d, w) -> {
                    String email = editEmail.getText() != null
                            ? editEmail.getText().toString().trim() : "";
                    if (email.isEmpty()) {
                        Toast.makeText(this, getString(R.string.j2_enter_email), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnSuccessListener(r -> Toast.makeText(this,
                                    getString(R.string.j2_reset_email_sent), Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    getString(R.string.j2_send_failed, e.getMessage()), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
