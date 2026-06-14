package com.expensemanager.app.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.util.MoneyValueParser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository cho {@link Budget}.
 *
 * <p>Migration an toàn (ràng buộc 5.2): đọc {@code limitAmount} bằng
 * {@link MoneyValueParser#toLong(Object)} nên dữ liệu cũ lưu dưới dạng
 * {@code Double} vẫn đọc được mà không cần cập nhật hàng loạt.
 */
public class BudgetRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Observe tất cả budget của user — UI tự lọc theo tháng.
     * Trả về snapshot liên tục.
     */
    public LiveData<List<Budget>> observeAll(String uid) {
        MutableLiveData<List<Budget>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("budgets")
                .addSnapshotListener((snap, e) -> {
                    List<Budget> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Budget b = parseSnapshot(doc.getId(), doc.getData());
                            list.add(b);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    public LiveData<List<Budget>> observeMonth(String uid, String monthKey) {
        MutableLiveData<List<Budget>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("budgets")
                .whereEqualTo("month", monthKey)
                .addSnapshotListener((snap, e) -> {
                    List<Budget> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Budget b = parseSnapshot(doc.getId(), doc.getData());
                            list.add(b);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    /**
     * Parse snapshot an toàn: đọc {@code limitAmount} bằng {@link MoneyValueParser}.
     * Tương thích ngược với dữ liệu cũ {@code Double}.
     */
    @NonNull
    private Budget parseSnapshot(String docId, Map<String, Object> data) {
        Budget b = new Budget();
        b.setId(docId);
        if (data == null) return b;
        b.setScope((String) data.get("scope"));
        b.setCategoryId((String) data.get("categoryId"));
        b.setMonth((String) data.get("month"));

        Long limitLong = MoneyValueParser.toLong(data.get("limitAmount"));
        b.setLimitAmount(limitLong != null ? limitLong : 0L);

        Object alerts = data.get("alertAt");
        if (alerts instanceof List) {
            try {
                List<Double> raw = (List<Double>) alerts;
                b.setAlertAt(new ArrayList<>(raw));
            } catch (ClassCastException ignore) {
                // Bỏ qua, dùng default
            }
        }

        Object createdAt = data.get("createdAt");
        if (createdAt instanceof Timestamp) b.setCreatedAt((Timestamp) createdAt);
        Object updatedAt = data.get("updatedAt");
        if (updatedAt instanceof Timestamp) b.setUpdatedAt((Timestamp) updatedAt);
        Object archived = data.get("isArchived");
        if (archived instanceof Boolean) b.setArchived((Boolean) archived);
        return b;
    }

    public void add(String uid, Budget b) {
        if (b.getCreatedAt() == null) b.setCreatedAt(Timestamp.now());
        b.setUpdatedAt(Timestamp.now());
        db.collection("users").document(uid).collection("budgets").add(b.toMap());
    }

    /**
     * Thêm mới hoặc cập nhật budget theo id ổn định
     * {@code uid_month_categoryId|monthly}.
     */
    public void addOrUpdate(String uid, Budget b,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        String docId = generateDocId(uid, b);
        b.setId(docId);
        if (b.getCreatedAt() == null) b.setCreatedAt(Timestamp.now());
        b.setUpdatedAt(Timestamp.now());
        db.collection("users").document(uid).collection("budgets")
                .document(docId).set(b.toMap())
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void addOrUpdate(String uid, Budget b) {
        addOrUpdate(uid, b, null, null);
    }

    /**
     * Cập nhật riêng trường limitAmount — dùng cho UI chỉnh sửa nhanh.
     * KHÔNG thay thế cả document, tránh mất alertAt và các trường khác.
     */
    public void updateLimitAmount(String uid, String budgetId, long newLimit,
                                   OnSuccessListener<Void> onSuccess,
                                   OnFailureListener onFailure) {
        if (budgetId == null) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalArgumentException("budgetId is null"));
            }
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("limitAmount", newLimit);
        updates.put("updatedAt", Timestamp.now());
        db.collection("users").document(uid).collection("budgets")
                .document(budgetId)
                .update(updates)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void updateLimitAmount(String uid, String budgetId, long newLimit) {
        updateLimitAmount(uid, budgetId, newLimit, null, null);
    }

    public void archive(String uid, String budgetId) {
        if (budgetId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("isArchived", true);
        updates.put("updatedAt", Timestamp.now());
        db.collection("users").document(uid).collection("budgets")
                .document(budgetId).update(updates);
    }

    public void delete(String uid, String id) {
        db.collection("users").document(uid).collection("budgets").document(id).delete();
    }

    /**
     * Tạo document id ổn định để {@code addOrUpdate} không tạo bản ghi trùng.
     * Với budget monthly: {@code uid_month_monthly}.
     * Với budget category: {@code uid_month_categoryId}.
     */
    @NonNull
    public static String generateDocId(String uid, @NonNull Budget b) {
        if (Budget.SCOPE_CATEGORY.equals(b.getScope()) && b.getCategoryId() != null) {
            return uid + "_" + b.getMonth() + "_" + b.getCategoryId();
        }
        return uid + "_" + b.getMonth() + "_monthly";
    }
}
