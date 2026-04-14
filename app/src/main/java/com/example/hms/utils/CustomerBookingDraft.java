package com.example.hms.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class CustomerBookingDraft {

    private static CustomerBookingDraft instance;

    public static CustomerBookingDraft get() {
        if (instance == null) {
            instance = new CustomerBookingDraft();
        }
        return instance;
    }

    public static void reset() {
        instance = new CustomerBookingDraft();
    }

    public String customerName = "";
    public String gender = "";
    public String mobile = "";
    public String email = "";
    public String govIdType = "";
    public String govIdNumber = "";
    public int adults = 1;
    public int children = 0;
    public long checkInMillis;
    public long checkOutMillis;

    public final List<String> selectedRoomIds = new ArrayList<>();

    public int nights() {
        if (checkOutMillis <= checkInMillis) {
            return 0;
        }
        long diff = checkOutMillis - checkInMillis;
        int n = (int) TimeUnit.MILLISECONDS.toDays(diff);
        return Math.max(n, 1);
    }
}
