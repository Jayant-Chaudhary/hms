package com.example.hms.utils.admin;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AdminFirestoreRepository {

    public static final String COL_FINANCE = "finance_transactions";
    public static final String COL_ROOMS = "rooms";
    public static final String COL_AUTH_USERS = "authorized_users";
    public static final String COL_BOOKINGS = "bookings";
    public static final String COL_CUSTOMERS = "customers";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CollectionReference finance() {
        return db.collection(COL_FINANCE);
    }

    public CollectionReference rooms() {
        return db.collection(COL_ROOMS);
    }

    public CollectionReference authorizedUsers() {
        return db.collection(COL_AUTH_USERS);
    }

    public CollectionReference bookings() {
        return db.collection(COL_BOOKINGS);
    }

    public CollectionReference customers() {
        return db.collection(COL_CUSTOMERS);
    }

    public Query financeByMonth(String monthKey) {
        return finance().whereEqualTo("monthKey", monthKey).orderBy("date", Query.Direction.DESCENDING);
    }

    public static String monthKeyNow() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    }

    public static Timestamp nowTimestamp() {
        return Timestamp.now();
    }
}
