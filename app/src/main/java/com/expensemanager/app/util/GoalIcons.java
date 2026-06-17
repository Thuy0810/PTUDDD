package com.expensemanager.app.util;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import com.expensemanager.app.R;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ánh xạ icon mục tiêu tiết kiệm sang drawable vector (tái dùng bộ icon ví/danh mục).
 * - {@link #PICKER_KEYS} danh sách khóa cho phép người dùng chọn (giữ thứ tự).
 * - {@link #drawableFor(String)} trả drawable theo khóa, mặc định là heo đất.
 * - {@link #apply(ImageView, String)} tô icon trắng cho badge mục tiêu.
 */
public final class GoalIcons {

    private GoalIcons() {}

    /** Khóa icon mặc định khi mục tiêu chưa chọn icon. */
    public static final String DEFAULT_KEY = "piggy";

    /** Các khóa icon hiển thị trong bộ chọn (giữ thứ tự). */
    public static final String[] PICKER_KEYS = {
            "piggy", "car", "house", "travel", "education",
            "health", "family", "phone", "shopping", "gift",
            "coins", "briefcase", "store", "food", "fun"
    };

    private static final Map<String, Integer> MAP = new LinkedHashMap<>();
    static {
        MAP.put("piggy", R.drawable.ico_w_piggy);
        MAP.put("car", R.drawable.ico_transport);
        MAP.put("house", R.drawable.ico_home);
        MAP.put("travel", R.drawable.ico_w_store);   // tạm dùng (chưa có icon máy bay)
        MAP.put("education", R.drawable.ico_education);
        MAP.put("health", R.drawable.ico_health);
        MAP.put("family", R.drawable.ico_family);
        MAP.put("phone", R.drawable.ico_w_mobile);
        MAP.put("shopping", R.drawable.ico_shopping);
        MAP.put("gift", R.drawable.ico_gift);
        MAP.put("coins", R.drawable.ico_w_coins);
        MAP.put("briefcase", R.drawable.ico_w_briefcase);
        MAP.put("store", R.drawable.ico_w_store);
        MAP.put("food", R.drawable.ico_food);
        MAP.put("fun", R.drawable.ico_entertainment);
    }

    /** @return res-id drawable cho khóa icon, mặc định heo đất nếu không có. */
    public static int drawableFor(String iconKey) {
        if (iconKey == null) return R.drawable.ico_w_piggy;
        Integer res = MAP.get(iconKey.trim().toLowerCase());
        return res != null ? res : R.drawable.ico_w_piggy;
    }

    /** Tô icon mục tiêu màu trắng (badge nền tròn xanh giữ nguyên trong layout). */
    public static void apply(ImageView iconView, String iconKey) {
        iconView.setImageResource(drawableFor(iconKey));
        iconView.setColorFilter(Color.WHITE);
    }
}
