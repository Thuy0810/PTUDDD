package com.expensemanager.app.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Wallet;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WalletRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Wallet>> observeAll(String uid) {
        MutableLiveData<List<Wallet>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("wallets")
                .addSnapshotListener((snap, e) -> {
                    List<Wallet> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Wallet w = doc.toObject(Wallet.class);
                            w.setId(doc.getId());
                            list.add(w);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    @NonNull
    public Task<Void> add(String uid, Wallet w) {
        String id = w.getId() != null ? w.getId()
                : db.collection("users").document(uid)
                        .collection("wallets").document().getId();
        return db.collection("users").document(uid)
                .collection("wallets").document(id).set(w.toMap());
    }

    @NonNull
    public Task<Void> update(String uid, Wallet w) {
        return db.collection("users").document(uid)
                .collection("wallets").document(w.getId()).set(w.toMap());
    }

    @NonNull
    public Task<Void> delete(String uid, String id) {
        return db.collection("users").document(uid)
                .collection("wallets").document(id).delete();
    }
}
