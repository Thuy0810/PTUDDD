package com.expensemanager.app.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/**
 * Quản lý lịch nhắc nhở hàng ngày.
 *
 * <p>Ưu tiên dùng exact alarm nếu được phép (Android 12+), fallback về inexact alarm.
 */
public final class ReminderScheduler {
    private static final String TAG = "ReminderScheduler";

    private ReminderScheduler() {}

    public static void scheduleDaily(Context ctx) {
        if (!PrefsHelper.isReminderEnabled(ctx)) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int hour = PrefsHelper.getReminderHour(ctx);
        int minute = PrefsHelper.getReminderMinute(ctx);

        Calendar cal = DateUtils.newCalendar();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Calendar now = DateUtils.newCalendar();
        if (cal.before(now)) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        long triggerAtMs = cal.getTimeInMillis();

        boolean scheduled = false;

        // Android 12+ (API 31): kiểm tra quyền exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                try {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
                    scheduled = true;
                    Log.i(TAG, "Scheduled exact alarm at " + triggerAtMs);
                } catch (SecurityException e) {
                    Log.w(TAG, "Exact alarm denied, falling back to inexact", e);
                }
            } else {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM not granted, using inexact alarm");
            }
        }

        if (!scheduled) {
            // Inexact alarm: scheduleRepeating vẫn hoạt động không cần quyền
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMs,
                    AlarmManager.INTERVAL_DAY, pi);
            Log.i(TAG, "Scheduled inexact alarm at " + triggerAtMs);
        }
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}
