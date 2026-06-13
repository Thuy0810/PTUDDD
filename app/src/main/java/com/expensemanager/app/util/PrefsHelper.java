package com.expensemanager.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public final class PrefsHelper {
    private static final String PREFS = "expense_prefs";
    private static final String KEY_PIN_ENABLED = "pin_enabled";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_REMINDER = "reminder_enabled";
    private static final String KEY_PENDING_LOGOUT = "pending_logout";

    private PrefsHelper() {}

    private static SharedPreferences prefs(Context ctx) {
        try {
            MasterKey key = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            return EncryptedSharedPreferences.create(ctx, PREFS, key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
    }

    public static boolean isPinEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_PIN_ENABLED, false);
    }

    public static void setPinEnabled(Context ctx, boolean enabled, String pinHash) {
        prefs(ctx).edit()
                .putBoolean(KEY_PIN_ENABLED, enabled)
                .putString(KEY_PIN_HASH, pinHash)
                .commit();
    }

    public static boolean verifyPin(Context ctx, String pin) {
        String hash = String.valueOf(pin.hashCode());
        return hash.equals(prefs(ctx).getString(KEY_PIN_HASH, ""));
    }

    public static boolean isDarkMode(Context ctx) {
        return prefs(ctx).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context ctx, boolean dark) {
        prefs(ctx).edit().putBoolean(KEY_DARK_MODE, dark).commit();
    }

    public static boolean isReminderEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_REMINDER, true);
    }

    public static void setReminderEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_REMINDER, enabled).commit();
    }

    public static void setPendingLogout(Context ctx, boolean pending) {
        prefs(ctx).edit().putBoolean(KEY_PENDING_LOGOUT, pending).commit();
    }

    public static boolean isPendingLogout(Context ctx) {
        return prefs(ctx).getBoolean(KEY_PENDING_LOGOUT, false);
    }

    public static void clearPendingLogout(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_PENDING_LOGOUT, false).commit();
    }
}
