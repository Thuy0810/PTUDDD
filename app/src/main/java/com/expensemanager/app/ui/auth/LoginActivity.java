package com.expensemanager.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.databinding.ActivityLoginBinding;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.ui.main.MainActivity;
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
    }

    private void login() {
        String email = binding.editEmail.getText() != null
                ? binding.editEmail.getText().toString().trim() : "";
        String password = binding.editPassword.getText() != null
                ? binding.editPassword.getText().toString() : "";

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText("Đang đăng nhập...");

        authRepo.login(email, password)
                .addOnSuccessListener(v -> {
                    ReminderScheduler.scheduleDaily(this);
                    new com.expensemanager.app.data.repository.RecurringRepository()
                            .catchUp(authRepo.getUid());
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnLogin.setEnabled(true);
                    binding.btnLogin.setText("Đăng nhập");
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showForgotPasswordDialog() {
        TextInputEditText editEmail = new TextInputEditText(this);
        editEmail.setHint("Nhập email của bạn");
        editEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        editEmail.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(this)
                .setTitle("Khôi phục mật khẩu")
                .setMessage("Nhập email đã đăng ký, chúng tôi sẽ gửi link đặt lại mật khẩu.")
                .setView(editEmail)
                .setPositiveButton("Gửi", (d, w) -> {
                    String email = editEmail.getText() != null
                            ? editEmail.getText().toString().trim() : "";
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Nhập email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnSuccessListener(r -> Toast.makeText(this,
                                    "Đã gửi email đặt lại mật khẩu!", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Gửi thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
