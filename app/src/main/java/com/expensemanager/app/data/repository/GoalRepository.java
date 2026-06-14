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
        MutableLiveData<List<SavingsGoal>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("savings_goals")
                .addSnapshotListener((snap, e) -> {
                    List<SavingsGoal> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            SavingsGoal g = parseSnapshot(doc.getId(), doc.getData());
                            list.add(g);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    public LiveData<List<SavingsGoal>> observeOverdue(String uid) {
        MutableLiveData<List<SavingsGoal>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("savings_goals")
                .whereEqualTo("completed", false)
                .addSnapshotListener((snap, e) -> {
                    List<SavingsGoal> overdue = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            SavingsGoal g = parseSnapshot(doc.getId(), doc.getData());
                            if (g.isOverdue()) overdue.add(g);
                        }
                    }
                    live.setValue(overdue);
                });
        return live;
    }

    private SavingsGoal parseSnapshot(String docId, Map<String, Object> data) {
        SavingsGoal g = new SavingsGoal();
        g.setId(docId);
        if (data == null) return g;
        g.setTitle((String) data.get("title"));
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

    public void delete(String uid, String id) {
        db.collection("users").document(uid).collection("savings_goals").document(id).delete();
    }
}
