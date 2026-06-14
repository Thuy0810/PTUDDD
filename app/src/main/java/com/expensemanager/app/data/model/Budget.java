package com.expensemanager.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ngân sách tháng (tổng hoặc theo danh mục).
 *
 * <p>Quy ước (ràng buộc 9):
 * <ul>
 *   <li>{@code month} định dạng {@code yyyy-MM}.</li>
 *   <li>{@code limitAmount} là {@code long} (đơn vị VND, không có thập phân).</li>
 *   <li>{@code scope=monthlyTotal}: {@code categoryId} rỗng.</li>
 *   <li>{@code scope=category}: {@code categoryId} bắt buộc.</li>
 *   <li>{@code alertAt}: danh sách tỷ lệ {@code (0, 1]}, không trùng, sắp xếp tăng dần.</li>
 *   <li>Mỗi tháng chỉ có 1 budget tổng; mỗi cặp (tháng, category) chỉ có 1 budget category.</li>
 * </ul>
 */
public class Budget {
    public static final String SCOPE_MONTHLY = "monthlyTotal";
    public static final String SCOPE_CATEGORY = "category";

    @DocumentId
    private String id;
    private String scope;
    private String categoryId;
    private String month;
    private long limitAmount;
    private List<Double> alertAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean isArchived;

    public Budget() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public long getLimitAmount() { return limitAmount; }
    public void setLimitAmount(long limitAmount) { this.limitAmount = limitAmount; }

    public List<Double> getAlertAt() {
        return alertAt != null ? alertAt : new ArrayList<>(Arrays.asList(0.8, 0.9));
    }

    public void setAlertAt(List<Double> alertAt) { this.alertAt = alertAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    /**
     * Trả về bản sao không thể sửa của alertAt.
     */
    public List<Double> getAlertAtReadOnly() {
        return Collections.unmodifiableList(getAlertAt());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("scope", scope);
        if (categoryId != null) map.put("categoryId", categoryId);
        map.put("month", month);
        map.put("limitAmount", limitAmount);
        map.put("alertAt", getAlertAt());
        if (createdAt != null) map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt != null ? updatedAt : Timestamp.now());
        map.put("isArchived", isArchived);
        return map;
    }
}
