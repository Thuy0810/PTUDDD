package com.expensemanager.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.PrefsHelper;

import java.util.Locale;

public class ExpenseApplication extends Application {
    public static final String CHANNEL_REMINDER = "expense_reminder";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        applyCurrencySettings();
    }

    private void applyCurrencySettings() {
        try {
            String symbol = PrefsHelper.getCurrencySymbol(this);
            int decimals = PrefsHelper.getCurrencyDecimals(this);
            String localeTag = PrefsHelper.getCurrencyLocale(this);

            Locale loc;
            if (localeTag != null && !localeTag.isEmpty()) {
                String[] parts = localeTag.split("[_]");
                if (parts.length >= 2) {
                    loc = new Locale(parts[0], parts[1]);
                } else {
                    loc = new Locale(localeTag);
                }
            } else {
                loc = new Locale("vi", "VN");
            }

            MoneyFormat.applySettings(symbol, false, decimals, loc);
        } catch (Exception e) {
            MoneyFormat.reset();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_REMINDER,
                    "Nhắc nhở chi tiêu",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
