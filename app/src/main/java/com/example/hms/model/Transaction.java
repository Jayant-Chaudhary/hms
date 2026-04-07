package com.example.hms.model;

import java.util.List;
import java.util.Map;

public class Transaction {
    private String id;
    private long timestamp;
    private double amount;
    private String type; // "INCOME", "EXPENSE", or "TRANSFER"
    private String categoryId;
    private String remark;

    // The flexible custom fields
    private List<String> tags;
    private Map<String, String> customAttributes;

    // Audit Trail
    private String createdByAdminId;
    private long lastModifiedAt;
    private Double auditedAmount; // Nullable

    // Required empty constructor for Firebase
    public Transaction() {}

    public Transaction(String id, long timestamp, double amount, String type,
                       String categoryId, String remark, List<String> tags,
                       Map<String, String> customAttributes, String createdByAdminId,
                       long lastModifiedAt, Double auditedAmount) {
        this.id = id;
        this.timestamp = timestamp;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.remark = remark;
        this.tags = tags;
        this.customAttributes = customAttributes;
        this.createdByAdminId = createdByAdminId;
        this.lastModifiedAt = lastModifiedAt;
        this.auditedAmount = auditedAmount;
    }

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Map<String, String> getCustomAttributes() { return customAttributes; }
    public void setCustomAttributes(Map<String, String> customAttributes) { this.customAttributes = customAttributes; }

    public String getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; }

    public long getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(long lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }

    public Double getAuditedAmount() { return auditedAmount; }
    public void setAuditedAmount(Double auditedAmount) { this.auditedAmount = auditedAmount; }
}