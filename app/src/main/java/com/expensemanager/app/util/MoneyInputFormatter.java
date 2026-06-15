package com.expensemanager.app.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Tiện ích nhập số tiền vnd — tự động thêm dấu phẩy khi nhập.
 *
 * <p>Dùng {@link #attach(EditText)} để gắn formatter vào ô nhập liệu.
 * Dùng {@link #getRawValue(EditText)} để lấy giá trị {@code long}.
 *
 * <p>Quy tắc:
 * <ul>
 *   <li>Chỉ giữ chữ số.</li>
 *   <li>Thêm dấu phẩy sau mỗi 3 chữ số.</li>
 *   <li>Giữ đúng vị trí con trỏ.</li>
 *   <li>Không crash khi rỗng.</li>
 *   <li>Parse kết quả ra {@code long}.</li>
 * </ul>
 */
public final class MoneyInputFormatter {

    private MoneyInputFormatter() {}

    /** Gắn TextWatcher auto-format vào EditText. */
    public static void attach(EditText editText) {
        editText.addTextChangedListener(new MoneyTextWatcher(editText));
    }

    /**
     * Lấy giá trị {@code long} từ EditText đã được format.
     * Trả về 0 nếu không hợp lệ hoặc rỗng.
     */
    public static long getRawValue(EditText editText) {
        if (editText == null) return 0L;
        return parse(editText.getText().toString());
    }

    /**
     * Format một giá trị {@code long} thành chuỗi hiển thị.
     * Ví dụ: {@code 500000 -> "500,000"}
     */
    public static String format(long value) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            symbols.setGroupingSeparator(',');
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(true);
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);
        return df.format(value);
    }

    /**
     * Parse chuỗi đã format sang {@code long}.
     * Bỏ dấu phẩy, khoảng trắng, ký hiệu tiền tệ.
     */
    public static long parse(String input) {
        if (input == null) return 0L;
        String s = input.trim()
                .replace(".", "")
                .replace(",", "")
                .replace(" ", "")
                .replace("đ", "")
                .replace("Đ", "")
                .replace("VND", "")
                .replace("$", "")
                .replace("€", "")
                .replace("¥", "");
        if (s.isEmpty()) return 0L;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Format một giá trị {@code long} thành chuỗi hiển thị vnd.
     * Ví dụ: {@code 500000 -> "500,000 vnd"}
     */
    public static String formatWithCurrency(long value) {
        return format(value) + " vnd";
    }

    private static class MoneyTextWatcher implements TextWatcher {
        private final EditText editText;
        private boolean isFormatting;

        MoneyTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (isFormatting) return;
            isFormatting = true;

            try {
                String digits = s.toString().replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    long value = Long.parseLong(digits);
                    String formatted = format(value);
                    if (!formatted.equals(s.toString())) {
                        int selStart = editText.getSelectionStart();
                        int selEnd = editText.getSelectionEnd();

                        editText.setText(formatted);

                        // Giữ vị trí con trỏ gần điểm nhập hiện tại.
                        int diff = formatted.length() - s.length();
                        int newPos = Math.min(selStart + diff,
                                Math.max(0, formatted.length()));
                        newPos = Math.min(newPos, formatted.length());
                        if (newPos >= 0) {
                            editText.setSelection(newPos);
                        }
                    }
                } else {
                    editText.setText("");
                }
            } catch (Exception e) {
                // Ignore formatting errors
            } finally {
                isFormatting = false;
            }
        }
    }
}
