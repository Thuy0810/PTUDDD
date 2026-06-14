package com.expensemanager.app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Transaction;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Transaction>> observeMonth(String uid, String monthKey) {
        MutableLiveData<List<Transaction>> live = new MutableLiveData<>();
        try {
            String[] parts = monthKey.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1, 0, 0, 0);
            Date start = cal.getTime();
            Calendar endCal = (Calendar) cal.clone();
            endCal.add(Calendar.MONTH, 1);
            Date end = endCal.getTime();

            db.collection("users").document(uid).collection("transactions")
                    .whereGreaterThanOrEqualTo("date", new Timestamp(start))
                    .whereLessThan("date", new Timestamp(end))
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener((snap, e) -> {
                        if (e != null) {
                            Log.e(TAG, "observeMonth: listen failed", e);
                            live.setValue(new ArrayList<>());
                            return;
                        }
                        List<Transaction> list = new ArrayList<>();
                        if (snap != null) {
                            for (QueryDocumentSnapshot doc : snap) {
                                Transaction t = doc.toObject(Transaction.class);
                                t.setId(doc.getId());
                                list.add(t);
                            }
                        }
                        live.setValue(list);
                    });
        } catch (Exception ex) {
            Log.e(TAG, "observeMonth: exception", ex);
            live.setValue(new ArrayList<>());
        }
        return live;
    }

    public LiveData<List<Transaction>> observeAll(String uid) {
        MutableLiveData<List<Transaction>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "observeAll: listen failed", e);
                        live.setValue(new ArrayList<>());
                        return;
                    }
                    List<Transaction> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Transaction t = doc.toObject(Transaction.class);
                            t.setId(doc.getId());
                            list.add(t);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    public void add(String uid, Transaction t, WalletRepository walletRepo) {
        if (t.getWalletId() == null || t.getWalletId().isEmpty()) {
            db.collection("users").document(uid).collection("transactions").add(t.toMap());
            return;
        }
        addAtomic(uid, t, walletRepo, t.getWalletId());
    }

    public void update(String uid, Transaction t, WalletRepository walletRepo) {
        if (t.getId() == null) return;
        updateAtomic(uid, t, t, walletRepo, t.getWalletId(), t.getWalletId());
    }

    public void delete(String uid, String txId) {
        db.collection("users").document(uid).collection("transactions")
                .document(txId).delete();
    }

    @NonNull
    public Task<Void> addAtomic(String uid, Transaction t, WalletRepository walletRepo, String walletId) {
        return db.runTransaction(transaction -> {
            DocumentReference walletRef = db.collection("users").document(uid)
                    .collection("wallets").document(walletId);
            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document();

            // Read wallet balance FIRST
            DocumentSnapshot walletSnap = transaction.get(walletRef);
            Double balance = walletSnap.getDouble("currentBalance");
            if (balance == null) balance = 0.0;
            double change = Transaction.TYPE_INCOME.equals(t.getType()) ? t.getAmount() : -t.getAmount();

            // THEN write transaction and update wallet
            transaction.set(txRef, t.toMap());
            transaction.update(walletRef, "currentBalance", balance + change);
            return null;
        });
    }

    @NonNull
    public Task<Void> updateAtomic(String uid, Transaction original, Transaction updated,
                                   WalletRepository walletRepo,
                                   String originalWalletId, String newWalletId) {
        return db.runTransaction(transaction -> {
            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document(updated.getId());

            double originalEffect = Transaction.TYPE_INCOME.equals(original.getType())
                    ? original.getAmount() : -original.getAmount();
            double newEffect = Transaction.TYPE_INCOME.equals(updated.getType())
                    ? updated.getAmount() : -updated.getAmount();
            double totalChange = newEffect - originalEffect;

            if (originalWalletId != null && originalWalletId.equals(newWalletId)) {
                // Read the single wallet BEFORE any write
                DocumentReference walletRef = db.collection("users").document(uid)
                        .collection("wallets").document(originalWalletId);
                DocumentSnapshot walletSnap = transaction.get(walletRef);
                Double balance = walletSnap.getDouble("currentBalance");
                if (balance == null) balance = 0.0;
                // Write transaction first, then update wallet
                transaction.set(txRef, updated.toMap());
                transaction.update(walletRef, "currentBalance", balance + totalChange);
            } else {
                DocumentReference origWalletRef = null;
                DocumentReference newWalletRef = null;
                if (originalWalletId != null) {
                    origWalletRef = db.collection("users").document(uid)
                            .collection("wallets").document(originalWalletId);
                }
                if (newWalletId != null) {
                    newWalletRef = db.collection("users").document(uid)
                            .collection("wallets").document(newWalletId);
                }
                // Read both wallets BEFORE any write
                DocumentSnapshot origSnap = null;
                DocumentSnapshot newSnap = null;
                if (origWalletRef != null) {
                    origSnap = transaction.get(origWalletRef);
                }
                if (newWalletRef != null) {
                    newSnap = transaction.get(newWalletRef);
                }
                // Write transaction first
                transaction.set(txRef, updated.toMap());
                // Then update original wallet (reverse effect)
                if (origSnap != null) {
                    Double origBalance = origSnap.getDouble("currentBalance");
                    if (origBalance == null) origBalance = 0.0;
                    transaction.update(origWalletRef, "currentBalance", origBalance - originalEffect);
                }
                // Then update new wallet (apply new effect)
                if (newSnap != null) {
                    Double newBalance = newSnap.getDouble("currentBalance");
                    if (newBalance == null) newBalance = 0.0;
                    transaction.update(newWalletRef, "currentBalance", newBalance + newEffect);
                }
            }
            return null;
        });
    }

    @NonNull
    public Task<Void> deleteAtomic(String uid, Transaction t, WalletRepository walletRepo, String walletId) {
        return db.runTransaction(transaction -> {
            DocumentReference txRef = null;
            if (t.getId() != null) {
                txRef = db.collection("users").document(uid)
                        .collection("transactions").document(t.getId());
            }

            DocumentReference walletRef = null;
            if (walletId != null) {
                walletRef = db.collection("users").document(uid)
                        .collection("wallets").document(walletId);
            }

            // Read wallet balance FIRST
            Double balance = null;
            if (walletRef != null) {
                DocumentSnapshot walletSnap = transaction.get(walletRef);
                balance = walletSnap.getDouble("currentBalance");
                if (balance == null) balance = 0.0;
            }

            // Then write: delete transaction first, update wallet second
            if (txRef != null) {
                transaction.delete(txRef);
            }
            if (walletRef != null) {
                double change = Transaction.TYPE_INCOME.equals(t.getType()) ? -t.getAmount() : t.getAmount();
                transaction.update(walletRef, "currentBalance", balance + change);
            }
            return null;
        });
    }

    public static List<Transaction> filterByCategory(List<Transaction> list, String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) return list;
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : list) {
            if (categoryId.equals(t.getCategoryId())) out.add(t);
        }
        return out;
    }

    public static List<Transaction> filterBySearch(List<Transaction> list, String query) {
        if (query == null || query.trim().isEmpty()) return list;
        String q = query.toLowerCase();
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : list) {
            String note = t.getNote();
            if ((note != null && note.toLowerCase().contains(q))
                    || String.valueOf((long) t.getAmount()).contains(q)) {
                out.add(t);
            }
        }
        return out;
    }

    public static List<Transaction> expensesOnly(List<Transaction> list) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : list) {
            if (Transaction.TYPE_EXPENSE.equals(t.getType())) out.add(t);
        }
        return out;
    }

    public LiveData<List<Transaction>> observeRange(String uid, Date start, Date end) {
        MutableLiveData<List<Transaction>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("transactions")
                .whereGreaterThanOrEqualTo("date", new Timestamp(start))
                .whereLessThan("date", new Timestamp(end))
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "observeRange: listen failed", e);
                        live.setValue(new ArrayList<>());
                        return;
                    }
                    List<Transaction> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Transaction t = doc.toObject(Transaction.class);
                            t.setId(doc.getId());
                            list.add(t);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }
}
