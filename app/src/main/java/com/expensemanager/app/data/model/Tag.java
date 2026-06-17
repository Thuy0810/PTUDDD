package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

/**
 * Nhãn (tag) gắn cho giao dịch. Người dùng tự quản lý danh sách nhãn.
 * Một giao dịch có thể mang nhiều nhãn (lưu danh sách id trong Transaction.tagIds).
 */
public class Tag {
    @DocumentId
    private String id;
    private String name;
    private String colorHex;
    private Timestamp createdAt;

    public Tag() {}

    public Tag(String id, String name, String colorHex) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex != null ? colorHex : "#6B7280"; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name != null ? name : "");
        map.put("colorHex", getColorHex());
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        return map;
    }
}
