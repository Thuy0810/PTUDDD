package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

public class SavingsGoal {
    @DocumentId
    private String id;
    private String title;
    private double targetAmount;
    private double savedAmount;
    private String walletId;
    private boolean completed;
    private Timestamp deadline;

    public SavingsGoal() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Timestamp getDeadline() { return deadline; }
    public void setDeadline(Timestamp deadline) { this.deadline = deadline; }

    public float getProgress() {
        if (targetAmount <= 0) return 0;
        return (float) Math.min(1.0, savedAmount / targetAmount);
    }

    public boolean isOverdue() {
        if (deadline == null || completed) return false;
        return new java.util.Date().after(deadline.toDate());
    }

    public long getRemainingDays() {
        if (deadline == null) return -1;
        long diff = deadline.toDate().getTime() - new java.util.Date().getTime();
        return diff / (24 * 3600 * 1000);
    }

    public double getMonthlyRequired() {
        if (deadline == null || completed) return 0;
        long daysLeft = getRemainingDays();
        if (daysLeft <= 0) return targetAmount - savedAmount;
        long monthsLeft = Math.max(1, daysLeft / 30);
        return (targetAmount - savedAmount) / monthsLeft;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("targetAmount", targetAmount);
        map.put("savedAmount", savedAmount);
        map.put("walletId", walletId);
        map.put("completed", completed);
        if (deadline != null) map.put("deadline", deadline);
        return map;
    }
}
