package com.expensemanager.app.util;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuickParseUtil {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(k|K|nghìn|ngàn|trieu|tr|t|đ|d)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{1,2})[/.-](\\d{1,2})(?:[/.-](\\d{2,4}))?",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, Pattern> RELATIVE_DATE_PATTERNS = new HashMap<>();

    static {
        RELATIVE_DATE_PATTERNS.put("hom nay", Pattern.compile(
                "hom\\s*nay", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("hom qua", Pattern.compile(
                "hom\\s*qua", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("hom kia", Pattern.compile(
                "hom\\s*kia", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("tuan nay", Pattern.compile(
                "tuan\\s*nay", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("tuan truoc", Pattern.compile(
                "tuan\\s*truoc", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thang truoc", Pattern.compile(
                "thang\\s*truoc", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thang nay", Pattern.compile(
                "thang\\s*nay", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thang sau", Pattern.compile(
                "thang\\s*sau", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("ngay mai", Pattern.compile(
                "ngay\\s*mai", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thu hai", Pattern.compile(
                "thứ?\\s*hai", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thu ba", Pattern.compile(
                "thứ?\\s*ba", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thu tu", Pattern.compile(
                "thứ?\\s*tư", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thu nam", Pattern.compile(
                "thứ?\\s*năm", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thu sau", Pattern.compile(
                "thứ?\\s*sáu", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thu bay", Pattern.compile(
                "thứ?\\s*bảy", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("cn", Pattern.compile(
                "chủ?\\s*nhật", Pattern.CASE_INSENSITIVE));
        RELATIVE_DATE_PATTERNS.put("thang ", Pattern.compile(
                "thang\\s+(\\d{1,2})", Pattern.CASE_INSENSITIVE));
    }

    public static class ParseResult {
        public final Double amount;
        public final String note;
        public final Date date;

        public ParseResult(Double amount, String note, Date date) {
            this.amount = amount;
            this.note = note;
            this.date = date;
        }
    }

    private QuickParseUtil() {}

    public static ParseResult parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ParseResult(null, null, null);
        }
        String trimmed = input.trim();

        Date date = parseRelativeDate(trimmed);
        String withoutDate = removeDateFromString(trimmed, date);
        Double amount = parseAmount(withoutDate);
        String note = buildNote(withoutDate, amount);

        return new ParseResult(amount, note, date);
    }

    private static String removeDateFromString(String input, Date date) {
        if (date == null) return input;
        String result = input;

        String[] relativeKeys = {"hom nay", "hom qua", "hom kia", "tuan nay",
                "tuan truoc", "thang truoc", "thang nay", "thang sau",
                "ngay mai", "thu hai", "thu ba", "thu tu", "thu nam",
                "thu sau", "thu bay", "cn"};
        for (String key : relativeKeys) {
            Pattern p = RELATIVE_DATE_PATTERNS.get(key);
            if (p != null && p.matcher(result).find()) {
                result = result.replaceAll("(?i)" + Pattern.quote(key), "").trim();
                break;
            }
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        boolean found = false;
        while (dateMatcher.find()) {
            int start = dateMatcher.start();
            boolean atStart = start == 0 || Character.isWhitespace(input.charAt(start - 1))
                    || input.charAt(start - 1) == ',' || input.charAt(start - 1) == '(';
            int end = dateMatcher.end();
            boolean atEnd = end == input.length() || Character.isWhitespace(input.charAt(end))
                    || input.charAt(end) == ',' || input.charAt(end) == ')' || input.charAt(end) == '.';
            if (atStart && atEnd) {
                dateMatcher.appendReplacement(sb, "");
                found = true;
            } else {
                dateMatcher.appendReplacement(sb, dateMatcher.group());
            }
        }
        dateMatcher.appendTail(sb);
        if (found) return sb.toString().trim();
        return result;
    }

    private static Double parseAmount(String input) {
        Matcher m = AMOUNT_PATTERN.matcher(input);
        if (!m.find()) return null;

        double amount = Double.parseDouble(m.group(1).replace(',', '.'));
        String unit = m.group(2);
        if (unit != null) {
            String u = unit.toLowerCase();
            if (u.equals("k") || u.equals("nghìn") || u.equals("ngàn")) {
                amount *= 1_000;
            } else if (u.equals("trieu") || u.equals("t") || u.equals("tr")) {
                amount *= 1_000_000;
            }
        }
        return amount;
    }

    private static Date parseRelativeDate(String input) {
        for (Map.Entry<String, Pattern> e : RELATIVE_DATE_PATTERNS.entrySet()) {
            if (e.getValue().matcher(input).find()) {
                if ("thang ".equals(e.getKey())) {
                    return parseMonthFromInput(input, e.getValue());
                }
                return resolveRelativeDate(e.getKey());
            }
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(input);
        if (dateMatcher.find()) {
            return parseExplicitDate(dateMatcher);
        }

        return null;
    }

    private static Date parseMonthFromInput(String input, Pattern p) {
        Matcher m = p.matcher(input);
        if (m.find()) {
            try {
                int month = Integer.parseInt(m.group(1));
                if (month >= 1 && month <= 12) {
                    Calendar cal = DateUtils.newCalendar();
                    cal.set(Calendar.MONTH, month - 1);
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    return cal.getTime();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Date resolveRelativeDate(String key) {
        Calendar cal = DateUtils.newCalendar();

        switch (key) {
            case "hom nay":
                return cal.getTime();
            case "hom qua":
                cal.add(Calendar.DAY_OF_MONTH, -1);
                return cal.getTime();
            case "hom kia":
                cal.add(Calendar.DAY_OF_MONTH, -2);
                return cal.getTime();
            case "tuan nay":
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                int daysToMonday = (dayOfWeek == Calendar.SUNDAY) ? -6 : -(dayOfWeek - Calendar.MONDAY);
                cal.add(Calendar.DAY_OF_MONTH, daysToMonday);
                return cal.getTime();
            case "tuan truoc":
                int dOfW = cal.get(Calendar.DAY_OF_WEEK);
                int daysToPrevMonday = (dOfW == Calendar.SUNDAY) ? -6 : -(dOfW - Calendar.MONDAY);
                cal.add(Calendar.DAY_OF_MONTH, daysToPrevMonday - 7);
                return cal.getTime();
            case "thang truoc":
                cal.add(Calendar.MONTH, -1);
                return cal.getTime();
            case "thang nay":
                return cal.getTime();
            case "thang sau":
                cal.add(Calendar.MONTH, 1);
                return cal.getTime();
            case "ngay mai":
                cal.add(Calendar.DAY_OF_MONTH, 1);
                return cal.getTime();
            case "thu hai":
                return nextDayOfWeek(cal, Calendar.MONDAY);
            case "thu ba":
                return nextDayOfWeek(cal, Calendar.TUESDAY);
            case "thu tu":
                return nextDayOfWeek(cal, Calendar.WEDNESDAY);
            case "thu nam":
                return nextDayOfWeek(cal, Calendar.THURSDAY);
            case "thu sau":
                return nextDayOfWeek(cal, Calendar.FRIDAY);
            case "thu bay":
                return nextDayOfWeek(cal, Calendar.SATURDAY);
            case "cn":
                return nextDayOfWeek(cal, Calendar.SUNDAY);
            case "thang ":
                return null;
            default:
                return null;
        }
    }

    private static Date nextDayOfWeek(Calendar cal, int targetDayOfWeek) {
        Calendar c = (Calendar) cal.clone();
        int diff = targetDayOfWeek - c.get(Calendar.DAY_OF_WEEK);
        if (diff < 0) diff += 7;
        if (diff == 0) diff = 7;
        c.add(Calendar.DAY_OF_MONTH, diff);
        return c.getTime();
    }

    private static Date parseExplicitDate(Matcher m) {
        try {
            int day = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            String yearStr = m.group(3);
            int year;

            if (yearStr != null) {
                year = Integer.parseInt(yearStr);
                if (year < 100) year += 2000;
            } else {
                year = DateUtils.newCalendar().get(Calendar.YEAR);
            }

            Calendar cal = DateUtils.newCalendar();
            cal.set(year, month - 1, day, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildNote(String withoutDate, Double amount) {
        String result = withoutDate.trim();

        if (amount != null) {
            Matcher am = AMOUNT_PATTERN.matcher(result);
            String matched = am.find() ? am.group() : null;
            if (matched != null) {
                result = result.replace(matched, "").trim();
            }
        }

        if (result.isEmpty()) return withoutDate.trim();
        return result.trim();
    }

    public static String formatDate(Date date) {
        if (date == null) return "";
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        return sdf.format(date);
    }
}
