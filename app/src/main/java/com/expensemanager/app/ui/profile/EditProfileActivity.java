package com.expensemanager.app.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivityEditProfileBinding;
import com.expensemanager.app.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.edit_profile);
        }

        // Load current email from Firebase Auth (read-only)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            if (email != null) {
                binding.textEmail.setText(email);
            }
        }

        // Load current display name from Firestore
        authRepo.observeProfile().observe(this, p -> {
            if (p != null && binding.editName != null) {
                String name = p.getDisplayName();
                if (name != null && !name.isEmpty()) {
                    binding.editName.setText(name);
                    binding.textAvatarInitial.setText(name.substring(0, 1).toUpperCase());
                }
            }
        });

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        if (isSaving) return;

        String name = binding.editName.getText() != null
                ? binding.editName.getText().toString().trim() : "";

        // Validation
        if (name.isEmpty()) {
            binding.textError.setText(R.string.error_name_empty);
            binding.textError.setVisibility(View.VISIBLE);
            binding.editName.requestFocus();
            return;
        }
        if (name.length() < 2) {
            binding.textError.setText(R.string.error_name_too_short);
            binding.textError.setVisibility(View.VISIBLE);
            binding.editName.requestFocus();
            return;
        }
        if (name.length() > 50) {
            binding.textError.setText(R.string.error_name_too_long);
            binding.textError.setVisibility(View.VISIBLE);
            binding.editName.requestFocus();
            return;
        }

        // Check if name unchanged
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentName = user != null ? user.getDisplayName() : null;
        if (name.equals(currentName != null ? currentName.trim() : "")) {
            // No change needed
            finish();
            return;
        }

        binding.textError.setVisibility(View.GONE);
        isSaving = true;
        binding.btnSave.setEnabled(false);
        binding.layoutLoading.setVisibility(View.VISIBLE);

        authRepo.updateProfile(name)
                .addOnSuccessListener(unused -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    isSaving = false;
                    binding.btnSave.setEnabled(true);
                    binding.layoutLoading.setVisibility(View.GONE);
                    binding.textError.setText(getString(R.string.error_save_failed) + ": " + e.getMessage());
                    binding.textError.setVisibility(View.VISIBLE);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
