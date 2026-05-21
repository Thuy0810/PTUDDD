package com.expensemanager.app.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuickParseUtil {
    private static final Pattern PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(k|K|nghìn|ngàn|đ|d)?", Pattern.CASE_INSENSITIVE);

    public static class ParseResult {
        public final Double amount;
        public final String note;

        public ParseResult(Double amount, String note) {
            this.amount = amount;
            this.note = note;
        }
    }

    private QuickParseUtil() {}

    public static ParseResult parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ParseResult(null, null);
        }
        String trimmed = input.trim();
        Matcher m = PATTERN.matcher(trimmed);
        if (!m.find()) {
            return new ParseResult(null, trimmed);
        }
        double amount = Double.parseDouble(m.group(1).replace(',', '.'));
        String unit = m.group(2);
        if (unit != null) {
            String u = unit.toLowerCase();
            if (u.equals("k") || u.equals("nghìn") || u.equals("ngàn")) {
                amount *= 1000;
            }
        }
        String note = trimmed.replace(m.group(), "").trim();
        if (note.isEmpty()) note = trimmed;
        return new ParseResult(amount, note);
    }
}
