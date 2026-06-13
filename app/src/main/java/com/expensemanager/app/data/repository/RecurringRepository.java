package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.RecurringRule;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecurringRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TransactionRepository txRepo = new TransactionRepository();
    private final WalletRepository walletRepo = new WalletRepository();

    public LiveData<List<RecurringRule>> observeAll(String uid) {
        MutableLiveData<List<RecurringRule>> live = new MutableLiveData<>();
        db.collection("users").document(uid).collection("recurring")
                .addSnapshotListener((snap, e) -> {
                    List<RecurringRule> list = new ArrayList<>();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            RecurringRule r = doc.toObject(RecurringRule.class);
                            r.setId(doc.getId());
                            list.add(r);
                        }
                    }
                    live.setValue(list);
                });
        return live;
    }

    public void catchUp(String uid) {
        db.collection("users").document(uid).collection("recurring")
                .whereEqualTo("enabled", true)
                .get()
                .addOnSuccessListener(snap -> {
                    Calendar now = Calendar.getInstance();
                    int today = now.get(Calendar.DAY_OF_MONTH);
                    int todayStart = (int) (now.getTimeInMillis() / 86400000);
                    int todayEnd = todayStart + 1;

                    for (QueryDocumentSnapshot doc : snap) {
                        RecurringRule rule = doc.toObject(RecurringRule.class);
                        if (rule == null || rule.getWalletId() == null) continue;
                        if (rule.getDayOfMonth() == today) {
                            checkAndCreateTransaction(uid, rule, todayStart, todayEnd);
                        }
                    }
                });
    }

    private void checkAndCreateTransaction(String uid, RecurringRule rule, int todayStart, int todayEnd) {
        long startTs = (long) todayStart * 86400000;
        long endTs = (long) todayEnd * 86400000;

        db.collection("users").document(uid).collection("transactions")
                .whereEqualTo("walletId", rule.getWalletId())
                .whereEqualTo("note", rule.getNote() != null ? rule.getNote() : "Giao dịch định kỳ")
                .whereGreaterThanOrEqualTo("date", new Timestamp(new java.util.Date(startTs)))
                .whereLessThan("date", new Timestamp(new java.util.Date(endTs)))
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        return;
                    }
                    createTransactionAndUpdateBalance(uid, rule);
                });
    }

    private void createTransactionAndUpdateBalance(String uid, RecurringRule rule) {
        Transaction t = new Transaction();
        t.setType(rule.getType());
        t.setAmount(rule.getAmount());
        t.setCategoryId(rule.getCategoryId());
        t.setWalletId(rule.getWalletId());
        t.setNote(rule.getNote() != null ? rule.getNote() : "Giao dịch định kỳ");
        t.setDate(Timestamp.now());

        txRepo.add(uid, t);

        db.collection("users").document(uid).collection("wallets")
                .document(rule.getWalletId())
                .get()
                .addOnSuccessListener(walletDoc -> {
                    if (walletDoc.exists()) {
                        Wallet wallet = walletDoc.toObject(Wallet.class);
                        if (wallet != null) {
                            double change = Transaction.TYPE_INCOME.equals(rule.getType())
                                    ? rule.getAmount() : -rule.getAmount();
                            wallet.setCurrentBalance(wallet.getCurrentBalance() + change);
                            walletRepo.update(uid, wallet);
                        }
                    }
                });
    }

    public void add(String uid, RecurringRule rule) {
        db.collection("users").document(uid).collection("recurring").add(rule.toMap());
    }

    public void update(String uid, RecurringRule rule) {
        if (rule.getId() == null) return;
        db.collection("users").document(uid).collection("recurring")
                .document(rule.getId()).set(rule.toMap());
    }

    public void delete(String uid, String id) {
        db.collection("users").document(uid).collection("recurring")
                .document(id).delete();
    }
}
