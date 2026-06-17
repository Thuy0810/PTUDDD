package com.expensemanager.app.data.firebase;

public final class FirestorePaths {
    private FirestorePaths() {}

    public static String user(String uid) {
        return "users/" + uid;
    }

    public static String wallets(String uid) {
        return user(uid) + "/wallets";
    }

    public static String categories(String uid) {
        return user(uid) + "/categories";
    }

    public static String transactions(String uid) {
        return user(uid) + "/transactions";
    }

    public static String budgets(String uid) {
        return user(uid) + "/budgets";
    }

    public static String goals(String uid) {
        return user(uid) + "/savings_goals";
    }

    public static String recurring(String uid) {
        return user(uid) + "/recurring";
    }

    public static String challenges(String uid) {
        return user(uid) + "/challenges";
    }
}
