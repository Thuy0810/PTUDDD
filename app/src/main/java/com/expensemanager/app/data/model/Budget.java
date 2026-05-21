package com.expensemanager.app.data.model;

import com.google.firebase.firestore.DocumentId;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Budget {
    public static final String SCOPE_MONTHLY = "monthlyTotal";
    public static final String SCOPE_CATEGORY = "category";

    @DocumentId
    private String id;
    private String scope;
    private String categoryId;
    private String month;
    private double limitAmount;
    private List<Double> alertAt;

    public Budget() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }

    public List<Double> getAlertAt() {
        return alertAt != null ? alertAt : Arrays.asList(0.8, 0.9);
    }

    public void setAlertAt(List<Double> alertAt) { this.alertAt = alertAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("scope", scope);
        if (categoryId != null) map.put("categoryId", categoryId);
        map.put("month", month);
        map.put("limitAmount", limitAmount);
        map.put("alertAt", getAlertAt());
        return map;
    }
}
