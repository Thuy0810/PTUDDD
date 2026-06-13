package com.expensemanager.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PrefsHelper {
    private static final String PREFS = "expense_prefs";
    private static final String KEY_PIN_ENABLED = "pin_enabled";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_REMINDER = "reminder_enabled";
    private static final String KEY_PENDING_LOGOUT = "pending_logout";
    private static final String KEY_SALT = "pin_salt";

    private PrefsHelper() {}

    @NonNull
    private static SharedPreferences prefs(Context ctx) {
        try {
            MasterKey key = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            return EncryptedSharedPreferences.create(ctx, PREFS, key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            throw new RuntimeException("Khong the khoi tao EncryptedSharedPreferences", e);
        }
    }

    public static boolean isPinEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_PIN_ENABLED, false);
    }

    public static void setPinEnabled(Context ctx, boolean enabled, String pinHash) {
        prefs(ctx).edit()
                .putBoolean(KEY_PIN_ENABLED, enabled)
                .putString(KEY_PIN_HASH, pinHash)
                .apply();
    }

    public static boolean verifyPin(Context ctx, String pin) {
        String hash = hashPin(ctx, pin);
        return hash.equals(prefs(ctx).getString(KEY_PIN_HASH, ""));
    }

    public static String hashPin(Context ctx, String pin) {
        try {
            String salt = getOrCreateSalt(ctx);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest((pin + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(pin.hashCode());
        }
    }

    private static String getOrCreateSalt(Context ctx) {
        SharedPreferences p = prefs(ctx);
        String existingSalt = p.getString(KEY_SALT, null);
        if (existingSalt != null && !existingSalt.isEmpty()) {
            return existingSalt;
        }
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String newSalt = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
        p.edit().putString(KEY_SALT, newSalt).apply();
        return newSalt;
    }

    public static boolean isDarkMode(Context ctx) {
        return prefs(ctx).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context ctx, boolean dark) {
        prefs(ctx).edit().putBoolean(KEY_DARK_MODE, dark).apply();
    }

    public static boolean isReminderEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_REMINDER, true);
    }

    public static void setReminderEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_REMINDER, enabled).apply();
    }

    public static void setPendingLogout(Context ctx, boolean pending) {
        prefs(ctx).edit().putBoolean(KEY_PENDING_LOGOUT, pending).apply();
    }

    public static boolean isPendingLogout(Context ctx) {
        return prefs(ctx).getBoolean(KEY_PENDING_LOGOUT, false);
    }

    public static void clearPendingLogout(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_PENDING_LOGOUT, false).apply();
    }
}
