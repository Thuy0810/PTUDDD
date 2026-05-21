package com.expensemanager.app.data.model;

import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

public class Category {
    public static final String TYPE_INCOME = "income";
    public static final String TYPE_EXPENSE = "expense";

    @DocumentId
    private String id;
    private String name;
    private String type;
    private String iconKey;
    private String colorHex;
    private boolean isSystem;
    private String group; // "essential", "need", "want", "other"

    public Category() {}

    public Category(String id, String name, String type, String iconKey, String colorHex, boolean isSystem) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.iconKey = iconKey;
        this.colorHex = colorHex;
        this.isSystem = isSystem;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getTypeLabel() {
        return TYPE_INCOME.equals(type) ? "Thu nhập" : "Chi tiêu";
    }

    @Override
    public String toString() {
        return name;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("type", type);
        map.put("iconKey", iconKey);
        map.put("colorHex", colorHex);
        map.put("isSystem", isSystem);
        return map;
    }
}
