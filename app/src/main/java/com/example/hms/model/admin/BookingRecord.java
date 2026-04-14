package com.example.hms.model.admin;

import com.google.firebase.Timestamp;

import java.util.List;

public class BookingRecord {
    public String id;
    public String customerName;
    public String customerId;
    public List<String> rooms;
    public double totalAmount;
    public Timestamp checkIn;
    public Timestamp checkOut;
    public String status;

    public BookingRecord() {
    }
}
