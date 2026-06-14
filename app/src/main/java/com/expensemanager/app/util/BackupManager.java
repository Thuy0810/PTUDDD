package com.expensemanager.app.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class BackupManager {
    private static final String TAG = "BackupManager";

    private static final String[] COLLECTIONS = {
            "transactions", "wallets", "categories",
            "budgets", "savings_goals", "savings_challenges", "recurring"
    };

    private BackupManager() {}

    public static void exportUserData(Context ctx, String uid, Runnable onDone, Runnable onError) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        JSONObject backup = new JSONObject();
        AtomicBoolean hasError = new AtomicBoolean(false);
        AtomicInteger remaining = new AtomicInteger(2);

        exportProfile(db, uid, backup, hasError, remaining, () ->
                exportCollections(db, uid, backup, hasError, remaining, () -> {
                    try {
                        File dir = ctx.getCacheDir();
                        File file = new File(dir, "expense_backup_" + System.currentTimeMillis() + ".json");
                        FileWriter w = new FileWriter(file);
                        w.write(backup.toString(2));
                        w.close();

                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("application/json");
                        share.putExtra(Intent.EXTRA_STREAM,
                                FileProvider.getUriForFile(ctx,
                                        ctx.getPackageName() + ".fileprovider", file));
                        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        ctx.startActivity(Intent.createChooser(share, "Sao luu du lieu"));
                        finishOne(remaining, hasError, onDone, onError);
                    } catch (Exception e) {
                        Log.e(TAG, "exportUserData: failed to write file", e);
                        hasError.set(true);
                        finishOne(remaining, hasError, onDone, onError);
                    }
                }, onDone, onError),
                onDone, onError);
    }

    private static void exportProfile(FirebaseFirestore db, String uid, JSONObject backup,
                                      AtomicBoolean hasError,
                                      AtomicInteger remaining,
                                      Runnable onDone, Runnable onError) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    try {
                        if (doc.exists()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                backup.put("profile", new JSONObject(data));
                            }
                        }
                    } catch (Exception ignored) {}
                    finishOne(remaining, hasError, onDone, onError);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "exportProfile: failed", e);
                    hasError.set(true);
                    finishOne(remaining, hasError, onDone, onError);
                });
    }

    private static void exportCollections(FirebaseFirestore db, String uid, JSONObject backup,
                                         AtomicBoolean hasError,
                                         AtomicInteger remaining,
                                         Runnable onDone, Runnable onError) {
        AtomicInteger collRemaining = new AtomicInteger(COLLECTIONS.length);

        for (String coll : COLLECTIONS) {
            Query query;
            if ("transactions".equals(coll)) {
                query = db.collection("users").document(uid).collection(coll)
                        .orderBy("date", Query.Direction.DESCENDING);
            } else {
                query = db.collection("users").document(uid).collection(coll);
            }

            query.get()
                    .addOnSuccessListener(snap -> {
                        try {
                            JSONArray arr = new JSONArray();
                            if (snap != null) {
                                for (QueryDocumentSnapshot doc : snap) {
                                    arr.put(serializeDoc(doc));
                                }
                            }
                            backup.put(coll, arr);
                        } catch (Exception ignored) {}

                        if (collRemaining.decrementAndGet() == 0) {
                            finishOne(remaining, hasError, onDone, onError);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "exportCollections: failed to export " + coll, e);
                        hasError.set(true);
                        if (collRemaining.decrementAndGet() == 0) {
                            finishOne(remaining, hasError, onDone, onError);
                        }
                    });
        }
    }

    public static void importUserData(Context ctx, String uid, Uri fileUri,
                                       Runnable onDone, Runnable onError) {
        try {
            java.io.InputStream is = ctx.getContentResolver().openInputStream(fileUri);
            if (is == null) { onError.run(); return; }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject backup = new JSONObject(sb.toString());
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            AtomicBoolean hasError = new AtomicBoolean(false);
            AtomicInteger remaining = new AtomicInteger(COLLECTIONS.length + 1);

            restoreProfile(db, uid, backup, hasError, remaining, onDone, onError);
            for (String coll : COLLECTIONS) {
                restoreCollection(db, uid, coll, backup, hasError, remaining, onDone, onError);
            }
        } catch (Exception e) {
            Log.e(TAG, "importUserData: failed", e);
            onError.run();
        }
    }

    private static void restoreProfile(FirebaseFirestore db, String uid, JSONObject backup,
                                       AtomicBoolean hasError, AtomicInteger remaining,
                                       Runnable onDone, Runnable onError) {
        try {
            if (backup.has("profile")) {
                JSONObject profile = backup.getJSONObject("profile");
                Map<String, Object> data = jsonToMap(profile);
                db.collection("users").document(uid).set(data)
                        .addOnSuccessListener(a -> finishOne(remaining, hasError, onDone, onError))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "restoreProfile: failed", e);
                            hasError.set(true);
                            finishOne(remaining, hasError, onDone, onError);
                        });
            } else {
                finishOne(remaining, hasError, onDone, onError);
            }
        } catch (Exception e) {
            Log.e(TAG, "restoreProfile: exception", e);
            hasError.set(true);
            finishOne(remaining, hasError, onDone, onError);
        }
    }

    private static void restoreCollection(FirebaseFirestore db, String uid, String coll,
                                          JSONObject backup,
                                          AtomicBoolean hasError, AtomicInteger remaining,
                                          Runnable onDone, Runnable onError) {
        try {
            if (!backup.has(coll)) {
                finishOne(remaining, hasError, onDone, onError);
                return;
            }

            JSONArray arr = backup.getJSONArray(coll);
            if (arr.length() == 0) {
                finishOne(remaining, hasError, onDone, onError);
                return;
            }

            int batchSize = 400;
            int totalBatches = (int) Math.ceil((double) arr.length() / batchSize);
            AtomicInteger completedBatches = new AtomicInteger(0);

            for (int batchStart = 0; batchStart < arr.length(); batchStart += batchSize) {
                WriteBatch batch = db.batch();
                int batchEnd = Math.min(batchStart + batchSize, arr.length());

                for (int i = batchStart; i < batchEnd; i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String docId = obj.optString("_id", null);
                    if (docId == null || docId.isEmpty()) continue;

                    Map<String, Object> data = jsonToMap(obj);
                    data.remove("_id");

                    batch.set(db.collection("users").document(uid)
                            .collection(coll).document(docId), data);
                }

                final int thisBatchEnd = batchEnd;
                batch.commit()
                        .addOnSuccessListener(a -> {
                            if (completedBatches.incrementAndGet() >= totalBatches) {
                                finishOne(remaining, hasError, onDone, onError);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "restoreCollection: failed for " + coll, e);
                            hasError.set(true);
                            if (completedBatches.incrementAndGet() >= totalBatches) {
                                finishOne(remaining, hasError, onDone, onError);
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "restoreCollection: exception for " + coll, e);
            hasError.set(true);
            finishOne(remaining, hasError, onDone, onError);
        }
    }

    private static void finishOne(AtomicInteger remaining, AtomicBoolean hasError,
                                  Runnable onDone, Runnable onError) {
        if (remaining.decrementAndGet() == 0) {
            if (hasError.get()) {
                onError.run();
            } else {
                onDone.run();
            }
        }
    }

    private static JSONObject serializeDoc(QueryDocumentSnapshot doc) throws JSONException {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, Object> entry : doc.getData().entrySet()) {
            obj.put(entry.getKey(), serializeValue(entry.getValue()));
        }
        obj.put("_id", doc.getId());
        return obj;
    }

    private static Object serializeValue(Object value) {
        if (value instanceof Timestamp) {
            Timestamp ts = (Timestamp) value;
            JSONObject wrapper = new JSONObject();
            wrapper.put("_type", "timestamp");
            wrapper.put("seconds", ts.getSeconds());
            wrapper.put("nanoseconds", ts.getNanoseconds());
            return wrapper;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonToMap(JSONObject obj) throws JSONException {
        Map<String, Object> map = new java.util.HashMap<>();
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = obj.get(key);
            if (value instanceof JSONObject) {
                value = deserializeValue(jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray) value);
            } else {
                value = deserializeValue(value);
            }
            map.put(key, value);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Object jsonArrayToList(JSONArray arr) throws JSONException {
        ArrayList<Object> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            Object value = arr.get(i);
            if (value instanceof JSONObject) {
                value = deserializeValue(jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray) value);
            } else {
                value = deserializeValue(value);
            }
            list.add(value);
        }
        return list;
    }

    private static Object deserializeValue(Object value) {
        if (value instanceof JSONObject) {
            JSONObject jo = (JSONObject) value;
            if ("timestamp".equals(jo.optString("_type"))) {
                return new Timestamp(jo.getLong("seconds"), jo.getInt("nanoseconds"));
            }
        }
        return value;
    }
}
