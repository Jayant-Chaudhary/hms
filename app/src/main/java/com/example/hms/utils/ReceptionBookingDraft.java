package com.example.hms.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Holds in-progress reception booking data between screens. Cleared after payment or on new customer.
 */
public final class ReceptionBookingDraft {

    private static ReceptionBookingDraft instance;

    public static ReceptionBookingDraft get() {
        if (instance == null) {
            instance = new ReceptionBookingDraft();
        }
        return instance;
    }

    public static void reset() {
        instance = new ReceptionBookingDraft();
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
