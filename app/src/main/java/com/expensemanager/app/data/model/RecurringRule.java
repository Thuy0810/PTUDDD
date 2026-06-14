package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

/**
 * Giao dịch định kỳ.
 *
 * <p>Hỗ trợ INCOME và EXPENSE. Không hỗ trợ TRANSFER trong phiên bản hiện tại.
 *
 * <p>Ngày trong tháng dùng {@link #useLastDayOfMonth} thay vì magic number 32.
 */
public class RecurringRule {
    public static final String CYCLE_DAILY = "daily";
    public static final String CYCLE_WEEKLY = "weekly";
    public static final String CYCLE_MONTHLY = "monthly";
    public static final String CYCLE_YEARLY = "yearly";

    @DocumentId
    private String id;
    private String type;           // INCOME hoặc EXPENSE
    private long amount;
    private String categoryId;
    private String walletId;
    private String note;
    private String cycleType;
    /** Ngày trong tháng (1-31). 0 = không có. */
    private int dayOfMonth;
    /** Thứ trong tuần (1=CN, 2=T2, ..., 7=T7). 0 = không có. */
    private int dayOfWeek;
    /** Tháng trong năm (1-12). 0 = không có. */
    private int monthOfYear;
    /** Dùng ngày cuối tháng thay vì dayOfMonth=32. */
    private boolean useLastDayOfMonth;
    private Timestamp dateStart;
    private Timestamp dateEnd;
    private boolean enabled;
    /** Thời điểm chạy tiếp theo. */
    private Timestamp nextRun;
    /** Thời điểm chạy lần cuối. */
    private Timestamp lastRun;

    public RecurringRule() {}

    public String getId()                        { return id; }
    public void setId(String id)                  { this.id = id; }

    public String getType()                      { return type; }
    public void setType(String type)            { this.type = type; }

    public long getAmount()                      { return amount; }
    public void setAmount(long amount)          { this.amount = amount; }

    public String getCategoryId()                { return categoryId; }
    public void setCategoryId(String id)        { this.categoryId = id; }

    public String getWalletId()                 { return walletId; }
    public void setWalletId(String id)           { this.walletId = id; }

    public String getNote()                     { return note; }
    public void setNote(String note)            { this.note = note; }

    public String getCycleType()                { return cycleType; }
    public void setCycleType(String t)          { this.cycleType = t; }

    /** @deprecated Dùng {@link #isUseLastDayOfMonth()} thay vì kiểm tra {@code dayOfMonth == 32}. */
    @Deprecated
    public int getDayOfMonth()                  { return dayOfMonth; }
    /** @deprecated Dùng {@link #setUseLastDayOfMonth(boolean)}. */
    @Deprecated
    public void setDayOfMonth(int d)           { this.dayOfMonth = d; }

    public int getDayOfWeek()                   { return dayOfWeek; }
    public void setDayOfWeek(int d)            { this.dayOfWeek = d; }

    public int getMonthOfYear()                 { return monthOfYear; }
    public void setMonthOfYear(int m)          { this.monthOfYear = m; }

    public boolean isUseLastDayOfMonth()        { return useLastDayOfMonth; }
    public void setUseLastDayOfMonth(boolean b) { this.useLastDayOfMonth = b; }

    public boolean isEnabled()                  { return enabled; }
    public void setEnabled(boolean e)           { this.enabled = e; }

    public Timestamp getNextRun()               { return nextRun; }
    public void setNextRun(Timestamp t)        { this.nextRun = t; }

    public Timestamp getLastRun()               { return lastRun; }
    public void setLastRun(Timestamp t)        { this.lastRun = t; }

    public Timestamp getDateStart()             { return dateStart; }
    public void setDateStart(Timestamp t)       { this.dateStart = t; }

    public Timestamp getDateEnd()               { return dateEnd; }
    public void setDateEnd(Timestamp t)         { this.dateEnd = t; }

    public boolean isIncome()   { return "income".equals(type); }
    public boolean isExpense()  { return "expense".equals(type); }

    /**
     * Tạo occurrence ID dùng làm document ID hoặc trường chống trùng.
     * Format: {@code ruleId_scheduledDate}
     */
    public String makeOccurrenceId(Timestamp scheduledRun) {
        String dateStr = scheduledRun != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(scheduledRun.toDate())
                : "unknown";
        return (id != null ? id : "new") + "_" + dateStr;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type != null ? type : Transaction.TYPE_EXPENSE);
        map.put("amount", amount);
        map.put("categoryId", categoryId != null ? categoryId : "");
        map.put("walletId", walletId != null ? walletId : "");
        map.put("note", note != null ? note : "");
        map.put("cycleType", cycleType != null ? cycleType : CYCLE_MONTHLY);
        map.put("dayOfMonth", dayOfMonth);
        map.put("dayOfWeek", dayOfWeek);
        map.put("monthOfYear", monthOfYear);
        map.put("useLastDayOfMonth", useLastDayOfMonth);
        if (dateStart != null) map.put("dateStart", dateStart);
        if (dateEnd != null)   map.put("dateEnd", dateEnd);
        map.put("enabled", enabled);
        if (nextRun != null) map.put("nextRun", nextRun);
        if (lastRun != null) map.put("lastRun", lastRun);
        return map;
    }
}
