package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Transaction {
    public static final String TYPE_INCOME = "income";
    public static final String TYPE_EXPENSE = "expense";

    @DocumentId
    private String id;
    private String type;
    private long amount;
    private String categoryId;
    private String walletId;
    private String note;
    private Timestamp date;
    private String mood;
    private String regretFlag;
    private String recurringRuleId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Transaction() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getNote() { return note != null ? note : ""; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public Date getDateAsDate() {
        return date != null ? date.toDate() : new Date();
    }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getRegretFlag() { return regretFlag; }
    public void setRegretFlag(String regretFlag) { this.regretFlag = regretFlag; }

    public String getRecurringRuleId() { return recurringRuleId; }
    public void setRecurringRuleId(String recurringRuleId) { this.recurringRuleId = recurringRuleId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isIncome()    { return TYPE_INCOME.equals(type); }
    public boolean isExpense()   { return TYPE_EXPENSE.equals(type); }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("amount", amount);
        map.put("categoryId", categoryId != null ? categoryId : "");
        map.put("walletId", walletId != null ? walletId : "");
        map.put("note", getNote());
        map.put("date", date != null ? date : Timestamp.now());
        if (mood != null) map.put("mood", mood);
        if (regretFlag != null)         map.put("regretFlag", regretFlag);
        if (recurringRuleId != null) map.put("recurringRuleId", recurringRuleId);
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        map.put("updatedAt", Timestamp.now());
        return map;
    }
}
