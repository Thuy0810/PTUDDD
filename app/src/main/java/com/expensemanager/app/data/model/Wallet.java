package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

public class Wallet {
    @DocumentId
    private String id;
    private String name;
    private String type;
    private double initialBalance;
    private double currentBalance;
    private String icon;
    private String color;
    private Timestamp createdAt;

    public Wallet() {}

    public Wallet(String id, String name, String type, double initialBalance) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.initialBalance = initialBalance;
        this.currentBalance = initialBalance;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getInitialBalance() { return initialBalance; }
    public void setInitialBalance(double initialBalance) { this.initialBalance = initialBalance; }

    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public void adjustBalance(double amount, boolean isIncome) {
        if (isIncome) {
            this.currentBalance += amount;
        } else {
            this.currentBalance -= amount;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public String getTypeLabel() {
        if (type == null) return "";
        switch (type) {
            case "cash": return "Tiền mặt";
            case "bank": return "Ngân hàng";
            case "ewallet": return "Ví điện tử";
            case "savings": return "Tiết kiệm";
            default: return type;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("type", type);
        map.put("initialBalance", initialBalance);
        map.put("currentBalance", currentBalance);
        if (icon != null) map.put("icon", icon);
        if (color != null) map.put("color", color);
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        return map;
    }
}
