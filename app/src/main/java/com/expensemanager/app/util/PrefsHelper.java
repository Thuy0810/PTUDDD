package com.expensemanager.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PrefsHelper {
    private static final String TAG = "PrefsHelper";
    private static final String PREFS = "expense_prefs";
    private static final String KEY_PIN_ENABLED = "pin_enabled";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_REMINDER = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MIN = "reminder_min";
    private static final String KEY_PENDING_LOGOUT = "pending_logout";
    private static final String KEY_SALT = "pin_salt";
    private static final String KEY_PIN_FAILS = "pin_fail_count";
    private static final String KEY_PIN_LOCK_UNTIL = "pin_lock_until";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_CURRENCY_SYMBOL = "currency_symbol";
    private static final String KEY_CURRENCY_POSITION = "currency_position";
    private static final String KEY_CURRENCY_DECIMALS = "currency_decimals";
    private static final String KEY_CURRENCY_LOCALE = "currency_locale";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";

    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 5 * 60 * 1000L; // 5 minutes

    /** Lazy-initialized, thread-safe EncryptedSharedPreferences instance. */
    private static volatile SharedPreferences cachedPrefs;

    private PrefsHelper() {}

    /**
     * Returns the cached EncryptedSharedPreferences instance.
     * Uses application context to avoid memory leaks.
     * Thread-safe double-checked locking.
     */
    @NonNull
    private static SharedPreferences prefs(Context ctx) {
        if (cachedPrefs == null) {
            synchronized (PrefsHelper.class) {
                if (cachedPrefs == null) {
                    cachedPrefs = createEncryptedPrefs(ctx);
                }
            }
        }
        return cachedPrefs;
    }

    @NonNull
    private static SharedPreferences createEncryptedPrefs(Context ctx) {
        Context appCtx = ctx.getApplicationContext();
        try {
            MasterKey key = new MasterKey.Builder(appCtx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    appCtx, PREFS, key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            // Ghi log kỹ thuật nhưng KHÔNG ghi dữ liệu nhạy cảm
            Log.e(TAG, "EncryptedSharedPreferences init failed: " + e.getClass().getSimpleName(), e);
            // Fallback: không âm thầm dùng unencrypted — crash rõ ràng để developer biết
            throw new IllegalStateException(
                    "EncryptedSharedPreferences unavailable. Device may be rooted or encryption is unsupported.",
                    e);
        }
    }

    // ========== PIN ==========

    public static boolean isPinEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_PIN_ENABLED, false);
    }

    /**
     * Bật PIN — chỉ gọi sau khi đã lưu thành công.
     */
    public static void setPinEnabled(Context ctx, boolean enabled, String pinHash) {
        prefs(ctx).edit()
                .putBoolean(KEY_PIN_ENABLED, enabled)
                .putString(KEY_PIN_HASH, pinHash)
                .apply();
    }

    /**
     * Tắt PIN — xóa toàn bộ dữ liệu liên quan bao gồm biometric.
     * KHÔNG lưu hash null, mà dùng remove().
     */
    public static void disablePin(Context ctx) {
        prefs(ctx).edit()
                .remove(KEY_PIN_ENABLED)
                .remove(KEY_PIN_HASH)
                .remove(KEY_SALT)
                .remove(KEY_PIN_FAILS)
                .remove(KEY_PIN_LOCK_UNTIL)
                .remove(KEY_BIOMETRIC_ENABLED)
                .apply();
    }

    public static boolean verifyPin(Context ctx, String pin) {
        String storedHash = prefs(ctx).getString(KEY_PIN_HASH, "");
        if (storedHash == null || storedHash.isEmpty()) return false;
        String computedHash = hashPin(ctx, pin);
        // So sánh hằng-thời-gian để tránh timing side-channel.
        return java.security.MessageDigest.isEqual(
                computedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                storedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Hash PIN với PBKDF2 — KHÔNG ghi PIN hoặc hash ra logcat.
     */
    public static String hashPin(Context ctx, String pin) {
        try {
            String salt = getOrCreateSalt(ctx);
            byte[] saltBytes = Base64.decode(salt, Base64.NO_WRAP);
            int iterations = 120_000;
            int keyLength = 256;

            PBEKeySpec spec = new PBEKeySpec(
                    pin.toCharArray(), saltBytes, iterations, keyLength);
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return iterations + ":" + Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 hashing failed", e);
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

    // ========== PIN fail counter ==========

    public static int getPinFailCount(Context ctx) {
        return prefs(ctx).getInt(KEY_PIN_FAILS, 0);
    }

    public static void incrementPinFailCount(Context ctx) {
        prefs(ctx).edit().putInt(KEY_PIN_FAILS, getPinFailCount(ctx) + 1).apply();
    }

    public static void resetPinFailCount(Context ctx) {
        prefs(ctx).edit().putInt(KEY_PIN_FAILS, 0).apply();
    }

    public static int getRemainingAttempts(Context ctx) {
        return Math.max(0, MAX_PIN_ATTEMPTS - getPinFailCount(ctx));
    }

    public static boolean isPinLockedOut(Context ctx) {
        long lockUntil = prefs(ctx).getLong(KEY_PIN_LOCK_UNTIL, 0);
        return lockUntil > 0 && System.currentTimeMillis() < lockUntil;
    }

    public static long getLockRemainingMs(Context ctx) {
        long lockUntil = prefs(ctx).getLong(KEY_PIN_LOCK_UNTIL, 0);
        return Math.max(0, lockUntil - System.currentTimeMillis());
    }

    public static void setPinLockout(Context ctx) {
        long lockUntil = System.currentTimeMillis() + LOCK_DURATION_MS;
        prefs(ctx).edit().putLong(KEY_PIN_LOCK_UNTIL, lockUntil).apply();
    }

    public static void clearPinLockout(Context ctx) {
        prefs(ctx).edit().putLong(KEY_PIN_LOCK_UNTIL, 0).apply();
    }

    // ========== Biometric ==========

    public static boolean isBiometricEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public static void setBiometricEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    // ========== Dark mode ==========

    public static boolean isDarkMode(Context ctx) {
        return prefs(ctx).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context ctx, boolean dark) {
        prefs(ctx).edit().putBoolean(KEY_DARK_MODE, dark).apply();
    }

    // ========== Reminder ==========

    public static boolean isReminderEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_REMINDER, true);
    }

    public static void setReminderEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_REMINDER, enabled).apply();
    }

    public static int getReminderHour(Context ctx) {
        return prefs(ctx).getInt(KEY_REMINDER_HOUR, 21);
    }

    public static int getReminderMinute(Context ctx) {
        return prefs(ctx).getInt(KEY_REMINDER_MIN, 0);
    }

    public static void setReminderTime(Context ctx, int hour, int minute) {
        prefs(ctx).edit()
                .putInt(KEY_REMINDER_HOUR, hour)
                .putInt(KEY_REMINDER_MIN, minute)
                .apply();
    }

    // ========== Pending logout ==========

    public static void setPendingLogout(Context ctx, boolean pending) {
        prefs(ctx).edit().putBoolean(KEY_PENDING_LOGOUT, pending).apply();
    }

    public static boolean isPendingLogout(Context ctx) {
        return prefs(ctx).getBoolean(KEY_PENDING_LOGOUT, false);
    }

    public static void clearPendingLogout(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_PENDING_LOGOUT, false).apply();
    }

    // ========== Onboarding ==========

    /** Da xem man hinh gioi thieu chua (chi hien thi lan dau mo app). */
    public static boolean isOnboardingDone(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public static void setOnboardingDone(Context ctx, boolean done) {
        prefs(ctx).edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    // ========== Currency settings ==========

    public static String getCurrencySymbol(Context ctx) {
        return prefs(ctx).getString(KEY_CURRENCY_SYMBOL, "vnd");
    }

    public static void setCurrencySymbol(Context ctx, String symbol) {
        prefs(ctx).edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply();
    }

    public static boolean isCurrencySymbolBefore(Context ctx) {
        return prefs(ctx).getBoolean(KEY_CURRENCY_POSITION, false);
    }

    public static void setCurrencyPosition(Context ctx, boolean before) {
        prefs(ctx).edit().putBoolean(KEY_CURRENCY_POSITION, before).apply();
    }

    public static int getCurrencyDecimals(Context ctx) {
        return prefs(ctx).getInt(KEY_CURRENCY_DECIMALS, 0);
    }

    public static void setCurrencyDecimals(Context ctx, int decimals) {
        prefs(ctx).edit().putInt(KEY_CURRENCY_DECIMALS, decimals).apply();
    }

    public static String getCurrencyLocale(Context ctx) {
        return prefs(ctx).getString(KEY_CURRENCY_LOCALE, "vi_VN");
    }

    public static void setCurrencyLocale(Context ctx, String localeTag) {
        prefs(ctx).edit().putString(KEY_CURRENCY_LOCALE, localeTag).apply();
    }
}
