package com.expensemanager.app.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * LiveData gắn với một Firestore {@link Query}.
 *
 * <p>Tự đăng ký snapshot listener khi có observer ({@link #onActive()}) và GỠ listener
 * khi không còn observer ({@link #onInactive()}). Nhờ đó listener không bị rò rỉ và
 * không tiếp tục đọc Firestore (tốn chi phí) khi màn hình không hiển thị.
 *
 * @param <T> kiểu phần tử sau khi parse mỗi document
 */
public class FirestoreQueryLiveData<T> extends LiveData<List<T>> {

    private static final String TAG = "FirestoreQueryLiveData";

    /** Parse một document sang model; trả về {@code null} để bỏ qua phần tử. */
    public interface Parser<T> {
        T parse(@NonNull QueryDocumentSnapshot doc);
    }

    private final Query query;
    private final Parser<T> parser;
    private ListenerRegistration registration;

    public FirestoreQueryLiveData(@NonNull Query query, @NonNull Parser<T> parser) {
        this.query = query;
        this.parser = parser;
    }

    @Override
    protected void onActive() {
        registration = query.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "snapshot listen failed", e);
                setValue(new ArrayList<>());
                return;
            }
            List<T> list = new ArrayList<>();
            if (snap != null) {
                for (QueryDocumentSnapshot doc : snap) {
                    try {
                        T item = parser.parse(doc);
                        if (item != null) list.add(item);
                    } catch (Exception ex) {
                        Log.e(TAG, "parse failed for doc " + doc.getId(), ex);
                    }
                }
            }
            setValue(list);
        });
    }

    @Override
    protected void onInactive() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
