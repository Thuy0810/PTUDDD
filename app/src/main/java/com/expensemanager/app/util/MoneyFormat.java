package com.expensemanager.app.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class MoneyFormat {
    /** Mã tiền tệ hiển thị sau số tiền. */
    private static volatile String currencyCode = "vnd";
    /** Vị trí ký hiệu tiền: true = trước số, false = sau số. Mặc định false (sau). */
    private static volatile boolean symbolBefore = false;
    private static volatile int decimals = 0;
    private static volatile Locale locale = new Locale("vi", "VN");
    private static final ConcurrentHashMap<Locale, DecimalFormat> formatCache = new ConcurrentHashMap<>();

    private MoneyFormat() {}

    /**
     * Áp dụng cài đặt định dạng tiền tệ.
     *
     * @param code    mã tiền tệ (VND, USD, EUR...)
     * @param before  ký hiệu đứng trước số tiền
     * @param dec    số chữ số thập phân
     * @param loc    locale cho định dạng số
     */
    public static void applySettings(String code, boolean before, int dec, Locale loc) {
        currencyCode = normalizeCurrencyCode(code);
        symbolBefore = false;
        decimals = Math.max(0, dec);
        locale = loc != null ? loc : new Locale("vi", "VN");
        formatCache.clear();
    }

    public static void reset() {
        applySettings("vnd", false, 0, new Locale("vi", "VN"));
    }

    private static DecimalFormat getFormat() {
        DecimalFormat df = formatCache.get(locale);
        if (df == null) {
            df = (DecimalFormat) NumberFormat.getInstance(locale);
            DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            symbols.setGroupingSeparator(',');
            df.setDecimalFormatSymbols(symbols);
            df.setMaximumFractionDigits(decimals);
            df.setMinimumFractionDigits(0);
            df.setGroupingUsed(true);
            formatCache.put(locale, df);
        }
        return df;
    }

    /**
     * @deprecated Dùng {@link #format(long)} thay vì cast qua double.
     */
    @Deprecated
    public static String format(double amount) {
        return formatLong((long) Math.round(amount));
    }

    /**
     * Format số tiền. Mặc định: {@code 500000 -> "500,000 vnd"}.
     *
     * <p>Ký hiệu luôn đứng SAU số tiền. Muốn đứng trước dùng
     * {@link #applySettings(String, boolean, int, Locale)}.
     */
    public static String format(long amount) {
        String number = getFormat().format(amount);
        if (symbolBefore) {
            return currencyCode + " " + number;
        } else {
            return number + " " + currencyCode;
        }
    }

    /** Alias cho {@link #format(long)}. */
    public static String formatLong(long amount) {
        return format(amount);
    }

    /**
     * Format số tiền có dấu cộng/trừ cho thu/chi.
     *
     * <p>Chuyển tiền (TRANSFER) trả về số không dấu.
     *
     * @param amount số tiền
     * @param type   {@code "income"}, {@code "expense"}, {@code "transfer"}
     */
    public static String formatSigned(long amount, String type) {
        String base = format(amount);
        if ("income".equals(type)) {
            return "+" + base;
        } else if ("expense".equals(type)) {
            return "-" + base;
        }
        return base;
    }

    public static String formatCompact(long amount) {
        String number;
        if (amount >= 1_000_000) {
            number = String.format(locale, "%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            number = String.format(locale, "%.1fK", amount / 1_000.0);
        } else {
            number = getFormat().format(amount);
        }
        if (symbolBefore) {
            return currencyCode + " " + number;
        } else {
            return number + " " + currencyCode;
        }
    }

    /** @deprecated Dùng {@link #formatCompact(long)}. */
    @Deprecated
    public static String formatCompact(double amount) {
        return formatCompact((long) Math.round(amount));
    }

    private static String normalizeCurrencyCode(String code) {
        if (code == null || code.trim().isEmpty()) return "vnd";
        String normalized = code.trim();
        if ("đ".equals(normalized) || "₫".equals(normalized)) return "vnd";
        return normalized.toLowerCase(Locale.ROOT);
    }
}
