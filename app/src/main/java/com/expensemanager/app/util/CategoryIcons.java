package com.expensemanager.app.util;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Ánh xạ iconKey của danh mục sang icon vector (kiểu nét Lucide) theo thiết kế.
 * Mỗi badge danh mục = vòng tròn nền mờ (màu danh mục) + icon nét cùng màu.
 */
public final class CategoryIcons {

    private CategoryIcons() {}

    private static final Map<String, Integer> MAP = new HashMap<>();
    static {
        // Icon từ thư viện Iconics (Google Material) — dùng dưới dạng drawable XML.
        MAP.put("food", R.drawable.ico_food);
        MAP.put("transport", R.drawable.ico_transport);
        MAP.put("shopping", R.drawable.ico_shopping);
        MAP.put("bills", R.drawable.ico_bills);
        MAP.put("education", R.drawable.ico_education);
        MAP.put("entertainment", R.drawable.ico_entertainment);
        MAP.put("fun", R.drawable.ico_entertainment);
        MAP.put("health", R.drawable.ico_health);
        MAP.put("family", R.drawable.ico_family);
        MAP.put("saving", R.drawable.ico_saving);
        MAP.put("other", R.drawable.ico_other);
        MAP.put("salary", R.drawable.ico_salary);
        MAP.put("income", R.drawable.ico_salary);
        MAP.put("bonus", R.drawable.ico_bonus);
        MAP.put("gift", R.drawable.ico_gift);
        MAP.put("sales", R.drawable.ico_sales);
        MAP.put("home", R.drawable.ico_home);
        MAP.put("rent", R.drawable.ico_home);
    }

    /** @return res-id icon vector cho iconKey, hoặc 0 nếu không có (vd: iconKey là emoji). */
    public static int drawableFor(String iconKey) {
        if (iconKey == null) return 0;
        Integer res = MAP.get(iconKey.trim().toLowerCase());
        return res != null ? res : 0;
    }

    /** Icon mặc định theo loại giao dịch khi danh mục không xác định. */
    public static int defaultFor(String type) {
        return Transaction.TYPE_INCOME.equals(type)
                ? R.drawable.ico_salary : R.drawable.ico_other;
    }

    /** iconKey có phải emoji không (không phải khóa chữ thường ascii đã biết). */
    public static boolean isEmoji(String iconKey) {
        if (TextUtils.isEmpty(iconKey)) return false;
        return drawableFor(iconKey) == 0 && iconKey.length() <= 4
                && !iconKey.matches("[a-zA-Z_]+");
    }

    /**
     * Tô badge danh mục: vòng tròn nền mờ + icon nét cùng màu.
     * @param iconView  ImageView hiển thị icon vector
     * @param bgView    View nền tròn (oval)
     * @param iconKey   khóa icon của danh mục
     * @param color     màu danh mục (đã parse)
     * @param fallbackType loại giao dịch để chọn icon mặc định
     */
    public static void apply(ImageView iconView, View bgView, String iconKey,
                             int color, String fallbackType) {
        int res = drawableFor(iconKey);
        if (res == 0) res = defaultFor(fallbackType);
        iconView.setImageResource(res);
        iconView.setColorFilter(color);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(softTint(color));
        bgView.setBackground(bg);
    }

    /** Trả về phiên bản mờ (~14% alpha) của màu để làm nền badge. */
    public static int softTint(int color) {
        return Color.argb(0x24, Color.red(color), Color.green(color), Color.blue(color));
    }
}
