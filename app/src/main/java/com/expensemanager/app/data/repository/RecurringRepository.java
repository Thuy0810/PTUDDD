package com.expensemanager.app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.RecurringRule;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.util.DateUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository cho giao dịch định kỳ.
 *
 * <p>Đảm bảo:
 * <ul>
 *   <li>Idempotent execution qua {@code occurrenceId} — không tạo trùng dù nhiều process.</li>
 *   <li>Tính {@code nextRun} chính xác.</li>
 *   <li>Chạy bù (catch-up) đúng chu kỳ.</li>
 *   <li>Respect {@code dateStart} và {@code dateEnd}.</li>
 * </ul>
 */
public class RecurringRepository {
    private static final String TAG = "RecurringRepository";
    private static final int MAX_CATCH_UP_RUNS = 50;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TransactionRepository txRepo = new TransactionRepository();

    public LiveData<List<RecurringRule>> observeAll(String uid) {
        MutableLiveData<List<RecurringRule>> live = new MutableLiveData<>();
        db.collection("users").document(uid)
                .collection("recurring")
                .addSnapshotListener((snap, e) -> {
                    List<RecurringRule> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            RecurringRule r = doc.toObject(RecurringRule.class);
                            r.setId(doc.getId());
                            list.add(r);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    // === CRUD ===

    public void add(String uid, RecurringRule rule) {
        if (rule.getId() != null) {
            db.collection("users").document(uid)
                    .collection("recurring")
                    .document(rule.getId())
                    .set(rule.toMap());
        } else {
            db.collection("users").document(uid)
                    .collection("recurring")
                    .add(rule.toMap())
                    .addOnSuccessListener(doc -> rule.setId(doc.getId()));
        }
    }

    public void add(String uid, RecurringRule rule, OnSuccessListener<Void> onSuccess,
                    OnFailureListener onFailure) {
        if (rule.getId() != null) {
            db.collection("users").document(uid)
                    .collection("recurring")
                    .document(rule.getId())
                    .set(rule.toMap())
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure);
        } else {
            db.collection("users").document(uid)
                    .collection("recurring")
                    .add(rule.toMap())
                    .addOnSuccessListener(doc -> {
                        rule.setId(doc.getId());
                        onSuccess.onSuccess(null);
                    })
                    .addOnFailureListener(onFailure);
        }
    }

    public void update(String uid, RecurringRule rule) {
        if (rule.getId() == null) return;
        db.collection("users").document(uid)
                .collection("recurring")
                .document(rule.getId())
                .set(rule.toMap());
    }

    public void delete(String uid, String id) {
        db.collection("users").document(uid)
                .collection("recurring")
                .document(id)
                .delete();
    }

    // === Execution ===

    /**
     * Chạy bù tất cả rule đến hạn, bao gồm các kỳ bị bỏ lỡ trước đó.
     *
     * <p>Thực thi trong Firestore Transaction để đảm bảo không trùng.
     */
    public void catchUp(String uid) {
        db.collection("users").document(uid)
                .collection("recurring")
                .whereEqualTo("enabled", true)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) return;
                    AtomicInteger pending = new AtomicInteger(snap.size());
                    Date now = DateUtils.nowVietnam();

                    for (QueryDocumentSnapshot doc : snap) {
                        RecurringRule rule = doc.toObject(RecurringRule.class);
                        rule.setId(doc.getId());

                        if (rule.getWalletId() == null) {
                            pending.decrementAndGet();
                            continue;
                        }

                        // Nếu rule đã quá dateEnd → disable
                        if (rule.getDateEnd() != null
                                && now.after(rule.getDateEnd().toDate())) {
                            rule.setEnabled(false);
                            update(uid, rule);
                            pending.decrementAndGet();
                            continue;
                        }

                        // Chạy tất cả các kỳ bị bỏ lỡ
                        runMissedOccurrences(uid, rule, pending);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "catchUp failed to load rules", e));
    }

    /**
     * Chạy bù nhiều kỳ nếu app không mở trong nhiều ngày.
     */
    private void runMissedOccurrences(String uid, RecurringRule rule, AtomicInteger pending) {
        Date now = DateUtils.nowVietnam();
        Date startDate = rule.getLastRun() != null
                ? rule.getLastRun().toDate()
                : (rule.getDateStart() != null ? rule.getDateStart().toDate() : now);

        if (startDate.after(now)) {
            pending.decrementAndGet();
            return;
        }

        Date cursor = startDate;
        int runsScheduled = 0;

        while (cursor.before(now) && runsScheduled < MAX_CATCH_UP_RUNS) {
            Timestamp nextRun = calculateNextRunFromCursor(rule, cursor);
            if (nextRun == null) break;

            Date runDate = nextRun.toDate();
            if (!runDate.after(now)) {
                // Nếu runDate hợp lệ: check dateEnd, execute
                if (rule.getDateEnd() == null || !runDate.after(rule.getDateEnd().toDate())) {
                    executeOccurrence(uid, rule, nextRun, () -> {}, e -> {});
                    runsScheduled++;
                }
            }
            cursor = runDate;
        }

        pending.decrementAndGet();
    }

    /**
     * Thực thi một kỳ trong Firestore Transaction để đảm bảo atomic và idempotent.
     */
    private void executeOccurrence(String uid, RecurringRule rule, Timestamp runAt,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        String occurrenceId = rule.makeOccurrenceId(runAt);
        DocumentReference occRef = db.collection("users").document(uid)
                .collection("recurring_occurrences")
                .document(occurrenceId);

        db.runTransaction(transaction -> {
            DocumentSnapshot occSnap = transaction.get(occRef).get();
            if (occSnap.exists()) {
                return null;
            }

            // 1. Tạo occurrence record
            java.util.Map<String, Object> occData = new java.util.HashMap<>();
            occData.put("ruleId", rule.getId());
            occData.put("runAt", runAt);
            occData.put("createdAt", Timestamp.now());
            transaction.set(occRef, occData);

            // 2. Tạo transaction
            Transaction t = new Transaction();
            t.setType(rule.getType());
            t.setAmount(rule.getAmount());
            t.setCategoryId(rule.getCategoryId());
            t.setWalletId(rule.getWalletId());
            t.setRecurringRuleId(rule.getId());
            t.setNote(rule.getNote() != null ? rule.getNote()
                    : (rule.isIncome() ? "Thu nhập định kỳ" : "Chi tiêu định kỳ"));
            t.setDate(runAt);

            String walletId = rule.getWalletId();
            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document();
            transaction.set(txRef, t.toMap());

            // 3. Cập nhật số dư ví
            DocumentReference walletRef = db.collection("users").document(uid)
                    .collection("wallets").document(walletId);
            DocumentSnapshot walletSnap = transaction.get(walletRef);
            if (walletSnap.exists()) {
                Long balance = MoneyValueParser.toLong(walletSnap.get("currentBalance"));
                if (balance == null) balance = 0L;
                long newBalance = rule.isIncome()
                        ? balance + rule.getAmount()
                        : balance - rule.getAmount();
                transaction.update(walletRef, "currentBalance", newBalance);
            }

            // 4. Cập nhật rule: lastRun và nextRun
            DocumentReference ruleRef = db.collection("users").document(uid)
                    .collection("recurring").document(rule.getId());
            Timestamp newNextRun = calculateNextRunFromCursor(rule, runAt);
            transaction.update(ruleRef, "lastRun", runAt);
            if (newNextRun != null) {
                transaction.update(ruleRef, "nextRun", newNextRun);
            }
            if (rule.getDateEnd() != null && newNextRun != null
                    && newNextRun.toDate().after(rule.getDateEnd().toDate())) {
                transaction.update(ruleRef, "enabled", false);
            }

            return null;
        }).addOnSuccessListener(unused -> {
                    Log.d(TAG, "executeOccurrence success: " + occurrenceId);
                    onSuccess.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "executeOccurrence failed: " + occurrenceId, e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Tính nextRun từ một thời điểm cursor.
     * @param rule rule
     * @param cursor thời điểm hiện tại
     * @return next run timestamp, hoặc null nếu đã quá dateEnd
     */
    @Nullable
    public static Timestamp calculateNextRunFromCursor(RecurringRule rule, Date cursor) {
        Calendar cal = DateUtils.newCalendar();
        cal.setTime(cursor);
        String cycle = rule.getCycleType();
        if (cycle == null) cycle = RecurringRule.CYCLE_MONTHLY;

        switch (cycle) {
            case RecurringRule.CYCLE_DAILY:
                cal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case RecurringRule.CYCLE_WEEKLY:
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case RecurringRule.CYCLE_MONTHLY:
                cal.add(Calendar.MONTH, 1);
                if (rule.isUseLastDayOfMonth()) {
                    cal.set(Calendar.DAY_OF_MONTH,
                            cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                } else if (rule.getDayOfMonth() > 0) {
                    cal.set(Calendar.DAY_OF_MONTH,
                            Math.min(rule.getDayOfMonth(),
                                    cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
                }
                break;
            case RecurringRule.CYCLE_YEARLY:
                cal.add(Calendar.YEAR, 1);
                if (rule.getMonthOfYear() >= 1 && rule.getMonthOfYear() <= 12) {
                    cal.set(Calendar.MONTH, rule.getMonthOfYear() - 1);
                    if (rule.isUseLastDayOfMonth()) {
                        cal.set(Calendar.DAY_OF_MONTH,
                                cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    } else {
                        cal.set(Calendar.DAY_OF_MONTH,
                                Math.min(Math.max(rule.getDayOfMonth(), 1),
                                        cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
                    }
                }
                break;
        }

        Date next = cal.getTime();
        if (rule.getDateEnd() != null && next.after(rule.getDateEnd().toDate())) {
            return null;
        }
        return new Timestamp(next);
    }

    /**
     * Tính nextRun từ lastRun hoặc dateStart (dùng cho khởi tạo rule mới).
     */
    public static Timestamp calculateNextRun(RecurringRule rule) {
        Date base = rule.getLastRun() != null
                ? rule.getLastRun().toDate()
                : rule.getDateStart() != null ? rule.getDateStart().toDate()
                : DateUtils.nowVietnam();
        return calculateNextRunFromCursor(rule, base);
    }
}
