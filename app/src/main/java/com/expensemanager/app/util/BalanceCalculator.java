package com.expensemanager.app.util;

import androidx.annotation.NonNull;

import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;

import java.util.List;

public final class BalanceCalculator {
    private BalanceCalculator() {}

    /**
     * Tính lại số dư ví từ lịch sử giao dịch — KHÔNG dùng {@code currentBalance} từ cache.
     * Dùng để đối soát với {@code wallet.currentBalance} và phát hiện lệch.
     */
    public static long recomputeBalance(@NonNull Wallet wallet, @NonNull List<Transaction> all) {
        long balance = wallet.getInitialBalance();
        String wid = wallet.getId();
        if (wid == null) return balance;
        for (Transaction t : all) {
            if (wid.equals(t.getWalletId())) {
                if (Transaction.TYPE_INCOME.equals(t.getType())) {
                    balance += t.getAmount();
                } else if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                    balance -= t.getAmount();
                }
            }
        }
        return balance;
    }

    /**
     * Ưu tiên {@code currentBalance} đã được cập nhật trực tiếp.
     * Nếu bằng 0 thì fallback tính từ lịch sử.
     */
    public static long walletBalance(@NonNull Wallet wallet, @NonNull List<Transaction> all) {
        long balance = wallet.getCurrentBalance();
        if (balance != 0L) return balance;
        return recomputeBalance(wallet, all);
    }

    public static long totalAssets(@NonNull List<Wallet> wallets, @NonNull List<Transaction> all) {
        long total = 0L;
        for (Wallet w : wallets) {
            total += walletBalance(w, all);
        }
        return total;
    }

    /**
     * Kết quả đối soát số dư.
     */
    public static class Verification {
        public final long expected;
        public final long actual;
        public final long difference;

        public Verification(long expected, long actual) {
            this.expected = expected;
            this.actual = actual;
            this.difference = expected - actual;
        }

        public boolean isMatch() { return difference == 0L; }
    }

    /**
     * Đối soát {@code currentBalance} với lịch sử giao dịch.
     */
    @NonNull
    public static Verification verify(@NonNull Wallet wallet, @NonNull List<Transaction> all) {
        long expected = recomputeBalance(wallet, all);
        long actual = wallet.getCurrentBalance();
        return new Verification(expected, actual);
    }
}
