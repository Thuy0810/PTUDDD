package com.expensemanager.app.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.expensemanager.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Bộ chọn icon cho danh mục — một hàng icon cuộn ngang, chọn được.
 * Dùng chung cho dialog thêm/sửa danh mục.
 *
 * <p>Cách dùng:
 * <pre>
 *   final String[] holder = { category.getIconKey() };
 *   View picker = CategoryIconPicker.createScroller(ctx, holder);
 *   container.addView(picker);
 *   // khi lưu: category.setIconKey(holder[0]);
 * </pre>
 */
public final class CategoryIconPicker {

    private CategoryIconPicker() {}

    public interface OnIconSelected { void onSelected(String key); }

    /** Tạo sẵn một {@link HorizontalScrollView} chứa các icon, có thể add thẳng vào dialog. */
    public static View createScroller(Context ctx, String[] selectedHolder) {
        HorizontalScrollView scroll = new HorizontalScrollView(ctx);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(row);
        populate(ctx, row, selectedHolder, null);
        return scroll;
    }

    /** Đổ các icon vào {@code container} (LinearLayout ngang) và xử lý chọn. */
    public static void populate(Context ctx, LinearLayout container,
                                String[] selectedHolder, OnIconSelected onChange) {
        container.removeAllViews();
        float d = ctx.getResources().getDisplayMetrics().density;
        int size = (int) (44 * d);
        int gap = (int) (8 * d);
        int pad = (int) (10 * d);
        final List<ImageView> chips = new ArrayList<>();

        if (selectedHolder[0] == null) selectedHolder[0] = CategoryIcons.PICKER_KEYS[0];

        for (String key : CategoryIcons.PICKER_KEYS) {
            ImageView iv = new ImageView(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(0, gap, gap, gap);
            iv.setLayoutParams(lp);
            iv.setPadding(pad, pad, pad, pad);
            iv.setImageResource(CategoryIcons.drawableFor(key));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setTag(key);
            iv.setOnClickListener(v -> {
                selectedHolder[0] = (String) v.getTag();
                refresh(ctx, chips, selectedHolder[0]);
                if (onChange != null) onChange.onSelected(selectedHolder[0]);
            });
            container.addView(iv);
            chips.add(iv);
        }
        refresh(ctx, chips, selectedHolder[0]);
    }

    private static void refresh(Context ctx, List<ImageView> chips, String selectedKey) {
        for (ImageView iv : chips) {
            boolean selected = iv.getTag() != null && iv.getTag().equals(selectedKey);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            if (selected) {
                bg.setColor(ctx.getColor(R.color.saving_blue));
                iv.setColorFilter(Color.WHITE);
            } else {
                bg.setColor(0x22000000);
                iv.setColorFilter(ctx.getColor(R.color.text_secondary));
            }
            iv.setBackground(bg);
        }
    }
}
