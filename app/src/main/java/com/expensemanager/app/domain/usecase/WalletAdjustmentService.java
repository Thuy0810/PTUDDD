package com.expensemanager.app.domain.usecase;

import androidx.annotation.NonNull;

import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.util.MoneyValueParser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Điều chỉnh số dư ví có lý do.
 *
 * <p>Cho phép người dùng chỉnh {@code currentBalance} thẳng vào giá trị mới,
 * đồng thời ghi log vào collection {@code wallet_adjustments} để audit.
 *
 * <p>Atomic: cập nhật ví + ghi log cùng transaction.
 */
public final class WalletAdjustmentService {

    private final FirebaseFirestore db;

    public WalletAdjustmentService() {
        this(FirebaseFirestore.getInstance());
    }

    public WalletAdjustmentService(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Đặt {@code currentBalance} thành {@code newBalance} và ghi log.
     *
     * @param uid user id
     * @param walletId ví cần điều chỉnh
     * @param newBalance giá trị mới (có thể &lt; 0)
     * @param reason lý do điều chỉnh
     * @param date ngày áp dụng
     */
    @NonNull
    public Task<Void> adjustTo(@NonNull String uid, @NonNull String walletId,
                                long newBalance, @NonNull String reason,
                                @NonNull Date date) {
        Timestamp ts = new Timestamp(date);
        return db.runTransaction(transaction -> {
            DocumentReference walletRef = db.collection("users").document(uid)
                    .collection("wallets").document(walletId);
            DocumentSnapshot snap = transaction.get(walletRef);
            if (!snap.exists()) {
                throw new FirebaseFirestoreException("Ví không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Boolean archived = snap.getBoolean("isArchived");
            if (archived != null && archived) {
                throw new FirebaseFirestoreException("Ví đã lưu trữ",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Long oldBalance = MoneyValueParser.toLong(snap.get("currentBalance"));
            if (oldBalance == null) oldBalance = 0L;
            long delta = newBalance - oldBalance;

            // Cập nhật ví
            transaction.update(walletRef, "currentBalance", newBalance,
                    "updatedAt", FieldValue.serverTimestamp());

            // Ghi log adjustment
            Map<String, Object> log = new HashMap<>();
            log.put("walletId", walletId);
            log.put("oldBalance", oldBalance);
            log.put("newBalance", newBalance);
            log.put("delta", delta);
            log.put("reason", reason);
            log.put("date", ts);
            log.put("createdAt", FieldValue.serverTimestamp());

            DocumentReference logRef = db.collection("users").document(uid)
                    .collection("wallet_adjustments").document();
            transaction.set(logRef, log);
            return null;
        });
    }

    /**
     * Cộng/trừ trực tiếp vào currentBalance (atomic với log).
     */
    @NonNull
    public Task<Void> adjustBy(@NonNull String uid, @NonNull String walletId,
                                 long delta, @NonNull String reason,
                                 @NonNull Date date) {
        Timestamp ts = new Timestamp(date);
        return db.runTransaction(transaction -> {
            DocumentReference walletRef = db.collection("users").document(uid)
                    .collection("wallets").document(walletId);
            DocumentSnapshot snap = transaction.get(walletRef);
            if (!snap.exists()) {
                throw new FirebaseFirestoreException("Ví không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Long balance = MoneyValueParser.toLong(snap.get("currentBalance"));
            if (balance == null) balance = 0L;
            long newBalance = balance + delta;

            transaction.update(walletRef, "currentBalance", newBalance,
                    "updatedAt", FieldValue.serverTimestamp());

            Map<String, Object> log = new HashMap<>();
            log.put("walletId", walletId);
            log.put("oldBalance", balance);
            log.put("newBalance", newBalance);
            log.put("delta", delta);
            log.put("reason", reason);
            log.put("date", ts);
            log.put("createdAt", FieldValue.serverTimestamp());
            DocumentReference logRef = db.collection("users").document(uid)
                    .collection("wallet_adjustments").document();
            transaction.set(logRef, log);
            return null;
        });
    }
}
