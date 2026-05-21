package com.expensemanager.app.util;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public final class BackupManager {
    private BackupManager() {}

    public static void exportUserData(Context ctx, String uid, Runnable onDone, Runnable onError) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> backup = new HashMap<>();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    backup.put("profile", userDoc.getData());
                    db.collection("users").document(uid).collection("transactions").get()
                            .addOnSuccessListener(txSnap -> {
                                backup.put("transactions", txSnap.getDocuments());
                                try {
                                    JSONObject json = new JSONObject(backup);
                                    File dir = ctx.getCacheDir();
                                    File file = new File(dir, "expense_backup.json");
                                    FileWriter w = new FileWriter(file);
                                    w.write(json.toString(2));
                                    w.close();
                                    Intent share = new Intent(Intent.ACTION_SEND);
                                    share.setType("application/json");
                                    share.putExtra(Intent.EXTRA_STREAM,
                                            FileProvider.getUriForFile(ctx,
                                                    ctx.getPackageName() + ".fileprovider", file));
                                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    ctx.startActivity(Intent.createChooser(share, "Sao lưu dữ liệu"));
                                    onDone.run();
                                } catch (Exception e) {
                                    onError.run();
                                }
                            })
                            .addOnFailureListener(e -> onError.run());
                })
                .addOnFailureListener(e -> onError.run());
    }
}
