package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Category>> observeAll(String uid) {
        MutableLiveData<List<Category>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("categories")
                .addSnapshotListener((snap, e) -> {
                    List<Category> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Category c = doc.toObject(Category.class);
                            c.setId(doc.getId());
                            list.add(c);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    public static Map<String, Category> toMap(List<Category> list) {
        Map<String, Category> map = new HashMap<>();
        for (Category c : list) map.put(c.getId(), c);
        return map;
    }

    public void add(String uid, Category c) {
        String id = c.getId() != null ? c.getId() : db.collection("users").document(uid)
                .collection("categories").document().getId();
        db.collection("users").document(uid).collection("categories")
                .document(id).set(c.toMap());
    }

    public void update(String uid, Category c) {
        db.collection("users").document(uid).collection("categories")
                .document(c.getId()).set(c.toMap());
    }

    public void delete(String uid, String id) {
        db.collection("users").document(uid).collection("categories")
                .document(id).delete();
    }
}
