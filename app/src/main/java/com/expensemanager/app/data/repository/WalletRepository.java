package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Wallet;
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

    public void add(String uid, Wallet w) {
        String id = w.getId() != null ? w.getId() : db.collection("users").document(uid)
                .collection("wallets").document().getId();
        db.collection("users").document(uid).collection("wallets")
                .document(id).set(w.toMap());
    }

    public void update(String uid, Wallet w) {
        db.collection("users").document(uid).collection("wallets")
                .document(w.getId()).set(w.toMap());
    }

    public void delete(String uid, String id) {
        db.collection("users").document(uid).collection("wallets")
                .document(id).delete();
    }
}
