package com.expensemanager.app.util;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;

import com.expensemanager.app.R;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ánh xạ icon ví sang drawable vector (thư viện Iconics / Google Material).
 * - {@link #PICKER_KEYS} là danh sách khóa cho phép người dùng chọn.
 * - {@link #drawableFor(String)} trả drawable theo khóa icon đã chọn.
 * - {@link #defaultForType(String)} icon mặc định theo loại ví.
 */
public final class WalletIcons {

    private WalletIcons() {}

    /** Các khóa icon hiển thị trong bộ chọn (giữ thứ tự). */
    public static final String[] PICKER_KEYS = {
            "wallet", "card", "cash", "bank", "piggy",
            "coins", "briefcase", "mobile", "gift", "store"
    };

    private static final Map<String, Integer> MAP = new LinkedHashMap<>();
    static {
        MAP.put("wallet", R.drawable.ico_w_wallet);
        MAP.put("card", R.drawable.ico_w_card);
        MAP.put("cash", R.drawable.ico_w_cash);
        MAP.put("bank", R.drawable.ico_w_bank);
        MAP.put("piggy", R.drawable.ico_w_piggy);
        MAP.put("coins", R.drawable.ico_w_coins);
        MAP.put("briefcase", R.drawable.ico_w_briefcase);
        MAP.put("mobile", R.drawable.ico_w_mobile);
        MAP.put("gift", R.drawable.ico_w_gift);
        MAP.put("store", R.drawable.ico_w_store);
    }

    /** Icon mặc định theo loại ví (gồm cả khóa loại cũ). */
    private static final Map<String, String> TYPE_DEFAULT = new HashMap<>();
    static {
        TYPE_DEFAULT.put("payment", "wallet");
        TYPE_DEFAULT.put("debit", "card");
        // legacy
        TYPE_DEFAULT.put("cash", "cash");
        TYPE_DEFAULT.put("bank", "bank");
        TYPE_DEFAULT.put("ewallet", "mobile");
        TYPE_DEFAULT.put("savings", "piggy");
    }

    /** @return res-id drawable cho khóa icon, hoặc 0 nếu không có. */
    public static int drawableFor(String iconKey) {
        if (iconKey == null) return 0;
        Integer res = MAP.get(iconKey.trim().toLowerCase());
        return res != null ? res : 0;
    }

    /** Drawable mặc định theo loại ví. */
    public static int defaultForType(String type) {
        String key = type != null ? TYPE_DEFAULT.get(type) : null;
        Integer res = key != null ? MAP.get(key) : null;
        return res != null ? res : R.drawable.ico_w_wallet;
    }

    /**
     * Tô badge ví: vòng tròn nền (màu ví) + icon trắng.
     * @param iconView ImageView icon
     * @param bgView   View nền tròn
     * @param iconKey  khóa icon người dùng chọn (có thể null)
     * @param type     loại ví (dùng khi chưa chọn icon)
     * @param colorHex màu nền (hex, có thể null)
     */
    public static void apply(ImageView iconView, View bgView,
                             String iconKey, String type, String colorHex) {
        int res = drawableFor(iconKey);
        if (res == 0) res = defaultForType(type);
        iconView.setImageResource(res);
        iconView.setColorFilter(Color.WHITE);

        int color;
        try {
            color = Color.parseColor(colorHex != null ? colorHex : "#4A6CF7");
        } catch (Exception e) {
            color = Color.parseColor("#4A6CF7");
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        bgView.setBackground(bg);
    }
}
