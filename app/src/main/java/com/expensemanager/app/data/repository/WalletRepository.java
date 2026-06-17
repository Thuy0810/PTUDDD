package com.expensemanager.app.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.util.MoneyValueParser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Wallet>> observeAll(String uid) {
        return new FirestoreQueryLiveData<>(
                db.collection("users").document(uid).collection("wallets"),
                doc -> parseSnapshot(doc.getId(), doc.getData()));
    }

    @NonNull
    private Wallet parseSnapshot(String docId, Map<String, Object> data) {
        Wallet w = new Wallet();
        w.setId(docId);
        if (data == null) return w;
        w.setName((String) data.get("name"));
        w.setType((String) data.get("type"));
        Long initial = MoneyValueParser.toLong(data.get("initialBalance"));
        Long current = MoneyValueParser.toLong(data.get("currentBalance"));
        w.setInitialBalance(initial != null ? initial : 0L);
        w.setCurrentBalance(current != null ? current : 0L);
        w.setIcon((String) data.get("icon"));
        w.setColor((String) data.get("color"));
        Object createdAt = data.get("createdAt");
        if (createdAt instanceof Timestamp) w.setCreatedAt((Timestamp) createdAt);
        Object updatedAt = data.get("updatedAt");
        if (updatedAt instanceof Timestamp) w.setUpdatedAt((Timestamp) updatedAt);
        Object archived = data.get("isArchived");
        if (archived instanceof Boolean) w.setArchived((Boolean) archived);
        return w;
    }

    @NonNull
    public Task<Void> add(String uid, Wallet w) {
        String id = w.getId() != null ? w.getId()
                : db.collection("users").document(uid)
                        .collection("wallets").document().getId();
        w.setId(id);
        if (w.getCreatedAt() == null) w.setCreatedAt(Timestamp.now());
        w.setUpdatedAt(Timestamp.now());
        return db.collection("users").document(uid)
                .collection("wallets").document(id).set(w.toMap());
    }

    @NonNull
    public Task<Void> update(String uid, Wallet w) {
        w.setUpdatedAt(Timestamp.now());
        return db.collection("users").document(uid)
                .collection("wallets").document(w.getId()).set(w.toMap());
    }

    @NonNull
    public Task<Void> delete(String uid, String id) {
        return db.collection("users").document(uid)
                .collection("wallets").document(id).delete();
    }

    @NonNull
    public Task<Void> archive(String uid, String id) {
        if (id == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("id is null"));
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("isArchived", true);
        updates.put("updatedAt", Timestamp.now());
        return db.collection("users").document(uid)
                .collection("wallets").document(id).update(updates);
    }

    @NonNull
    public Task<Void> unarchive(String uid, String id) {
        if (id == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("id is null"));
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("isArchived", false);
        updates.put("updatedAt", Timestamp.now());
        return db.collection("users").document(uid)
                .collection("wallets").document(id).update(updates);
    }

    /**
     * Kiểm tra ví có tồn tại và không archived hay không.
     */
    @NonNull
    public Task<Boolean> exists(String uid, String id) {
        if (id == null) {
            return com.google.android.gms.tasks.Tasks.forResult(false);
        }
        return db.collection("users").document(uid)
                .collection("wallets").document(id).get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return false;
                    if (!task.getResult().exists()) return false;
                    Boolean archived = task.getResult().getBoolean("isArchived");
                    return archived == null || !archived;
                });
    }

    /**
     * Đếm số giao dịch liên quan đến ví — dùng để quyết định cho phép xoá hay archive.
     */
    @NonNull
    public Task<Long> countTransactions(String uid, String walletId) {
        if (walletId == null) {
            return com.google.android.gms.tasks.Tasks.forResult(0L);
        }
        return db.collection("users").document(uid)
                .collection("transactions")
                .whereEqualTo("walletId", walletId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return 0L;
                    return (long) task.getResult().size();
                });
    }
}
