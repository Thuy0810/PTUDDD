package com.expensemanager.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.util.PrefsHelper;

/**
 * ViewModel cho Security: PIN, biometric, reminder.
 *
 * <p>Tách logic phức tạp ra khỏi {@link com.expensemanager.app.ui.profile.SecurityActivity}
 * theo ràng buộc 4.1.
 */
public class SecurityViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> pinEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> biometricEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> reminderEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> reminderTime = new MutableLiveData<>();
    private final MutableLiveData<String> pinError = new MutableLiveData<>();

    public SecurityViewModel(@NonNull Application app) {
        super(app);
        pinEnabled.setValue(PrefsHelper.isPinEnabled(app));
        biometricEnabled.setValue(PrefsHelper.isBiometricEnabled(app));
        reminderEnabled.setValue(PrefsHelper.isReminderEnabled(app));
        reminderTime.setValue(formatReminderTime(
                PrefsHelper.getReminderHour(app), PrefsHelper.getReminderMinute(app)));
    }

    public LiveData<Boolean> pinEnabled() { return pinEnabled; }
    public LiveData<Boolean> biometricEnabled() { return biometricEnabled; }
    public LiveData<Boolean> reminderEnabled() { return reminderEnabled; }
    public LiveData<String> reminderTime() { return reminderTime; }
    public LiveData<String> pinError() { return pinError; }

    /**
     * Đặt PIN mới — yêu cầu gọi với PIN cũ nếu đã có.
     */
    public void setPin(String oldPin, String newPin) {
        Application app = getApplication();
        if (newPin == null || newPin.length() < 4) {
            pinError.setValue("PIN phải có ít nhất 4 số");
            return;
        }
        if (PrefsHelper.isPinEnabled(app) && !PrefsHelper.verifyPin(app, oldPin)) {
            pinError.setValue("PIN cũ không đúng");
            return;
        }
        String hash = PrefsHelper.hashPin(app, newPin);
        PrefsHelper.setPinEnabled(app, true, hash);
        pinError.setValue(null);
        pinEnabled.setValue(true);
    }

    public void disablePin(String oldPin) {
        Application app = getApplication();
        if (PrefsHelper.isPinEnabled(app) && !PrefsHelper.verifyPin(app, oldPin)) {
            pinError.setValue("PIN không đúng");
            return;
        }
        PrefsHelper.disablePin(app);
        PrefsHelper.setBiometricEnabled(app, false);
        pinEnabled.setValue(false);
        biometricEnabled.setValue(false);
    }

    public void setBiometricEnabled(boolean enabled) {
        Application app = getApplication();
        if (enabled && !PrefsHelper.isPinEnabled(app)) return;
        PrefsHelper.setBiometricEnabled(app, enabled);
        biometricEnabled.setValue(enabled);
    }

    public void setReminderEnabled(boolean enabled) {
        PrefsHelper.setReminderEnabled(getApplication(), enabled);
        reminderEnabled.setValue(enabled);
    }

    public void setReminderTime(String time) {
        int[] parsed = parseReminderTime(time);
        PrefsHelper.setReminderTime(getApplication(), parsed[0], parsed[1]);
        reminderTime.setValue(time);
    }

    private static String formatReminderTime(int hour, int minute) {
        return String.format(java.util.Locale.US, "%02d:%02d", hour, minute);
    }

    private static int[] parseReminderTime(String time) {
        if (time == null) return new int[]{21, 0};
        String[] parts = time.trim().split(":");
        if (parts.length != 2) return new int[]{21, 0};
        try {
            int hour = Math.max(0, Math.min(23, Integer.parseInt(parts[0])));
            int minute = Math.max(0, Math.min(59, Integer.parseInt(parts[1])));
            return new int[]{hour, minute};
        } catch (NumberFormatException e) {
            return new int[]{21, 0};
        }
    }
}
