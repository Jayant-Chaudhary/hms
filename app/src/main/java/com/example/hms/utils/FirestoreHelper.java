package com.example.hms.utils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.hms.model.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirestoreHelper {

    // --- Role Constants ---
    public static final String COL_ROLES = "roles";
    public static final String ROLE_ADMIN      = "admin";
    public static final String ROLE_RECEPTION  = "reception";
    public static final String ROLE_CUSTOMER   = "customer";

    // --- Finance Tracker Constants ---
    public static final String COL_TRANSACTIONS = "Transactions";
    public static final String COL_CATEGORIES = "Categories";

    // --- Firestore Instance ---
    private final FirebaseFirestore db;

    // Constructor to initialize Firestore
    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // --- Callbacks ---
    public interface OnTransactionAddListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    // --- Finance Tracker Methods ---

    /**
     * Adds a new financial transaction to Firestore.
     */
    public void addTransaction(
            double amount,
            String type,
            String categoryId,
            String remark,
            List<String> tags,
            Map<String, String> customAttributes,
            String adminId,
            OnTransactionAddListener listener
    ) {
        String transactionId = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();

        Transaction newTransaction = new Transaction(
                transactionId,
                currentTime,
                amount,
                type,
                categoryId,
                remark,
                tags,
                customAttributes,
                adminId,
                currentTime,
                null // auditedAmount is null upon creation
        );

        db.collection(COL_TRANSACTIONS).document(transactionId)
                .set(newTransaction)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (listener != null) listener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (listener != null) listener.onFailure(e);
                    }
                });
    }

    // You can continue adding your role-based login methods down here...
}