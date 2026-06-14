package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

/**
 * Bản ghi phân bổ lại ngân sách.
 */
public class BudgetReallocation {
    @DocumentId
    private String id;

    private String month;
    private String sourceType;   // UNALLOCATED | OTHER_BUDGET
    private String sourceBudgetId;
    private String targetBudgetId;
    private long amount;
    private Timestamp createdAt;
    private String reason;

    public BudgetReallocation() {}

    public String getId()                              { return id; }
    public void setId(String id)                      { this.id = id; }

    public String getMonth()                           { return month; }
    public void setMonth(String month)                { this.month = month; }

    public String getSourceType()                     { return sourceType; }
    public void setSourceType(String t)              { this.sourceType = t; }

    public String getSourceBudgetId()                 { return sourceBudgetId; }
    public void setSourceBudgetId(String id)         { this.sourceBudgetId = id; }

    public String getTargetBudgetId()                { return targetBudgetId; }
    public void setTargetBudgetId(String id)        { this.targetBudgetId = id; }

    public long getAmount()                           { return amount; }
    public void setAmount(long a)                   { this.amount = a; }

    public Timestamp getCreatedAt()                    { return createdAt; }
    public void setCreatedAt(Timestamp t)           { this.createdAt = t; }

    public String getReason()                        { return reason; }
    public void setReason(String r)                  { this.reason = r; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("month", month);
        map.put("sourceType", sourceType);
        if (sourceBudgetId != null) map.put("sourceBudgetId", sourceBudgetId);
        if (targetBudgetId != null) map.put("targetBudgetId", targetBudgetId);
        map.put("amount", amount);
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        if (reason != null) map.put("reason", reason);
        return map;
    }
}
