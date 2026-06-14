package com.expensemanager.app.util;

import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tiện ích xử lý ngày tháng, dùng múi giờ cố định Asia/Ho_Chi_Minh (ICT, UTC+7).
 *
 * <p>Quy ước nghiệp vụ (ràng buộc 10):
 * <ul>
 *   <li>Hiển thị và lọc theo múi giờ ICT.</li>
 *   <li>Dữ liệu Firestore lưu UTC qua {@link Timestamp}.</li>
 *   <li>Khoảng lọc luôn dùng nửa mở: {@code [start, end)}.</li>
 *   <li>Không bao giờ dùng {@code 23:59:59.999} làm ranh giới cuối.</li>
 * </ul>
 */
public final class DateUtils {
    /** Múi giờ nghiệp vụ của ứng dụng. */
    public static final TimeZone VIETNAM = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    private static final SimpleDateFormat DISPLAY =
            new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
    private static final SimpleDateFormat MONTH_KEY =
            new SimpleDateFormat("yyyy-MM", new Locale("vi", "VN"));
    private static final SimpleDateFormat MONTH_LABEL =
            new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));

    static {
        DISPLAY.setTimeZone(VIETNAM);
        MONTH_KEY.setTimeZone(VIETNAM);
        MONTH_LABEL.setTimeZone(VIETNAM);
    }

    private DateUtils() {}

    /**
     * Trả về {@link Date} hiện tại theo ICT. Dùng method này thay cho {@code new Date()} khi
     * cần thời điểm hiện tại cho nghiệp vụ.
     */
    public static Date nowVietnam() {
        // new Date() luôn là UTC instant; Calendar sẽ chuyển sang ICT khi cần.
        return new Date();
    }

    public static String formatDisplay(Date date) {
        if (date == null) return "";
        return DISPLAY.format(date);
    }

    /**
     * Trả về khoá tháng {@code yyyy-MM} cho ngày bất kỳ theo ICT.
     */
    public static String monthKey(Date date) {
        if (date == null) return currentMonthKey();
        return MONTH_KEY.format(date);
    }

    /**
     * Khoá tháng hiện tại theo ICT.
     */
    public static String currentMonthKey() {
        return monthKey(nowVietnam());
    }

    /**
     * Khoá tháng kế tiếp theo ICT.
     */
    public static String nextMonthKey() {
        return nextMonthKey(currentMonthKey());
    }

    /**
     * Khoá tháng kế tiếp của {@code monthKey} cho trước.
     * <p>Ví dụ: {@code "2026-06" -> "2026-07"}, {@code "2026-12" -> "2027-01"}.
     */
    public static String nextMonthKey(String monthKey) {
        Date start = startOfMonth(monthKey);
        Calendar cal = newCalendar();
        cal.setTime(start);
        cal.add(Calendar.MONTH, 1);
        return MONTH_KEY.format(cal.getTime());
    }

    /**
     * Tháng trước của {@code monthKey} cho trước.
     */
    public static String previousMonthKey(String monthKey) {
        Date start = startOfMonth(monthKey);
        Calendar cal = newCalendar();
        cal.setTime(start);
        cal.add(Calendar.MONTH, -1);
        return MONTH_KEY.format(cal.getTime());
    }

    /**
     * Tạo {@link Calendar} với múi giờ ICT. Ưu tiên dùng thay cho {@code Calendar.getInstance()}.
     */
    public static Calendar newCalendar() {
        return Calendar.getInstance(VIETNAM);
    }

    /**
     * Đầu tháng theo ICT cho ngày bất kỳ — Timestamp UTC.
     */
    public static Timestamp startOfMonth(Date ref) {
        Calendar cal = newCalendar();
        cal.setTime(ref);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp(cal.getTime());
    }

    /**
     * Đầu tháng theo ICT cho khoá tháng {@code yyyy-MM}.
     *
     * @param monthKey định dạng {@code yyyy-MM}
     * @return {@link Date} 00:00:00 ICT của ngày 1
     * @throws IllegalArgumentException nếu monthKey sai định dạng
     */
    public static Date startOfMonth(String monthKey) {
        if (monthKey == null) throw new IllegalArgumentException("monthKey is null");
        String[] parts = monthKey.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("monthKey sai định dạng: " + monthKey);
        }
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]); // 1-12
        Calendar cal = newCalendar();
        cal.clear();
        cal.set(year, month - 1, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Đầu tháng kế tiếp theo ICT cho khoá tháng {@code yyyy-MM}.
     * Dùng làm ranh giới {@code endExclusive} khi truy vấn theo tháng.
     */
    public static Date startOfNextMonth(String monthKey) {
        Date start = startOfMonth(monthKey);
        Calendar cal = newCalendar();
        cal.setTime(start);
        cal.add(Calendar.MONTH, 1);
        return cal.getTime();
    }

    /**
     * Đầu ngày theo ICT cho một {@link Date} bất kỳ.
     */
    public static Date startOfDay(Date ref) {
        Calendar cal = newCalendar();
        cal.setTime(ref);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Đầu ngày hôm sau theo ICT — ranh giới {@code endExclusive} khi lọc theo ngày.
     */
    public static Date startOfNextDay(Date ref) {
        Calendar cal = newCalendar();
        cal.setTime(ref);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * So sánh 2 ngày có cùng ngày ICT hay không.
     */
    public static boolean isSameDay(Date a, Date b) {
        if (a == null || b == null) return false;
        Calendar ca = newCalendar();
        ca.setTime(a);
        Calendar cb = newCalendar();
        cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Số ngày trong tháng chứa {@code ref}.
     */
    public static int daysInMonth(Date ref) {
        Calendar cal = newCalendar();
        cal.setTime(ref);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Ngày trong tháng (1-31) của {@code ref} theo ICT.
     */
    public static int dayOfMonth(Date ref) {
        Calendar cal = newCalendar();
        cal.setTime(ref);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Tên tháng hiện tại theo ICT, định dạng &quot;Tháng 6 2026&quot;.
     */
    public static String currentMonthLabel() {
        return MONTH_LABEL.format(nowVietnam());
    }

    /**
     * Chuyển khoá tháng {@code yyyy-MM} sang nhãn hiển thị &quot;Tháng 6 2026&quot;.
     */
    public static String formatMonthYear(String monthKey) {
        if (monthKey == null || monthKey.isEmpty()) return "";
        try {
            Date start = startOfMonth(monthKey);
            return MONTH_LABEL.format(start);
        } catch (Exception e) {
            return monthKey;
        }
    }

    /**
     * Trả về số ngày còn lại trong tháng (bao gồm hôm nay) theo ICT.
     * Dùng cho insight & budget prediction.
     */
    public static int daysRemainingInMonth() {
        Date today = nowVietnam();
        Calendar cal = newCalendar();
        cal.setTime(today);
        int todayDay = cal.get(Calendar.DAY_OF_MONTH);
        int total = daysInMonth(today);
        return Math.max(0, total - todayDay + 1);
    }

    /**
     * Trả về Timestamp UTC của {@code date} dùng cho lưu Firestore.
     * {@code date} phải là thời điểm đã tính theo ICT.
     */
    public static Timestamp toTimestamp(Date date) {
        return date != null ? new Timestamp(date) : null;
    }

    /**
     * Định dạng ngày giờ: {@code dd/MM/yyyy • HH:mm}.
     * Sử dụng Locale hiện tại của thiết bị.
     */
    public static String formatDateTime(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault());
        sdf.setTimeZone(VIETNAM);
        return sdf.format(date);
    }

    /**
     * Trả về chuỗi nhóm ngày: "Hôm nay", "Hôm qua", hoặc "dd/MM/yyyy".
     * Locale-aware.
     */
    public static String getRelativeDate(Date date) {
        if (date == null) return "";
        Date today = nowVietnam();

        Date todayStart = startOfDay(today);
        Date yesterdayStart = startOfDay(new Date(today.getTime() - 24 * 60 * 60 * 1000));

        Calendar cal = newCalendar();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date d = cal.getTime();

        if (d.equals(todayStart)) {
            return "Hôm nay";
        } else if (d.equals(yesterdayStart)) {
            return "Hôm qua";
        }
        return formatDisplay(date);
    }
}
