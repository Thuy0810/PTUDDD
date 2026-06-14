package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

public class Challenge {
    @DocumentId
    private String id;
    private String title;
    private String description;
    private long targetSavings;
    private int totalDays;
    private int completedDays;
    private Timestamp startDate;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean isArchived;

    public Challenge() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTargetSavings() { return targetSavings; }
    public void setTargetSavings(long targetSavings) { this.targetSavings = targetSavings; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public int getCompletedDays() { return completedDays; }
    public void setCompletedDays(int completedDays) { this.completedDays = completedDays; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public float getProgress() {
        if (totalDays <= 0) return 0f;
        return Math.min(1f, (float) completedDays / totalDays);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description != null ? description : "");
        map.put("targetSavings", targetSavings);
        map.put("totalDays", totalDays);
        map.put("completedDays", completedDays);
        map.put("startDate", startDate != null ? startDate : Timestamp.now());
        map.put("active", active);
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        map.put("updatedAt", updatedAt != null ? updatedAt : Timestamp.now());
        map.put("isArchived", isArchived);
        return map;
    }
}
