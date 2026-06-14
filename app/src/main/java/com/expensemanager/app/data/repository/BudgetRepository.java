package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Budget;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BudgetRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Budget>> observeMonth(String uid, String monthKey) {
        MutableLiveData<List<Budget>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("budgets")
                .whereEqualTo("month", monthKey)
                .addSnapshotListener((snap, e) -> {
                    List<Budget> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Budget b = doc.toObject(Budget.class);
                            b.setId(doc.getId());
                            list.add(b);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    public void add(String uid, Budget b) {
        db.collection("users").document(uid).collection("budgets").add(b.toMap());
    }

    public void addOrUpdate(String uid, Budget b,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        String docId = uid + "_" + b.getMonth() + "_" + b.getCategoryId();
        b.setId(docId);
        db.collection("users").document(uid).collection("budgets")
                .document(docId).set(b.toMap())
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void addOrUpdate(String uid, Budget b) {
        addOrUpdate(uid, b, null, null);
    }

    public void delete(String uid, String id) {
        db.collection("users").document(uid).collection("budgets").document(id).delete();
    }
}
