package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;

import com.expensemanager.app.data.model.Challenge;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * CRUD cho "Thử thách tiết kiệm" (savings challenge).
 * Lưu tại users/{uid}/challenges. Không liên quan tới ví nên không cần transaction atomic.
 */
public class ChallengeRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private com.google.firebase.firestore.CollectionReference col(String uid) {
        return db.collection("users").document(uid).collection("challenges");
    }

    public LiveData<java.util.List<Challenge>> observeAll(String uid) {
        return new FirestoreQueryLiveData<>(
                col(uid),
                doc -> parseSnapshot(doc.getId(), doc.getData()));
    }

    private Challenge parseSnapshot(String docId, Map<String, Object> data) {
        Challenge c = new Challenge();
        c.setId(docId);
        if (data == null) return c;
        c.setTitle((String) data.get("title"));
        c.setDescription((String) data.get("description"));
        Object target = data.get("targetSavings");
        if (target instanceof Number) c.setTargetSavings(((Number) target).longValue());
        Object total = data.get("totalDays");
        if (total instanceof Number) c.setTotalDays(((Number) total).intValue());
        Object done = data.get("completedDays");
        if (done instanceof Number) c.setCompletedDays(((Number) done).intValue());
        Object start = data.get("startDate");
        if (start instanceof Timestamp) c.setStartDate((Timestamp) start);
        Object active = data.get("active");
        if (active instanceof Boolean) c.setActive((Boolean) active);
        Object createdAt = data.get("createdAt");
        if (createdAt instanceof Timestamp) c.setCreatedAt((Timestamp) createdAt);
        Object updatedAt = data.get("updatedAt");
        if (updatedAt instanceof Timestamp) c.setUpdatedAt((Timestamp) updatedAt);
        Object archived = data.get("isArchived");
        if (archived instanceof Boolean) c.setArchived((Boolean) archived);
        return c;
    }

    public void add(String uid, Challenge c) {
        DocumentReference ref = col(uid).document();
        c.setId(ref.getId());
        if (c.getStartDate() == null) c.setStartDate(Timestamp.now());
        c.setCreatedAt(Timestamp.now());
        c.setUpdatedAt(Timestamp.now());
        ref.set(c.toMap());
    }

    public void update(String uid, Challenge c) {
        c.setUpdatedAt(Timestamp.now());
        col(uid).document(c.getId()).set(c.toMap());
    }

    public void delete(String uid, String id) {
        col(uid).document(id).delete();
    }
}
