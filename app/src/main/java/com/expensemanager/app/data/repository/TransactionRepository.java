package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.util.DateUtils;
import com.google.firebase.Timestamp;
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
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            Date end = cal.getTime();

            db.collection("users").document(uid).collection("transactions")
                    .whereGreaterThanOrEqualTo("date", new Timestamp(start))
                    .whereLessThanOrEqualTo("date", new Timestamp(end))
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

    public void add(String uid, Transaction t) {
        db.collection("users").document(uid).collection("transactions")
                .add(t.toMap());
    }

    public void update(String uid, Transaction t) {
        if (t.getId() == null) return;
        db.collection("users").document(uid).collection("transactions")
                .document(t.getId()).set(t.toMap());
    }

    public void delete(String uid, String txId) {
        db.collection("users").document(uid).collection("transactions")
                .document(txId).delete();
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
            if (t.getNote().toLowerCase().contains(q)
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
}
