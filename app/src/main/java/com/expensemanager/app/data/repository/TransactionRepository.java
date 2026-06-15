package com.expensemanager.app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.ui.state.UiState;
import com.expensemanager.app.util.MoneyValueParser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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

    public Task<Void> add(String uid, Transaction transaction) {
        if (transaction.getWalletId() == null || transaction.getWalletId().isEmpty()) {
            return db.collection("users").document(uid)
                    .collection("transactions")
                    .add(transaction.toMap())
                    .continueWith(task -> null);
        }
        return addAtomic(uid, transaction, transaction.getWalletId());
    }

    @NonNull
    public Task<Void> addAtomic(String uid, Transaction t, String walletId) {
        return db.runTransaction(transaction -> {
            DocumentReference walletRef = db.collection("users").document(uid)
                    .collection("wallets").document(walletId);
            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document();

            DocumentSnapshot walletSnap = transaction.get(walletRef);
            if (!walletSnap.exists()) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Ví không tồn tại",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Boolean archived = walletSnap.getBoolean("isArchived");
            if (archived != null && archived) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Ví đã lưu trữ",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Long balance = MoneyValueParser.toLong(walletSnap.get("currentBalance"));
            if (balance == null) balance = 0L;
            long change = balanceChange(t);
            transaction.set(txRef, t.toMap());
            transaction.update(walletRef, "currentBalance", balance + change,
                    "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            return null;
        });
    }

    /**
     * Tính ảnh hưởng của transaction lên {@code currentBalance} của ví chính.
     * <ul>
     *   <li>income: +amount</li>
     *   <li>expense: -amount</li>
     *   <li>transfer: -amount (ví nguồn mới ghi)</li>
     * </ul>
     */
    private long balanceChange(Transaction t) {
        if (Transaction.TYPE_INCOME.equals(t.getType())) return t.getAmount();
        if (Transaction.TYPE_EXPENSE.equals(t.getType())) return -t.getAmount();
        if (Transaction.TYPE_TRANSFER.equals(t.getType())) return -t.getAmount();
        return 0L;
    }

    @NonNull
    public Task<Void> updateAtomic(String uid, Transaction original, Transaction updated,
                                   String originalWalletId, String newWalletId) {
        return db.runTransaction(transaction -> {
            DocumentReference txRef = db.collection("users").document(uid)
                    .collection("transactions").document(updated.getId());

            long originalEffect = Transaction.TYPE_INCOME.equals(original.getType())
                    ? original.getAmount() : -original.getAmount();
            long newEffect = Transaction.TYPE_INCOME.equals(updated.getType())
                    ? updated.getAmount() : -updated.getAmount();
            double totalChange = newEffect - originalEffect;

            if (originalWalletId != null && originalWalletId.equals(newWalletId)) {
                DocumentReference walletRef = db.collection("users").document(uid)
                        .collection("wallets").document(originalWalletId);
                DocumentSnapshot walletSnap = transaction.get(walletRef);
                Double balance = walletSnap.getDouble("currentBalance");
                if (balance == null) balance = 0.0;
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
                DocumentSnapshot origSnap = null;
                DocumentSnapshot newSnap = null;
                if (origWalletRef != null) {
                    origSnap = transaction.get(origWalletRef);
                }
                if (newWalletRef != null) {
                    newSnap = transaction.get(newWalletRef);
                }
                transaction.set(txRef, updated.toMap());
                if (origSnap != null) {
                    Double origBalance = origSnap.getDouble("currentBalance");
                    if (origBalance == null) origBalance = 0.0;
                    transaction.update(origWalletRef, "currentBalance", origBalance - originalEffect);
                }
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
    public Task<Void> deleteAtomic(String uid, Transaction t, String walletId) {
        final Transaction requested = t;
        return db.runTransaction(transaction -> {
            DocumentReference txRef = null;
            if (requested.getId() != null) {
                txRef = db.collection("users").document(uid)
                        .collection("transactions").document(requested.getId());
            }

            // Đọc lại transaction từ Firestore để chắc chắn dùng dữ liệu mới nhất
            Transaction actualTransaction = requested;
            if (txRef != null) {
                DocumentSnapshot fresh = transaction.get(txRef);
                if (fresh.exists()) {
                    Transaction actual = fresh.toObject(Transaction.class);
                    if (actual != null) {
                        actual.setId(fresh.getId());
                        actualTransaction = actual;
                    }
                }
            }

            // Xử lý transfer riêng
            if (Transaction.TYPE_TRANSFER.equals(actualTransaction.getType())) {
                return deleteTransfer(transaction, uid, actualTransaction);
            }

            // Xử lý income/expense thông thường
            DocumentReference walletRef = null;
            if (walletId != null) {
                walletRef = db.collection("users").document(uid)
                        .collection("wallets").document(walletId);
            }

            Long balance = null;
            if (walletRef != null) {
                DocumentSnapshot walletSnap = transaction.get(walletRef);
                balance = MoneyValueParser.toLong(walletSnap.get("currentBalance"));
                if (balance == null) balance = 0L;
            }

            if (txRef != null) {
                transaction.delete(txRef);
            }
            if (walletRef != null) {
                // Hoàn tác: income thì trừ, expense thì cộng
                long change = Transaction.TYPE_INCOME.equals(actualTransaction.getType())
                        ? -actualTransaction.getAmount() : actualTransaction.getAmount();
                transaction.update(walletRef, "currentBalance", balance + change,
                        "updatedAt",
                        com.google.firebase.firestore.FieldValue.serverTimestamp());
            }
            return null;
        });
    }

    private Void deleteTransfer(com.google.firebase.firestore.Transaction transaction,
                                String uid, Transaction t) throws FirebaseFirestoreException {
        String fromId = t.getFromWalletId();
        String toId = t.getToWalletId();
        if (fromId != null) {
            DocumentReference fromRef = db.collection("users").document(uid)
                    .collection("wallets").document(fromId);
            DocumentSnapshot fromSnap = transaction.get(fromRef);
            Long fromBalance = MoneyValueParser.toLong(fromSnap.get("currentBalance"));
            if (fromBalance == null) fromBalance = 0L;
            transaction.update(fromRef, "currentBalance", fromBalance + t.getAmount(),
                    "updatedAt",
                    com.google.firebase.firestore.FieldValue.serverTimestamp());
        }
        if (toId != null) {
            DocumentReference toRef = db.collection("users").document(uid)
                    .collection("wallets").document(toId);
            DocumentSnapshot toSnap = transaction.get(toRef);
            Long toBalance = MoneyValueParser.toLong(toSnap.get("currentBalance"));
            if (toBalance == null) toBalance = 0L;
            transaction.update(toRef, "currentBalance", toBalance - t.getAmount(),
                    "updatedAt",
                    com.google.firebase.firestore.FieldValue.serverTimestamp());
        }
        // Xoá transaction document
        if (t.getId() != null) {
            transaction.delete(db.collection("users").document(uid)
                    .collection("transactions").document(t.getId()));
        }
        return null;
    }

    /**
     * Đọc 1 transaction theo id. Trả về {@link Task} với {@code null} nếu không tồn tại.
     */
    @NonNull
    public Task<Transaction> getTransactionById(String uid, @NonNull String transactionId) {
        return db.collection("users").document(uid)
                .collection("transactions").document(transactionId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return null;
                    DocumentSnapshot snap = task.getResult();
                    if (!snap.exists()) return null;
                    Transaction t = snap.toObject(Transaction.class);
                    if (t != null) t.setId(snap.getId());
                    return t;
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

    /**
     * Quan sát giao dịch trong khoảng thời gian — phiên bản trả về {@link UiState}.
     *
     * <p>Thay vì trả empty list khi có lỗi, phương thức này trả về
     * {@link UiState#error(String)} với thông báo phân biệt được loại lỗi.
     *
     * @param uid user ID
     * @param start ngày bắt đầu (inclusive)
     * @param end ngày kết thúc (exclusive)
     * @return LiveData phát UiState
     */
    public LiveData<UiState<List<Transaction>>> observeRangeWithState(
            @NonNull String uid, @NonNull java.util.Date start, @NonNull java.util.Date end) {
        MediatorLiveData<UiState<List<Transaction>>> live =
                new MediatorLiveData<>();
        live.setValue(UiState.<List<Transaction>>loading());

        LiveData<List<Transaction>> source = observeRange(uid, start, end);
        live.addSource(source, list -> {
            if (list == null || list.isEmpty()) {
                live.setValue(UiState.<List<Transaction>>empty());
            } else {
                live.setValue(UiState.success(list));
            }
            live.removeSource(source);
        });

        return live;
    }
}
