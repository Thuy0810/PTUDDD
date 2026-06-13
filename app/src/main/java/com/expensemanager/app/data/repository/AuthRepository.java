package com.expensemanager.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.expensemanager.app.data.SeedData;
import com.expensemanager.app.data.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getUid() {
        FirebaseUser u = getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    public Task<Void> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new Exception("Đăng nhập thất bại");
                    }
                    return seedForCurrentUser();
                });
    }

    public Task<Void> register(String email, String password, String displayName) {
        return auth.createUserWithEmailAndPassword(email, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new Exception("Đăng ký thất bại");
                    }
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) throw new Exception("Không tìm thấy thông tin người dùng");
                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName).build();
                    return user.updateProfile(profile);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new Exception("Cập nhật hồ sơ thất bại");
                    }
                    String uid = getUid();
                    if (uid == null) throw new Exception("Không tìm thấy ID người dùng");
                    UserProfile p = new UserProfile();
                    p.setDisplayName(displayName);
                    p.setEmail(email);
                    return db.collection("users").document(uid).set(p.toMap());
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null ? task.getException() : new Exception("Lưu thông tin người dùng thất bại");
                    }
                    return seedForCurrentUser();
                });
    }

    private Task<Void> seedForCurrentUser() {
        String uid = getUid();
        if (uid == null) return Tasks.forException(new Exception("No user"));
        return SeedData.seedIfNeeded(uid);
    }

    public void logout() {
        auth.signOut();
    }

    public Task<Void> updateDisplayName(String name) {
        String uid = getUid();
        if (uid == null) return Tasks.forException(new Exception("Not logged in"));
        Map<String, Object> map = new HashMap<>();
        map.put("displayName", name);
        FirebaseUser user = getCurrentUser();
        Task<Void> firestore = db.collection("users").document(uid).update(map);
        if (user != null) {
            UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name).build();
            return user.updateProfile(req).continueWithTask(t -> firestore);
        }
        return firestore;
    }

    public Task<Void> changePassword(String newPassword) {
        FirebaseUser user = getCurrentUser();
        if (user == null) return Tasks.forException(new Exception("Not logged in"));
        return user.updatePassword(newPassword);
    }

    public LiveData<UserProfile> observeProfile() {
        MutableLiveData<UserProfile> live = new MutableLiveData<>();
        String uid = getUid();
        if (uid == null) return live;
        db.collection("users").document(uid).addSnapshotListener((snap, e) -> {
            if (snap != null && snap.exists()) {
                UserProfile p = snap.toObject(UserProfile.class);
                live.setValue(p);
            }
        });
        return live;
    }
}
