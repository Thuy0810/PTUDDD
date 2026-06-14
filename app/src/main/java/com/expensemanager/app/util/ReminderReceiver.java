package com.expensemanager.app.util;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.expensemanager.app.ExpenseApplication;
import com.expensemanager.app.R;

/**
 * Nhận broadcast từ AlarmManager để hiển thị notification nhắc nhở hàng ngày.
 *
 * <p>Notification cố định — không truy vấn Firestore trong BroadcastReceiver
 * (chạy trên main thread, có thể bị system kill).
 *
 * <p>Giao dịch định kỳ được tạo tự động khi user đăng nhập
 * qua {@code RecurringRepository.catchUp()} — không cần notification.
 */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, Intent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ExpenseApplication.CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_reminder_title))
                .setContentText(context.getString(R.string.notification_reminder_body))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notification_reminder_body_long)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(1001, builder.build());
        }
    }
}
