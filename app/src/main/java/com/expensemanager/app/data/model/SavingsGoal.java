package com.expensemanager.app.data.model;

import com.expensemanager.app.util.DateUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SavingsGoal {
    @DocumentId
    private String id;
    private String title;
    private long targetAmount;
    private long savedAmount;
    private String walletId;
    private boolean completed;
    private Timestamp deadline;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean isArchived;

    public SavingsGoal() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getTargetAmount() { return targetAmount; }
    public void setTargetAmount(long targetAmount) { this.targetAmount = targetAmount; }

    public long getSavedAmount() { return savedAmount; }
    public void setSavedAmount(long savedAmount) { this.savedAmount = savedAmount; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Timestamp getDeadline() { return deadline; }
    public void setDeadline(Timestamp deadline) { this.deadline = deadline; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public float getProgress() {
        if (targetAmount <= 0L) return 0f;
        return (float) Math.min(1.0, (double) savedAmount / targetAmount);
    }

    /**
     * Kiểm tra quá hạn theo múi giờ ICT.
     */
    public boolean isOverdue() {
        if (deadline == null || completed) return false;
        return DateUtils.nowVietnam().after(deadline.toDate());
    }

    public long getRemainingDays() {
        if (deadline == null) return -1L;
        Date now = DateUtils.nowVietnam();
        long diff = deadline.toDate().getTime() - now.getTime();
        return diff / (24L * 3600L * 1000L);
    }

    public long getMonthlyRequired() {
        if (deadline == null || completed) return 0L;
        long daysLeft = getRemainingDays();
        if (daysLeft <= 0) return targetAmount - savedAmount;
        long monthsLeft = Math.max(1L, daysLeft / 30L);
        long remaining = targetAmount - savedAmount;
        if (remaining <= 0L) return 0L;
        return remaining / monthsLeft;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("targetAmount", targetAmount);
        map.put("savedAmount", savedAmount);
        map.put("walletId", walletId);
        map.put("completed", completed);
        if (deadline != null) map.put("deadline", deadline);
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        map.put("updatedAt", updatedAt != null ? updatedAt : Timestamp.now());
        map.put("isArchived", isArchived);
        return map;
    }
}
