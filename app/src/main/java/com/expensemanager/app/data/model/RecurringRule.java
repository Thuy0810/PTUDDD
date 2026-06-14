package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

public class RecurringRule {
    public static final String CYCLE_DAILY = "daily";
    public static final String CYCLE_WEEKLY = "weekly";
    public static final String CYCLE_MONTHLY = "monthly";
    public static final String CYCLE_YEARLY = "yearly";

    @DocumentId
    private String id;
    private String type;
    private long amount;
    private String categoryId;
    private String walletId;
    private String note;
    private String cycleType;
    private int dayOfMonth;
    private int dayOfWeek;
    private int monthOfYear;
    private Timestamp dateStart;
    private Timestamp dateEnd;
    private boolean enabled;
    private Timestamp nextRun;

    public RecurringRule() {}

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

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Timestamp getNextRun() { return nextRun; }
    public void setNextRun(Timestamp nextRun) { this.nextRun = nextRun; }

    public String getCycleType() { return cycleType; }
    public void setCycleType(String cycleType) { this.cycleType = cycleType; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getMonthOfYear() { return monthOfYear; }
    public void setMonthOfYear(int monthOfYear) { this.monthOfYear = monthOfYear; }

    public Timestamp getDateStart() { return dateStart; }
    public void setDateStart(Timestamp dateStart) { this.dateStart = dateStart; }

    public Timestamp getDateEnd() { return dateEnd; }
    public void setDateEnd(Timestamp dateEnd) { this.dateEnd = dateEnd; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("amount", amount);
        map.put("categoryId", categoryId);
        map.put("walletId", walletId);
        map.put("note", note != null ? note : "");
        map.put("cycleType", cycleType != null ? cycleType : CYCLE_MONTHLY);
        map.put("dayOfMonth", dayOfMonth);
        map.put("dayOfWeek", dayOfWeek);
        map.put("monthOfYear", monthOfYear);
        if (dateStart != null) map.put("dateStart", dateStart);
        if (dateEnd != null) map.put("dateEnd", dateEnd);
        map.put("enabled", enabled);
        if (nextRun != null) map.put("nextRun", nextRun);
        return map;
    }
}
