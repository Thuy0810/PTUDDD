package com.expensemanager.app.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.RecurringRepository;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager Worker chạy định kỳ để thực thi các giao dịch định kỳ đến hạn.
 *
 * <p>Dùng {@code OneTimeWorkRequest} với {@code ExistingPeriodicWorkPolicy.REPLACE}
 * để đảm bảo worker mới nhất luôn được dùng.
 *
 * <p>Thực tế WorkManager có thể chạy worker muộn hơn schedule tối thiểu 15 phút.
 * Với giao dịch định kỳ (hàng ngày/tuần/tháng), độ trễ này chấp nhận được.
 */
public class RecurringTransactionWorker extends Worker {
    private static final String TAG = "RecurringWorker";
    public static final String WORK_NAME = "recurring_transaction_worker";

    private final RecurringRepository recurringRepo = new RecurringRepository();

    public RecurringTransactionWorker(@NonNull Context context,
                                     @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String uid = new AuthRepository().getUid();
        if (uid == null) {
            Log.w(TAG, "No uid, skip");
            return Result.success();
        }

        Log.d(TAG, "Running recurring transaction check");
        recurringRepo.catchUp(uid);
        return Result.success();
    }

    /**
     * Lên lịch worker chạy định kỳ.
     * Gọi 1 lần khi app khởi động.
     */
    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                RecurringTransactionWorker.class,
                15, TimeUnit.MINUTES) // Minimum for PeriodicWorkRequest
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        request);

        Log.d(TAG, "Scheduled recurring worker");
    }

    /**
     * Hủy worker.
     */
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Cancelled recurring worker");
    }
}
