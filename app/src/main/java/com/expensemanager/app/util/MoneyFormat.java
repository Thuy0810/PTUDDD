package com.expensemanager.app.util;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class MoneyFormat {
    private static String currencySymbol = "đ";
    private static NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final ConcurrentHashMap<String, NumberFormat> formatCache = new ConcurrentHashMap<>();

    private MoneyFormat() {}

    public static void setCurrency(String symbol) {
        currencySymbol = symbol != null ? symbol : "đ";
    }

    public static void setLocale(String languageTag) {
        Locale locale;
        if (languageTag == null || languageTag.isEmpty()) {
            locale = new Locale("vi", "VN");
        } else {
            String[] parts = languageTag.split("[-_]");
            if (parts.length >= 2) {
                locale = new Locale(parts[0], parts[1]);
            } else {
                locale = new Locale(languageTag);
            }
        }
        format = NumberFormat.getInstance(locale);
    }

    public static String format(double amount) {
        return format.format(amount) + " " + currencySymbol;
    }

    public static String formatCompact(double amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", amount / 1_000_000) + " " + currencySymbol;
        } else if (amount >= 1_000) {
            return String.format(Locale.US, "%.1fK", amount / 1_000) + " " + currencySymbol;
        }
        return format.format(amount) + " " + currencySymbol;
    }

    public static void reset() {
        currencySymbol = "đ";
        format = NumberFormat.getInstance(new Locale("vi", "VN"));
    }
}
