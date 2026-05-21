package com.expensemanager.app.data.model;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {
    private String displayName;
    private String email;
    private String currency;
    private String theme;
    private boolean notificationsEnabled;
    private boolean pinEnabled;

    public UserProfile() {
        currency = "VND";
        theme = "system";
        notificationsEnabled = true;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isPinEnabled() { return pinEnabled; }
    public void setPinEnabled(boolean pinEnabled) { this.pinEnabled = pinEnabled; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("displayName", displayName);
        map.put("email", email);
        map.put("currency", currency);
        map.put("theme", theme);
        map.put("notificationsEnabled", notificationsEnabled);
        map.put("pinEnabled", pinEnabled);
        return map;
    }
}
