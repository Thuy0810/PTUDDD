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
 * Nghiệp vụ chuyển tiền giữa 2 ví.
 *
 * <p>Đảm bảo (ràng buộc 4.1, 7.3):
 * <ul>
 *   <li>Validate trước khi thực hiện (ví tồn tại, khác nhau, ví không archived, amount > 0).</li>
 *   <li>Tất cả thay đổi Firestore chạy trong cùng 1 transaction: tạo transaction + cập nhật 2 ví.</li>
 *   <li>Đọc currentBalance bằng {@link MoneyValueParser#toLong(Object)} để tương thích dữ liệu cũ.</li>
 *   <li>Transfer KHÔNG tính vào income/expense và KHÔNG ảnh hưởng ngân sách.</li>
 * </ul>
 */
public final class TransferService {

    /** Mã lỗi nội bộ cho callback. */
    public static final int ERR_AMOUNT_INVALID = 1;
    public static final int ERR_SAME_WALLET = 2;
    public static final int ERR_WALLET_NOT_FOUND = 3;
    public static final int ERR_WALLET_ARCHIVED = 4;
    public static final int ERR_INSUFFICIENT_BALANCE = 5;
    public static final int ERR_INTERNAL = 6;

    private final FirebaseFirestore db;

    public TransferService() {
        this(FirebaseFirestore.getInstance());
    }

    public TransferService(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Kiểm tra trước khi chuyển. Trả về mã lỗi; 0 = OK.
     */
    public static int validate(String fromWalletId, String toWalletId, long amount) {
        if (!MoneyValueParser.isValidAmount(amount)) return ERR_AMOUNT_INVALID;
        if (fromWalletId == null || toWalletId == null) return ERR_WALLET_NOT_FOUND;
        if (fromWalletId.equals(toWalletId)) return ERR_SAME_WALLET;
        return 0;
    }

    /**
     * Thực hiện chuyển tiền. Toàn bộ thay đổi atomic trong 1 Firestore transaction.
     */
    @NonNull
    public Task<Void> performTransfer(
            @NonNull String uid,
            @NonNull String fromWalletId,
            @NonNull String toWalletId,
            long amount,
            @NonNull String note,
            @NonNull Date date) {

        int code = validate(fromWalletId, toWalletId, amount);
        if (code != 0) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("Transfer invalid: code=" + code));
        }

        Timestamp ts = new Timestamp(date);
        Map<String, Object> txData = new HashMap<>();
        txData.put("type", Transaction.TYPE_TRANSFER);
        txData.put("amount", amount);
        txData.put("fromWalletId", fromWalletId);
        txData.put("toWalletId", toWalletId);
        txData.put("walletId", fromWalletId);
        txData.put("date", ts);
        txData.put("note", note);
        txData.put("createdAt", FieldValue.serverTimestamp());
        txData.put("updatedAt", FieldValue.serverTimestamp());

        return db.runTransaction(transaction -> {
            DocumentReference fromRef = db.collection("users").document(uid)
                    .collection("wallets").document(fromWalletId);
            DocumentReference toRef = db.collection("users").document(uid)
                    .collection("wallets").document(toWalletId);

            DocumentSnapshot fromSnap = transaction.get(fromRef);
            DocumentSnapshot toSnap = transaction.get(toRef);

            if (!fromSnap.exists()) {
                throw new FirebaseFirestoreException("Ví nguồn không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (!toSnap.exists()) {
                throw new FirebaseFirestoreException("Ví nhận không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Kiểm tra archived
            Boolean fromArchived = fromSnap.getBoolean("isArchived");
            if (fromArchived != null && fromArchived) {
                throw new FirebaseFirestoreException("Ví nguồn đã lưu trữ",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Boolean toArchived = toSnap.getBoolean("isArchived");
            if (toArchived != null && toArchived) {
                throw new FirebaseFirestoreException("Ví nhận đã lưu trữ",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Long fromBalance = MoneyValueParser.toLong(fromSnap.get("currentBalance"));
            Long toBalance = MoneyValueParser.toLong(toSnap.get("currentBalance"));
            if (fromBalance == null) fromBalance = 0L;
            if (toBalance == null) toBalance = 0L;

            if (fromBalance < amount) {
                throw new FirebaseFirestoreException("Số dư không đủ",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            // Tạo transaction document
            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document();
            transaction.set(txRef, txData);

            // Cập nhật 2 ví
            transaction.update(fromRef, "currentBalance", fromBalance - amount,
                    "updatedAt", FieldValue.serverTimestamp());
            transaction.update(toRef, "currentBalance", toBalance + amount,
                    "updatedAt", FieldValue.serverTimestamp());

            return null;
        });
    }
}
