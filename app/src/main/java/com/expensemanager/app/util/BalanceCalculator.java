package com.expensemanager.app.util;

import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;

import java.util.List;

public final class BalanceCalculator {
    private BalanceCalculator() {}

    public static double walletBalance(Wallet wallet, List<Transaction> all) {
        // Uu tien su dung currentBalance da duoc cap nhat truc tiep
        double balance = wallet.getCurrentBalance();
        if (balance != 0) return balance;
        // Fallback: tinh lai tu lich su giao dich
        balance = wallet.getInitialBalance();
        String wid = wallet.getId();
        for (Transaction t : all) {
            if (Transaction.TYPE_TRANSFER.equals(t.getType())) {
                if (wid.equals(t.getFromWalletId())) balance -= t.getAmount();
                if (wid.equals(t.getToWalletId())) balance += t.getAmount();
            } else if (wid.equals(t.getWalletId())) {
                if (Transaction.TYPE_INCOME.equals(t.getType())) {
                    balance += t.getAmount();
                } else if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                    balance -= t.getAmount();
                }
            }
        }
        return balance;
    }

    public static double totalAssets(List<Wallet> wallets, List<Transaction> all) {
        double total = 0;
        for (Wallet w : wallets) {
            total += walletBalance(w, all);
        }
        return total;
    }
}
