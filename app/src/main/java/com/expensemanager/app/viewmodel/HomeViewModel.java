package com.expensemanager.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.FinancialInsights;
import com.expensemanager.app.data.model.HomeSummary;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.BudgetRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.util.BalanceCalculator;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.InsightsEngine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends ViewModel {
    private final AuthRepository authRepo = new AuthRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final BudgetRepository budgetRepo = new BudgetRepository();

    private final MediatorLiveData<HomeSummary> summary = new MediatorLiveData<>();
    private final MediatorLiveData<FinancialInsights> insights = new MediatorLiveData<>();
    private final MediatorLiveData<List<String>> budgetAlerts = new MediatorLiveData<>();
    private final MediatorLiveData<List<Transaction>> recentTransactions = new MediatorLiveData<>();
    private final MediatorLiveData<Map<String, Category>> categoryMap = new MediatorLiveData<>();
    private final MediatorLiveData<Map<String, Wallet>> walletMap = new MediatorLiveData<>();

    private List<Transaction> monthTx = new ArrayList<>();
    private List<Transaction> allTx = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Budget> budgets = new ArrayList<>();

    public HomeViewModel() {
        String uid = authRepo.getUid();
        if (uid == null) return;
        String month = DateUtils.currentMonthKey();

        LiveData<List<Transaction>> monthLive = txRepo.observeMonth(uid, month);
        LiveData<List<Transaction>> allLive = txRepo.observeAll(uid);
        LiveData<List<Wallet>> walletLive = walletRepo.observeAll(uid);
        LiveData<List<Category>> catLive = categoryRepo.observeAll(uid);
        LiveData<List<Budget>> budgetLive = budgetRepo.observeMonth(uid, month);

        summary.addSource(monthLive, list -> { monthTx = list != null ? list : new ArrayList<>(); recompute(); });
        summary.addSource(allLive, list -> { allTx = list != null ? list : new ArrayList<>(); recompute(); });
        summary.addSource(walletLive, list -> { wallets = list != null ? list : new ArrayList<>(); recompute(); });
        summary.addSource(catLive, list -> { categories = list != null ? list : new ArrayList<>(); recompute(); });
        summary.addSource(budgetLive, list -> { budgets = list != null ? list : new ArrayList<>(); recompute(); });
    }

    private void recompute() {
        double income = 0, expense = 0, todayExpense = 0;
        Date today = new Date();
        Map<String, Double> catExpense = new HashMap<>();

        for (Transaction t : monthTx) {
            if (Transaction.TYPE_INCOME.equals(t.getType())) income += t.getAmount();
            if (Transaction.TYPE_EXPENSE.equals(t.getType())) {
                expense += t.getAmount();
                catExpense.put(t.getCategoryId(),
                        catExpense.getOrDefault(t.getCategoryId(), 0.0) + t.getAmount());
            }
            if (Transaction.TYPE_EXPENSE.equals(t.getType())
                    && DateUtils.isSameDay(t.getDateAsDate(), today)) {
                todayExpense += t.getAmount();
            }
        }

        long balance = BalanceCalculator.totalAssets(wallets, allTx);
        String topName = "";
        double topAmt = 0;
        Map<String, Category> catMap = CategoryRepository.toMap(categories);
        for (Map.Entry<String, Double> e : catExpense.entrySet()) {
            if (e.getValue() > topAmt) {
                topAmt = e.getValue();
                Category c = catMap.get(e.getKey());
                topName = c != null ? c.getName() : e.getKey();
            }
        }

        summary.setValue(new HomeSummary(balance, income, expense, todayExpense, topName, topAmt));

        long monthlyLimit = 0L;
        for (Budget b : budgets) {
            if (com.expensemanager.app.data.model.Budget.SCOPE_MONTHLY.equals(b.getScope())) {
                monthlyLimit = b.getLimitAmount();
                break;
            }
        }

        List<Transaction> lastMonth = new ArrayList<>();
        List<Transaction> last7 = new ArrayList<>();
        Calendar cal = DateUtils.newCalendar();
        cal.add(Calendar.MONTH, -1);
        String lastMonthKey = DateUtils.monthKey(cal.getTime());
        cal = DateUtils.newCalendar();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date weekAgo = cal.getTime();

        for (Transaction t : allTx) {
            if (DateUtils.monthKey(t.getDateAsDate()).equals(lastMonthKey)) {
                lastMonth.add(t);
            }
            if (t.getDateAsDate().after(weekAgo)) last7.add(t);
        }

        insights.setValue(InsightsEngine.compute(
                monthTx, lastMonth, last7, budgets, categories, balance, monthlyLimit));

        budgetAlerts.setValue(com.expensemanager.app.util.BudgetChecker.checkAlerts(
                budgets, TransactionRepository.expensesOnly(monthTx)));

        // Most recent 5 transactions
        List<Transaction> sorted = new ArrayList<>(allTx);
        sorted.sort((a, b) -> {
            Date da = a.getDateAsDate();
            Date db = b.getDateAsDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });
        recentTransactions.setValue(sorted.size() > 5 ? sorted.subList(0, 5) : sorted);

        // Map danh mục / ví để adapter tra cứu tên & icon (tránh hiển thị "đã xóa").
        categoryMap.setValue(catMap);
        Map<String, Wallet> wMap = new HashMap<>();
        for (Wallet w : wallets) {
            if (w != null && w.getId() != null) wMap.put(w.getId(), w);
        }
        walletMap.setValue(wMap);
    }

    public LiveData<HomeSummary> getSummary() { return summary; }
    public LiveData<FinancialInsights> getInsights() { return insights; }
    public LiveData<List<String>> getBudgetAlerts() { return budgetAlerts; }
    public LiveData<List<Transaction>> getRecentTransactions() { return recentTransactions; }
    public LiveData<Map<String, Category>> getCategoryMap() { return categoryMap; }
    public LiveData<Map<String, Wallet>> getWalletMap() { return walletMap; }
}
