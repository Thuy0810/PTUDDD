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

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Timestamp getDeadline() { return deadline; }
    public void setDeadline(Timestamp deadline) { this.deadline = deadline; }

    public float getProgress() {
        if (targetAmount <= 0) return 0;
        return (float) Math.min(1.0, savedAmount / targetAmount);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("targetAmount", targetAmount);
        map.put("savedAmount", savedAmount);
        map.put("completed", completed);
        if (deadline != null) map.put("deadline", deadline);
        return map;
    }
}
