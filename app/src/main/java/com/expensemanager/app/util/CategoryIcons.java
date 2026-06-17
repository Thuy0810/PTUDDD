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
        MAP.put("food", R.drawable.ic_cat_coffee);
        MAP.put("transport", R.drawable.ic_cat_car);
        MAP.put("shopping", R.drawable.ic_cat_bag);
        MAP.put("bills", R.drawable.ic_cat_bolt);
        MAP.put("education", R.drawable.ic_cat_laptop);
        MAP.put("entertainment", R.drawable.ic_cat_film);
        MAP.put("fun", R.drawable.ic_cat_film);
        MAP.put("health", R.drawable.ic_cat_heart);
        MAP.put("family", R.drawable.ic_user_stroke);
        MAP.put("saving", R.drawable.ic_target_ring);
        MAP.put("other", R.drawable.ic_note);
        MAP.put("salary", R.drawable.ic_cat_briefcase);
        MAP.put("income", R.drawable.ic_cat_briefcase);
        MAP.put("bonus", R.drawable.ic_trophy);
        MAP.put("gift", R.drawable.ic_cat_gift);
        MAP.put("sales", R.drawable.ic_creditcard);
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
                ? R.drawable.ic_cat_briefcase : R.drawable.ic_note;
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
