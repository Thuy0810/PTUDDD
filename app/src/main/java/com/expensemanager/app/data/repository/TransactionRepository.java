package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Transaction;
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
            live.setValue(new ArrayList<>());
        }
        return live;
    }

    public LiveData<List<Transaction>> observeAll(String uid) {
        MutableLiveData<List<Transaction>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
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

    public void addAtomic(String uid, Transaction t, WalletRepository walletRepo, String walletId) {
        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document();
            transaction.set(txRef, t.toMap());

            DocumentSnapshot walletSnap = transaction.get(
                    db.collection("users").document(uid).collection("wallets").document(walletId));
            Double balance = walletSnap.getDouble("currentBalance");
            if (balance == null) balance = 0.0;
            double change = Transaction.TYPE_INCOME.equals(t.getType()) ? t.getAmount() : -t.getAmount();
            transaction.update(walletSnap.getReference(), "currentBalance", balance + change);
            return null;
        });
    }

    public void updateAtomic(String uid, Transaction original, Transaction updated,
                             WalletRepository walletRepo,
                             String originalWalletId, String newWalletId) {
        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document(updated.getId());
            transaction.set(txRef, updated.toMap());

            double originalEffect = Transaction.TYPE_INCOME.equals(original.getType())
                    ? original.getAmount() : -original.getAmount();
            double newEffect = Transaction.TYPE_INCOME.equals(updated.getType())
                    ? updated.getAmount() : -updated.getAmount();
            double totalChange = newEffect - originalEffect;

            if (originalWalletId != null && originalWalletId.equals(newWalletId)) {
                DocumentSnapshot walletSnap = transaction.get(
                        db.collection("users").document(uid).collection("wallets").document(originalWalletId));
                Double balance = walletSnap.getDouble("currentBalance");
                if (balance == null) balance = 0.0;
                transaction.update(walletSnap.getReference(), "currentBalance", balance + totalChange);
            } else {
                if (originalWalletId != null) {
                    DocumentSnapshot origSnap = transaction.get(
                            db.collection("users").document(uid).collection("wallets").document(originalWalletId));
                    Double origBalance = origSnap.getDouble("currentBalance");
                    if (origBalance == null) origBalance = 0.0;
                    transaction.update(origSnap.getReference(), "currentBalance", origBalance - originalEffect);
                }
                if (newWalletId != null) {
                    DocumentSnapshot newSnap = transaction.get(
                            db.collection("users").document(uid).collection("wallets").document(newWalletId));
                    Double newBalance = newSnap.getDouble("currentBalance");
                    if (newBalance == null) newBalance = 0.0;
                    transaction.update(newSnap.getReference(), "currentBalance", newBalance + newEffect);
                }
            }
            return null;
        });
    }

    public void deleteAtomic(String uid, Transaction t, WalletRepository walletRepo, String walletId) {
        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            if (t.getId() != null) {
                DocumentReference txRef = db.collection("users").document(uid)
                        .collection("transactions").document(t.getId());
                transaction.delete(txRef);
            }

            if (walletId != null) {
                DocumentSnapshot walletSnap = transaction.get(
                        db.collection("users").document(uid).collection("wallets").document(walletId));
                Double balance = walletSnap.getDouble("currentBalance");
                if (balance == null) balance = 0.0;
                double change = Transaction.TYPE_INCOME.equals(t.getType()) ? -t.getAmount() : t.getAmount();
                transaction.update(walletSnap.getReference(), "currentBalance", balance + change);
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
