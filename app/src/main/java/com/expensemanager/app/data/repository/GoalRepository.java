package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.SavingsGoal;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GoalRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<SavingsGoal>> observeAll(String uid) {
        MutableLiveData<List<SavingsGoal>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("savings_goals")
                .addSnapshotListener((snap, e) -> {
                    List<SavingsGoal> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            SavingsGoal g = doc.toObject(SavingsGoal.class);
                            g.setId(doc.getId());
                            list.add(g);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    public void add(String uid, SavingsGoal g) {
        db.collection("users").document(uid).collection("savings_goals").add(g.toMap());
    }

    public void update(String uid, SavingsGoal g) {
        db.collection("users").document(uid).collection("savings_goals")
                .document(g.getId()).set(g.toMap());
    }

    public void delete(String uid, String id) {
        db.collection("users").document(uid).collection("savings_goals").document(id).delete();
    }
}
