package com.expensemanager.app.util;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {
    private static final SimpleDateFormat DISPLAY = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final SimpleDateFormat MONTH_KEY = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    private DateUtils() {}

    public static String formatDisplay(Date date) {
        return DISPLAY.format(date);
    }

    public static String monthKey(Date date) {
        return MONTH_KEY.format(date);
    }

    public static String currentMonthKey() {
        return monthKey(new Date());
    }

    public static String nextMonthKey() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        return monthKey(cal.getTime());
    }

    public static Timestamp startOfMonth(Date ref) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(ref);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp(cal.getTime());
    }

    public static Timestamp endOfMonth(Date ref) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(ref);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return new Timestamp(cal.getTime());
    }

    public static boolean isSameDay(Date a, Date b) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(a);
        Calendar cb = Calendar.getInstance();
        cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    public static int daysInMonth(Date ref) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(ref);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static int dayOfMonth(Date ref) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(ref);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public static String currentMonthLabel() {
        SimpleDateFormat label = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        return label.format(new Date());
    }

    public static String formatMonthYear(String monthKey) {
        try {
            String[] parts = monthKey.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, 1);
            SimpleDateFormat label = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
            return label.format(cal.getTime());
        } catch (Exception e) {
            return monthKey;
        }
    }
}
