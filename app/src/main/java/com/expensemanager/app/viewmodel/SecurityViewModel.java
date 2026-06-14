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
        PrefsHelper prefs = new PrefsHelper(app);
        pinEnabled.setValue(prefs.isPinEnabled());
        biometricEnabled.setValue(prefs.isBiometricEnabled());
        reminderEnabled.setValue(prefs.isReminderEnabled());
        reminderTime.setValue(prefs.getReminderTime());
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
        PrefsHelper prefs = new PrefsHelper(getApplication());
        if (newPin == null || newPin.length() < 4) {
            pinError.setValue("PIN phải có ít nhất 4 số");
            return;
        }
        if (prefs.isPinEnabled() && !prefs.verifyPin(oldPin)) {
            pinError.setValue("PIN cũ không đúng");
            return;
        }
        prefs.setPin(newPin);
        prefs.setPinEnabled(true);
        pinError.setValue(null);
        pinEnabled.setValue(true);
    }

    public void disablePin(String oldPin) {
        PrefsHelper prefs = new PrefsHelper(getApplication());
        if (prefs.isPinEnabled() && !prefs.verifyPin(oldPin)) {
            pinError.setValue("PIN không đúng");
            return;
        }
        prefs.setPinEnabled(false);
        prefs.setBiometricEnabled(false);
        pinEnabled.setValue(false);
        biometricEnabled.setValue(false);
    }

    public void setBiometricEnabled(boolean enabled) {
        PrefsHelper prefs = new PrefsHelper(getApplication());
        if (enabled && !prefs.isPinEnabled()) return;
        prefs.setBiometricEnabled(enabled);
        biometricEnabled.setValue(enabled);
    }

    public void setReminderEnabled(boolean enabled) {
        PrefsHelper prefs = new PrefsHelper(getApplication());
        prefs.setReminderEnabled(enabled);
        reminderEnabled.setValue(enabled);
    }

    public void setReminderTime(String time) {
        PrefsHelper prefs = new PrefsHelper(getApplication());
        prefs.setReminderTime(time);
        reminderTime.setValue(time);
    }
}
