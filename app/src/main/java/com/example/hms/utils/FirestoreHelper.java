package com.example.hms.utils;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

public class FirestoreHelper {

    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_RECEPTION = "Receptionist";
    public static final String ROLE_STAFF = "Staff";
    public static final String ROLE_CUSTOMER = "Customer";

    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public CollectionReference getUsersCollection() {
        return db.collection("users");
    }

    public CollectionReference getTransactionsCollection() {
        return db.collection("transactions");
    }

    public CollectionReference getAttendanceCollection() {
        return db.collection("attendance");
    }

    public CollectionReference getUserRolesCollection() {
        return db.collection("user_roles");
    }
}