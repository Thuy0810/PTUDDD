package com.expensemanager.app.util;

import java.util.Calendar;
import java.util.Date;

/**
 * Tính khoảng thời gian cho báo cáo.
 *
 * <p>Dùng {@link DateUtils#VIETNAM} để thống nhất timezone ICT.
 */
public final class DateRangeUtils {

    public static class Range {
        public final Date start;
        public final Date end; // nửa mở [start, end)
        public final String label;

        public Range(Date start, Date end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }
    }

    private DateRangeUtils() {}

    public static Range thisMonth() {
        Date now = DateUtils.nowVietnam();
        return new Range(
                DateUtils.startOfMonth(now),
                DateUtils.startOfNextMonth(DateUtils.currentMonthKey()),
                DateUtils.currentMonthLabel());
    }

    public static Range lastMonth() {
        Calendar cal = DateUtils.newCalendar();
        cal.add(Calendar.MONTH, -1);
        Date startLast = cal.getTime();
        String lastKey = DateUtils.monthKey(startLast);
        return new Range(
                DateUtils.startOfMonth(lastKey),
                DateUtils.startOfNextMonth(lastKey),
                DateUtils.formatMonthYear(lastKey));
    }

    public static Range thisWeek() {
        Calendar cal = DateUtils.newCalendar();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        Date weekStart = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date weekEnd = cal.getTime();
        return new Range(DateUtils.startOfDay(weekStart), DateUtils.startOfDay(weekEnd), "Tuần này");
    }

    public static Range last7Days() {
        Calendar cal = DateUtils.newCalendar();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        Date start = DateUtils.startOfDay(cal.getTime());
        Date end = DateUtils.startOfNextDay(DateUtils.nowVietnam());
        return new Range(start, end, "7 ngày qua");
    }

    public static Range last30Days() {
        Calendar cal = DateUtils.newCalendar();
        cal.add(Calendar.DAY_OF_YEAR, -29);
        Date start = DateUtils.startOfDay(cal.getTime());
        Date end = DateUtils.startOfNextDay(DateUtils.nowVietnam());
        return new Range(start, end, "30 ngày qua");
    }

    public static Range yearToDate() {
        Calendar cal = DateUtils.newCalendar();
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date start = DateUtils.startOfDay(cal.getTime());
        Date end = DateUtils.startOfNextDay(DateUtils.nowVietnam());
        return new Range(start, end, "Từ đầu năm");
    }
}
