package com.expensemanager.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.ui.main.MainActivity;
import com.expensemanager.app.ui.security.LockActivity;
import com.expensemanager.app.util.PrefsHelper;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AuthRepository auth = new AuthRepository();
            if (auth.getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
            } else if (PrefsHelper.isPinEnabled(this)) {
                startActivity(new Intent(this, LockActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }, 1200);
    }
}
