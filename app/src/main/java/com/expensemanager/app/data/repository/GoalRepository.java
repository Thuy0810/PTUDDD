package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.util.MoneyValueParser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<SavingsGoal>> observeAll(String uid) {
        return new FirestoreQueryLiveData<>(
                db.collection("users").document(uid).collection("savings_goals"),
                doc -> parseSnapshot(doc.getId(), doc.getData()));
    }

    public LiveData<List<SavingsGoal>> observeOverdue(String uid) {
        return new FirestoreQueryLiveData<>(
                db.collection("users").document(uid).collection("savings_goals")
                        .whereEqualTo("completed", false),
                doc -> {
                    SavingsGoal g = parseSnapshot(doc.getId(), doc.getData());
                    // Trả null để FirestoreQueryLiveData bỏ qua goal chưa quá hạn.
                    return g.isOverdue() ? g : null;
                });
    }

    private SavingsGoal parseSnapshot(String docId, Map<String, Object> data) {
        SavingsGoal g = new SavingsGoal();
        g.setId(docId);
        if (data == null) return g;
        g.setTitle((String) data.get("title"));
        g.setIconKey((String) data.get("iconKey"));
        Long target = MoneyValueParser.toLong(data.get("targetAmount"));
        Long saved = MoneyValueParser.toLong(data.get("savedAmount"));
        g.setTargetAmount(target != null ? target : 0L);
        g.setSavedAmount(saved != null ? saved : 0L);
        g.setWalletId((String) data.get("walletId"));
        Object completed = data.get("completed");
        if (completed instanceof Boolean) g.setCompleted((Boolean) completed);
        Object deadline = data.get("deadline");
        if (deadline instanceof Timestamp) g.setDeadline((Timestamp) deadline);
        Object createdAt = data.get("createdAt");
        if (createdAt instanceof Timestamp) g.setCreatedAt((Timestamp) createdAt);
        Object updatedAt = data.get("updatedAt");
        if (updatedAt instanceof Timestamp) g.setUpdatedAt((Timestamp) updatedAt);
        Object archived = data.get("isArchived");
        if (archived instanceof Boolean) g.setArchived((Boolean) archived);
        return g;
    }

    /**
     * Đóng góp vào mục tiêu tiết kiệm.
     *
     * <p>Atomic (ràng buộc 7.3, 8): trong 1 Firestore transaction
     * <ol>
     *   <li>Đọc ví, kiểm tra tồn tại + không archived + đủ số dư.</li>
     *   <li>Đọc goal, kiểm tra tồn tại + không archived + không completed.</li>
     *   <li>Trừ ví.</li>
     *   <li>Cộng savedAmount của goal, đánh dấu completed nếu đạt target.</li>
     *   <li>Ghi log history.</li>
     * </ol>
     */
    public Task<Void> addContribution(String uid, String goalId, long amount, String walletId) {
        if (!MoneyValueParser.isValidAmount(amount)) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("amount <= 0"));
        }
        return db.runTransaction(transaction -> {
            DocumentReference walletRef = db.collection("users").document(uid)
                    .collection("wallets").document(walletId);
            DocumentReference goalRef = db.collection("users").document(uid)
                    .collection("savings_goals").document(goalId);

            DocumentSnapshot walletSnap = transaction.get(walletRef);
            DocumentSnapshot goalSnap = transaction.get(goalRef);

            if (!walletSnap.exists()) {
                throw new FirebaseFirestoreException("Ví không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Boolean walletArchived = walletSnap.getBoolean("isArchived");
            if (walletArchived != null && walletArchived) {
                throw new FirebaseFirestoreException("Ví đã lưu trữ",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (!goalSnap.exists()) {
                throw new FirebaseFirestoreException("Mục tiêu không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Boolean goalArchived = goalSnap.getBoolean("isArchived");
            if (goalArchived != null && goalArchived) {
                throw new FirebaseFirestoreException("Mục tiêu đã lưu trữ",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Boolean goalCompleted = goalSnap.getBoolean("completed");
            if (goalCompleted != null && goalCompleted) {
                throw new FirebaseFirestoreException("Mục tiêu đã hoàn thành",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Long balance = MoneyValueParser.toLong(walletSnap.get("currentBalance"));
            if (balance == null) balance = 0L;
            if (balance < amount) {
                throw new FirebaseFirestoreException("Số dư không đủ",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Long target = MoneyValueParser.toLong(goalSnap.get("targetAmount"));
            Long saved = MoneyValueParser.toLong(goalSnap.get("savedAmount"));
            if (target == null) target = 0L;
            if (saved == null) saved = 0L;
            long newSaved = saved + amount;

            // Cập nhật ví
            transaction.update(walletRef, "currentBalance", balance - amount,
                    "updatedAt", FieldValue.serverTimestamp());

            // Cập nhật goal
            Map<String, Object> goalUpdates = new HashMap<>();
            goalUpdates.put("savedAmount", newSaved);
            goalUpdates.put("updatedAt", FieldValue.serverTimestamp());
            if (newSaved >= target && target > 0L) {
                goalUpdates.put("completed", true);
            }
            transaction.update(goalRef, goalUpdates);

            // Log history
            Map<String, Object> entry = new HashMap<>();
            entry.put("goalId", goalId);
            entry.put("amount", amount);
            entry.put("walletId", walletId);
            entry.put("date", FieldValue.serverTimestamp());
            DocumentReference historyRef = db.collection("users").document(uid)
                    .collection("savings_history").document();
            transaction.set(historyRef, entry);
            return null;
        });
    }

    /**
     * API cũ — giữ lại để tương thích, nhưng KHÔNG atomic.
     * Nên dùng {@link #addContribution(String, String, long, String)} thay thế.
     *
     * @deprecated dùng addContribution(uid, goalId, amount, walletId) để đảm bảo atomic.
     */
    @Deprecated
    public void addContributionLegacy(String uid, String goalId, long amount, String walletId) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("goalId", goalId);
        entry.put("amount", amount);
        entry.put("walletId", walletId);
        entry.put("date", Timestamp.now());
        db.collection("users").document(uid).collection("savings_history").add(entry);
    }

    /**
     * Sửa thẳng số tiền đã tiết kiệm (savedAmount) của mục tiêu — không đụng tới ví.
     *
     * <p>Tự đánh dấu completed nếu đạt/ vượt target, bỏ completed nếu xuống dưới.
     */
    public Task<Void> updateSavedAmount(String uid, String goalId, long newSaved) {
        if (newSaved < 0) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("savedAmount < 0"));
        }
        DocumentReference goalRef = db.collection("users").document(uid)
                .collection("savings_goals").document(goalId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(goalRef);
            if (!snap.exists()) {
                throw new FirebaseFirestoreException("Mục tiêu không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Long target = MoneyValueParser.toLong(snap.get("targetAmount"));
            if (target == null) target = 0L;

            Map<String, Object> updates = new HashMap<>();
            updates.put("savedAmount", newSaved);
            updates.put("completed", target > 0L && newSaved >= target);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(goalRef, updates);
            return null;
        });
    }

    /**
     * Chuyển số tiền đã tiết kiệm giữa hai mục tiêu (atomic).
     *
     * <p>Chỉ di chuyển savedAmount — KHÔNG ảnh hưởng tới ví (tiền đã rút khỏi ví
     * khi đóng góp trước đó). Trong 1 transaction: kiểm tra cả hai mục tiêu tồn tại,
     * không archived, mục tiêu nguồn đủ savedAmount; trừ nguồn, cộng đích; cập nhật
     * cờ completed cho cả hai theo target.
     */
    public Task<Void> transferBetweenGoals(String uid, String fromGoalId, String toGoalId, long amount) {
        if (fromGoalId == null || toGoalId == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("goalId is null"));
        }
        if (fromGoalId.equals(toGoalId)) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("same goal"));
        }
        if (amount <= 0) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("amount <= 0"));
        }
        DocumentReference fromRef = db.collection("users").document(uid)
                .collection("savings_goals").document(fromGoalId);
        DocumentReference toRef = db.collection("users").document(uid)
                .collection("savings_goals").document(toGoalId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot fromSnap = transaction.get(fromRef);
            DocumentSnapshot toSnap = transaction.get(toRef);
            if (!fromSnap.exists() || !toSnap.exists()) {
                throw new FirebaseFirestoreException("Mục tiêu không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Boolean fromArchived = fromSnap.getBoolean("isArchived");
            Boolean toArchived = toSnap.getBoolean("isArchived");
            if ((fromArchived != null && fromArchived) || (toArchived != null && toArchived)) {
                throw new FirebaseFirestoreException("Mục tiêu đã lưu trữ",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Long fromSaved = MoneyValueParser.toLong(fromSnap.get("savedAmount"));
            Long toSaved = MoneyValueParser.toLong(toSnap.get("savedAmount"));
            Long fromTarget = MoneyValueParser.toLong(fromSnap.get("targetAmount"));
            Long toTarget = MoneyValueParser.toLong(toSnap.get("targetAmount"));
            if (fromSaved == null) fromSaved = 0L;
            if (toSaved == null) toSaved = 0L;
            if (fromTarget == null) fromTarget = 0L;
            if (toTarget == null) toTarget = 0L;
            if (fromSaved < amount) {
                throw new FirebaseFirestoreException("Số tiền vượt quá nguồn có thể chuyển",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            long newFromSaved = fromSaved - amount;
            long newToSaved = toSaved + amount;

            Map<String, Object> fromUpdates = new HashMap<>();
            fromUpdates.put("savedAmount", newFromSaved);
            fromUpdates.put("completed", fromTarget > 0L && newFromSaved >= fromTarget);
            fromUpdates.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(fromRef, fromUpdates);

            Map<String, Object> toUpdates = new HashMap<>();
            toUpdates.put("savedAmount", newToSaved);
            toUpdates.put("completed", toTarget > 0L && newToSaved >= toTarget);
            toUpdates.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(toRef, toUpdates);
            return null;
        });
    }

    public void add(String uid, SavingsGoal g) {
        DocumentReference ref = db.collection("users").document(uid)
                .collection("savings_goals").document();
        g.setId(ref.getId());
        if (g.getCreatedAt() == null) g.setCreatedAt(Timestamp.now());
        g.setUpdatedAt(Timestamp.now());
        ref.set(g.toMap());
    }

    public void update(String uid, SavingsGoal g) {
        g.setUpdatedAt(Timestamp.now());
        db.collection("users").document(uid).collection("savings_goals")
                .document(g.getId()).set(g.toMap());
    }

    /** Cập nhật riêng icon của mục tiêu. */
    public Task<Void> updateIcon(String uid, String goalId, String iconKey) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("iconKey", iconKey);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        return db.collection("users").document(uid).collection("savings_goals")
                .document(goalId).update(updates);
    }

    public void delete(String uid, String id) {
        db.collection("users").document(uid).collection("savings_goals").document(id).delete();
    }
}
