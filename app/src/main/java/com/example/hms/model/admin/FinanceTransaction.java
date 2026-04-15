package com.example.hms.model.admin;

import com.google.firebase.Timestamp;

public class FinanceTransaction {
    public String id;
    public String type;
    public double amount;
    public String category;
    public String subCategory;
    public String microCategory;
    public String note;
    public String remark;
    public String sourceBookingId;
    public Timestamp date;
    public String monthKey;

    public FinanceTransaction() {
    }
}
