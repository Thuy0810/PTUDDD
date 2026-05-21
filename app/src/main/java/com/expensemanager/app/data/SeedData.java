package com.expensemanager.app.data;

import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Wallet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public final class SeedData {
    private SeedData() {}

    public static List<Category> defaultCategories() {
        List<Category> list = new ArrayList<>();
        // Expense categories
        list.add(new Category("food", "Ăn uống", Category.TYPE_EXPENSE, "food", "#F97316", true));
        list.add(new Category("transport", "Đi lại", Category.TYPE_EXPENSE, "transport", "#3B82F6", true));
        list.add(new Category("shopping", "Mua sắm", Category.TYPE_EXPENSE, "shopping", "#EC4899", true));
        list.add(new Category("bills", "Hóa đơn", Category.TYPE_EXPENSE, "bills", "#8B5CF6", true));
        list.add(new Category("education", "Học tập", Category.TYPE_EXPENSE, "education", "#14B8A6", true));
        list.add(new Category("entertainment", "Giải trí", Category.TYPE_EXPENSE, "entertainment", "#A855F7", true));
        list.add(new Category("health", "Sức khỏe", Category.TYPE_EXPENSE, "health", "#22C55E", true));
        list.add(new Category("family", "Gia đình", Category.TYPE_EXPENSE, "family", "#EAB308", true));
        list.add(new Category("saving", "Tiết kiệm", Category.TYPE_EXPENSE, "saving", "#3B82F6", true));
        list.add(new Category("expense_other", "Khác", Category.TYPE_EXPENSE, "other", "#6B7280", true));
        // Income categories
        list.add(new Category("salary", "Lương", Category.TYPE_INCOME, "salary", "#10B981", true));
        list.add(new Category("bonus", "Thưởng", Category.TYPE_INCOME, "bonus", "#06B6D4", true));
        list.add(new Category("gift", "Tiền được cho", Category.TYPE_INCOME, "gift", "#F59E0B", true));
        list.add(new Category("sales", "Bán hàng", Category.TYPE_INCOME, "sales", "#6366F1", true));
        list.add(new Category("income_other", "Khác", Category.TYPE_INCOME, "other", "#6B7280", true));
        return list;
    }

    public static List<Wallet> defaultWallets() {
        List<Wallet> list = new ArrayList<>();
        Wallet cash = new Wallet("cash", "Tiền mặt", "cash", 0);
        cash.setCurrentBalance(0);
        cash.setIcon("wallet");
        cash.setColor("#FFB84D");
        list.add(cash);

        Wallet bank = new Wallet("bank", "Ngân hàng", "bank", 0);
        bank.setCurrentBalance(0);
        bank.setIcon("bank");
        bank.setColor("#3B82F6");
        list.add(bank);

        Wallet ewallet = new Wallet("ewallet", "Ví điện tử", "ewallet", 0);
        ewallet.setCurrentBalance(0);
        ewallet.setIcon("smartphone");
        ewallet.setColor("#22C55E");
        list.add(ewallet);

        Wallet saving = new Wallet("saving", "Tiết kiệm", "savings", 0);
        saving.setCurrentBalance(0);
        saving.setIcon("piggy");
        saving.setColor("#A855F7");
        list.add(saving);
        return list;
    }

    public static void seedIfNeeded(String uid, Runnable onComplete, Runnable onError) {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("categories")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        onComplete.run();
                        return;
                    }
                    for (Category c : defaultCategories()) {
                        FirebaseFirestore.getInstance()
                                .collection("users").document(uid).collection("categories")
                                .document(c.getId()).set(c.toMap());
                    }
                    for (Wallet w : defaultWallets()) {
                        FirebaseFirestore.getInstance()
                                .collection("users").document(uid).collection("wallets")
                                .document(w.getId()).set(w.toMap());
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> onError.run());
    }

    public static Task<Void> seedIfNeeded(String uid) {
        return Tasks.call(() -> {
            final Object lock = new Object();
            final boolean[] done = {false};
            final Exception[] err = {null};
            seedIfNeeded(uid, () -> {
                synchronized (lock) { done[0] = true; lock.notify(); }
            }, () -> {
                synchronized (lock) {
                    err[0] = new Exception("Seed failed");
                    done[0] = true;
                    lock.notify();
                }
            });
            synchronized (lock) {
                while (!done[0]) lock.wait(15000);
            }
            if (err[0] != null) throw err[0];
            return null;
        });
    }
}
