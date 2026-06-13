package com.expensemanager.app.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class MoneyFormat {
    private static volatile String currencySymbol = "đ";
    private static volatile boolean symbolBefore = true;
    private static volatile int decimals = 0;
    private static volatile Locale locale = new Locale("vi", "VN");
    private static final ConcurrentHashMap<Locale, DecimalFormat> formatCache = new ConcurrentHashMap<>();

    private MoneyFormat() {}

    public static void applySettings(String symbol, boolean before, int decimalPlaces, Locale loc) {
        currencySymbol = symbol != null ? symbol : "đ";
        symbolBefore = before;
        decimals = Math.max(0, decimalPlaces);
        locale = loc != null ? loc : new Locale("vi", "VN");
        formatCache.clear();
    }

    public static void reset() {
        applySettings("đ", true, 0, new Locale("vi", "VN"));
    }

    private static DecimalFormat getFormat() {
        DecimalFormat df = formatCache.get(locale);
        if (df == null) {
            df = (DecimalFormat) NumberFormat.getInstance(locale);
            DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
            symbols.setDecimalSeparator(',');
            symbols.setGroupingSeparator('.');
            df.setDecimalFormatSymbols(symbols);
            df.setMaximumFractionDigits(decimals);
            df.setMinimumFractionDigits(0);
            df.setGroupingUsed(true);
            formatCache.put(locale, df);
        }
        return df;
    }

    public static String format(double amount) {
        String number = getFormat().format(amount);
        if (symbolBefore) {
            return currencySymbol + " " + number;
        } else {
            return number + " " + currencySymbol;
        }
    }

    public static String formatCompact(double amount) {
        String number;
        if (amount >= 1_000_000) {
            number = String.format(locale, "%.1fM", amount / 1_000_000);
        } else if (amount >= 1_000) {
            number = String.format(locale, "%.1fK", amount / 1_000);
        } else {
            number = getFormat().format(amount);
        }
        if (symbolBefore) {
            return currencySymbol + " " + number;
        } else {
            return number + " " + currencySymbol;
        }
    }
}
