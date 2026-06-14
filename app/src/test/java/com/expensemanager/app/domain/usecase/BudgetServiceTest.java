package com.expensemanager.app.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Transaction;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BudgetServiceTest {

    // ---------------------- validate ----------------------

    @Test
    public void validate_validMonthlyBudget() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 5_000_000L);
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertTrue(r.valid);
    }

    @Test
    public void validate_validCategoryBudget() {
        Budget b = newBudget(Budget.SCOPE_CATEGORY, "food", "2026-06", 1_500_000L);
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertTrue(r.valid);
    }

    @Test
    public void validate_invalidMonthFormat() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "06-2026", 5_000_000L);
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);
        assertNotNull(r.errorMessage);
    }

    @Test
    public void validate_zeroLimitFails() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 0L);
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);
    }

    @Test
    public void validate_negativeLimitFails() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", -100L);
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);
    }

    @Test
    public void validate_categoryBudgetMissingCategoryIdFails() {
        Budget b = newBudget(Budget.SCOPE_CATEGORY, null, "2026-06", 1_000_000L);
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);
    }

    @Test
    public void validate_monthlyBudgetWithCategoryIdFails() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, "food", "2026-06", 5_000_000L);
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);
    }

    @Test
    public void validate_duplicateMonthlyFails() {
        Budget existing = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 5_000_000L);
        existing.setId("budget-existing");
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 6_000_000L);
        BudgetService.ValidationResult r = BudgetService.validate(b, Arrays.asList(existing));
        assertFalse(r.valid);
    }

    @Test
    public void validate_duplicateCategoryFails() {
        Budget existing = newBudget(Budget.SCOPE_CATEGORY, "food", "2026-06", 1_000_000L);
        existing.setId("budget-existing");
        Budget b = newBudget(Budget.SCOPE_CATEGORY, "food", "2026-06", 2_000_000L);
        BudgetService.ValidationResult r = BudgetService.validate(b, Arrays.asList(existing));
        assertFalse(r.valid);
    }

    @Test
    public void validate_sameBudgetSkipsItself() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 5_000_000L);
        b.setId("same-id");
        BudgetService.ValidationResult r = BudgetService.validate(b, Arrays.asList(b));
        assertTrue(r.valid);
    }

    @Test
    public void validate_archivedExistingDoesNotBlock() {
        Budget existing = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 5_000_000L);
        existing.setId("budget-existing");
        existing.setArchived(true);
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 6_000_000L);
        BudgetService.ValidationResult r = BudgetService.validate(b, Arrays.asList(existing));
        assertTrue(r.valid);
    }

    @Test
    public void validate_alertAtSortedFails() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 5_000_000L);
        b.setAlertAt(Arrays.asList(0.9, 0.8));
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);
    }

    @Test
    public void validate_alertAtDuplicateFails() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 5_000_000L);
        b.setAlertAt(Arrays.asList(0.8, 0.8));
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);
    }

    @Test
    public void validate_alertAtOutOfRangeFails() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 5_000_000L);
        b.setAlertAt(Arrays.asList(0.0));
        BudgetService.ValidationResult r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);

        b.setAlertAt(Arrays.asList(1.5));
        r = BudgetService.validate(b, new ArrayList<>());
        assertFalse(r.valid);
    }

    // ---------------------- computeUsage ----------------------

    @Test
    public void sumUsed_monthlyIncludesAllExpenses() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 5_000_000L);
        List<Transaction> txs = Arrays.asList(
                newTransaction("expense", "food", 1_000_000L, "2026-06-15"),
                newTransaction("expense", "transport", 500_000L, "2026-06-20"),
                newTransaction("income", "salary", 10_000_000L, "2026-06-01"),
                newTransaction("transfer", null, 200_000L, "2026-06-10")
        );
        assertEquals(1_500_000L, BudgetService.sumUsed(b, txs));
    }

    @Test
    public void sumUsed_categoryFilters() {
        Budget b = newBudget(Budget.SCOPE_CATEGORY, "food", "2026-06", 2_000_000L);
        List<Transaction> txs = Arrays.asList(
                newTransaction("expense", "food", 1_000_000L, "2026-06-15"),
                newTransaction("expense", "transport", 500_000L, "2026-06-20")
        );
        assertEquals(1_000_000L, BudgetService.sumUsed(b, txs));
    }

    @Test
    public void computeUsage_exceeded() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 1_000_000L);
        List<Transaction> txs = Arrays.asList(
                newTransaction("expense", "food", 1_500_000L, "2026-06-15")
        );
        BudgetService.Usage u = BudgetService.computeUsage(b, txs);
        assertEquals(1_000_000L, u.limitAmount);
        assertEquals(1_500_000L, u.usedAmount);
        assertEquals(-500_000L, u.remainingAmount);
        assertTrue(u.isExceeded());
    }

    // ---------------------- checkAlerts ----------------------

    @Test
    public void checkAlerts_underThresholdEmpty() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 1_000_000L);
        b.setAlertAt(Arrays.asList(0.8, 0.9));
        List<Transaction> txs = Arrays.asList(
                newTransaction("expense", "food", 100_000L, "2026-06-15")
        );
        assertTrue(BudgetService.checkAlerts(b, txs).isEmpty());
    }

    @Test
    public void checkAlerts_80Percent() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 1_000_000L);
        b.setAlertAt(Arrays.asList(0.8, 0.9));
        List<Transaction> txs = Arrays.asList(
                newTransaction("expense", "food", 800_000L, "2026-06-15")
        );
        List<String> alerts = BudgetService.checkAlerts(b, txs);
        assertFalse(alerts.isEmpty());
    }

    @Test
    public void checkAlerts_exceededMessage() {
        Budget b = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 1_000_000L);
        b.setAlertAt(Arrays.asList(0.8, 0.9));
        List<Transaction> txs = Arrays.asList(
                newTransaction("expense", "food", 1_200_000L, "2026-06-15")
        );
        List<String> alerts = BudgetService.checkAlerts(b, txs);
        assertFalse(alerts.isEmpty());
        assertTrue(alerts.get(0).contains("Vượt"));
    }

    // ---------------------- normalizeAlertAt ----------------------

    @Test
    public void normalizeAlertAt_emptyReturnsDefault() {
        assertEquals(1, BudgetService.normalizeAlertAt(new ArrayList<>()).size());
    }

    @Test
    public void normalizeAlertAt_clampsToValidRange() {
        List<Double> result = BudgetService.normalizeAlertAt(Arrays.asList(0.0, 1.5, 0.8));
        assertEquals(2, result.size());
        assertTrue(result.get(0) > 0);
        assertTrue(result.get(1) <= 1.0);
    }

    @Test
    public void normalizeAlertAt_dedupesAndSorts() {
        List<Double> result = BudgetService.normalizeAlertAt(Arrays.asList(0.9, 0.5, 0.9, 0.5));
        assertEquals(2, result.size());
        assertTrue(result.get(0) < result.get(1));
    }

    // ---------------------- sumLimits ----------------------

    @Test
    public void sumLimits_skipsArchived() {
        Budget active = newBudget(Budget.SCOPE_MONTHLY, null, "2026-06", 1_000_000L);
        Budget archived = newBudget(Budget.SCOPE_CATEGORY, "food", "2026-06", 500_000L);
        archived.setArchived(true);
        assertEquals(1_000_000L, BudgetService.sumLimits(Arrays.asList(active, archived)));
    }

    // ---------------------- helpers ----------------------

    private static Budget newBudget(String scope, String categoryId, String month, long limit) {
        Budget b = new Budget();
        b.setScope(scope);
        b.setCategoryId(categoryId);
        b.setMonth(month);
        b.setLimitAmount(limit);
        b.setAlertAt(new ArrayList<>(Arrays.asList(0.8, 0.9)));
        return b;
    }

    private static Transaction newTransaction(String type, String categoryId, long amount, String date) {
        Transaction t = new Transaction();
        t.setType(type);
        t.setCategoryId(categoryId);
        t.setAmount(amount);
        try {
            String[] parts = date.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            t.setDate(new Timestamp(new Date(year - 1900, month - 1, day)));
        } catch (Exception e) {
            t.setDate(Timestamp.now());
        }
        return t;
    }
}
