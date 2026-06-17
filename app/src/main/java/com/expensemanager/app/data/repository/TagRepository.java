package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Tag;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private com.google.firebase.firestore.CollectionReference col(String uid) {
        return db.collection("users").document(uid).collection("tags");
    }

    public LiveData<List<Tag>> observeAll(String uid) {
        MutableLiveData<List<Tag>> live = new MutableLiveData<>();
        col(uid).orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    List<Tag> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Tag t = doc.toObject(Tag.class);
                            t.setId(doc.getId());
                            list.add(t);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    public static Map<String, Tag> toMap(List<Tag> list) {
        Map<String, Tag> map = new HashMap<>();
        if (list != null) {
            for (Tag t : list) {
                if (t.getId() != null) map.put(t.getId(), t);
            }
        }
        return map;
    }

    public Task<Void> add(String uid, Tag t) {
        String id = t.getId() != null ? t.getId() : col(uid).document().getId();
        return col(uid).document(id).set(t.toMap());
    }

    public Task<Void> update(String uid, Tag t) {
        return col(uid).document(t.getId()).set(t.toMap());
    }

    public Task<Void> delete(String uid, String id) {
        return col(uid).document(id).delete();
    }
}
