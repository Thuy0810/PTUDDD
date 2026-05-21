package com.expensemanager.app.util;

import java.util.HashMap;
import java.util.Map;

public final class CategorySuggester {
    private static final Map<String, String> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("trà sữa", "food");
        KEYWORDS.put("cafe", "food");
        KEYWORDS.put("cơm", "food");
        KEYWORDS.put("ăn", "food");
        KEYWORDS.put("xăng", "transport");
        KEYWORDS.put("xe", "transport");
        KEYWORDS.put("áo", "shopping");
        KEYWORDS.put("mạng", "bills");
        KEYWORDS.put("điện", "bills");
        KEYWORDS.put("thuốc", "health");
        KEYWORDS.put("lương", "salary");
        KEYWORDS.put("thưởng", "bonus");
    }

    private CategorySuggester() {}

    public static String suggestCategoryId(String note) {
        if (note == null) return null;
        String lower = note.toLowerCase();
        for (Map.Entry<String, String> e : KEYWORDS.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue();
        }
        return null;
    }
}
