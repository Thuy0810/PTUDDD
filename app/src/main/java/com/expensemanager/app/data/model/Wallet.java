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
    private long initialBalance;
    private long currentBalance;
    private String icon;
    private String color;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean isArchived;

    public Wallet() {}

    public Wallet(String id, String name, String type, long initialBalance) {
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

    public long getInitialBalance() { return initialBalance; }
    public void setInitialBalance(long initialBalance) { this.initialBalance = initialBalance; }

    public long getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(long currentBalance) { this.currentBalance = currentBalance; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    /**
     * Cộng/trừ trực tiếp vào currentBalance — chỉ dùng trong bộ nhớ, không ghi Firestore.
     */
    public void adjustBalance(long amount, boolean isIncome) {
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
            case "payment": return "Ví thanh toán";
            case "debit": return "Thẻ ghi nợ";
            // legacy (dữ liệu cũ)
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
        map.put("updatedAt", updatedAt != null ? updatedAt : Timestamp.now());
        map.put("isArchived", isArchived);
        return map;
    }
}
