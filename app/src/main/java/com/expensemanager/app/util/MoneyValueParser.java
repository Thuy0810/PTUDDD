package com.expensemanager.app.util;

import androidx.annotation.Nullable;

/**
 * Tiện ích chuyển đổi an toàn giữa các kiểu dữ liệu tiền tệ.
 *
 * Mục đích: hỗ trợ migration từ {@code double} sang {@code long} mà không làm crash app
 * khi Firestore trả về kiểu dữ liệu cũ hoặc không mong đợi.
 *
 * <p>Quy tắc chính:
 * <ul>
 *   <li>Tất cả số tiền VND nội bộ dùng {@code long} (ràng buộc 5.1).</li>
 *   <li>Đọc từ Firestore có thể nhận {@code Long}, {@code Integer}, {@code Double},
 *       {@code Float} hoặc {@code String} — tất cả phải được xử lý an toàn.</li>
 *   <li>Input người dùng từ EditText có thể chứa dấu phân cách hàng nghìn
 *       (&quot;1.000.000&quot; hoặc &quot;1,000,000&quot;) — phải được chuẩn hoá trước khi parse.</li>
 *   <li>VND không có phần thập phân, nên dấu chấm/phẩy luôn là grouping separator.</li>
 * </ul>
 */
public final class MoneyValueParser {

    private MoneyValueParser() {}

    /**
     * Chuyển đổi an toàn từ {@link Object} (Firestore snapshot) sang {@link Long}.
     *
     * @param value giá trị đọc được từ Firestore
     * @return {@link Long} tương ứng, hoặc {@code null} nếu không chuyển được
     */
    @Nullable
    public static Long toLong(@Nullable Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) {
            // Double, Float, Short, Byte đều implements Number
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return Math.round(d);
        }
        if (value instanceof String) return parseString((String) value);
        return null;
    }

    /**
     * Parse chuỗi người dùng nhập (EditText) sang {@code long}.
     *
     * <p>Chuẩn hoá: bỏ dấu phân cách hàng nghìn ({@code .} hoặc {@code ,}), bỏ khoảng trắng,
     * bỏ ký tự tiền tệ, bỏ dấu âm ở đầu (coi là không hợp lệ).
     *
     * @param input chuỗi đầu vào
     * @param defaultValue giá trị trả về nếu parse lỗi hoặc rỗng
     * @return số tiền dạng {@code long}
     */
    public static long tryParse(@Nullable String input, long defaultValue) {
        if (input == null) return defaultValue;
        String normalized = input.trim();
        if (normalized.isEmpty()) return defaultValue;

        // Bỏ tất cả dấu chấm, dấu phẩy, khoảng trắng
        normalized = normalized.replace(".", "").replace(",", "").replace(" ", "");
        // Bỏ ký tự tiền tệ phổ biến ở cuối/đầu
        if (normalized.endsWith("đ") || normalized.endsWith("Đ")
                || normalized.endsWith("d") || normalized.endsWith("D")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("đ") || normalized.startsWith("Đ")
                || normalized.startsWith("d") || normalized.startsWith("D")) {
            normalized = normalized.substring(1);
        }

        // Nếu là số âm → không hợp lệ cho giao dịch, trả default
        if (normalized.startsWith("-")) return defaultValue;

        try {
            long value = Long.parseLong(normalized);
            return value < 0 ? defaultValue : value;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parse chuỗi người dùng nhập, trả về {@code null} nếu không hợp lệ.
     *
     * <p>Hợp lệ khi: &gt; 0 và &le; {@link Long#MAX_VALUE}.
     */
    @Nullable
    public static Long tryParseStrict(@Nullable String input) {
        long value = tryParse(input, -1L);
        if (value <= 0) return null;
        if (value == Long.MAX_VALUE) return null;
        return value;
    }

    /**
     * Validate số tiền: phải &gt; 0 và &le; {@link Long#MAX_VALUE}.
     */
    public static boolean isValidAmount(long amount) {
        return amount > 0 && amount <= Long.MAX_VALUE;
    }

    /**
     * Chuyển {@link Long} có thể null sang {@code long} an toàn.
     */
    public static long orZero(@Nullable Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Cộng hai giá trị long có thể null, trả về 0 nếu cả 2 đều null.
     */
    public static long add(@Nullable Long a, @Nullable Long b) {
        return orZero(a) + orZero(b);
    }

    @Nullable
    private static Long parseString(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            // Thử parse thẳng trước
            return Long.parseLong(s.trim());
        } catch (NumberFormatException ignore) {
            // Nếu không được, thử bỏ dấu phân cách
            String normalized = s.trim().replace(".", "").replace(",", "").replace(" ", "");
            try {
                long v = Long.parseLong(normalized);
                return v < 0 ? null : v;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
