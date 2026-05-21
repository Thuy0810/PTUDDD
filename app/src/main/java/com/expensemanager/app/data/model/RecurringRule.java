package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

public class RecurringRule {
    @DocumentId
    private String id;
    private String type;
    private double amount;
    private String categoryId;
    private String walletId;
    private String note;
    private int dayOfMonth;
    private boolean enabled;
    private Timestamp nextRun;

    public RecurringRule() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Timestamp getNextRun() { return nextRun; }
    public void setNextRun(Timestamp nextRun) { this.nextRun = nextRun; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("amount", amount);
        map.put("categoryId", categoryId);
        map.put("walletId", walletId);
        map.put("note", note != null ? note : "");
        map.put("dayOfMonth", dayOfMonth);
        map.put("enabled", enabled);
        if (nextRun != null) map.put("nextRun", nextRun);
        return map;
    }
}
