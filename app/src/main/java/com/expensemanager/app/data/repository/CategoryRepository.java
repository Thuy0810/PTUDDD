package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;

import com.expensemanager.app.data.model.Category;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Category>> observeAll(String uid) {
        return new FirestoreQueryLiveData<>(
                db.collection("users").document(uid).collection("categories"),
                doc -> {
                    Category c = doc.toObject(Category.class);
                    c.setId(doc.getId());
                    return c;
                });
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
