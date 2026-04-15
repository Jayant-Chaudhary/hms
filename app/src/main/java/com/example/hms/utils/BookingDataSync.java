package com.example.hms.utils;

import com.example.hms.model.HotelRoom;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BookingDataSync {

    private BookingDataSync() {}

    public static Task<Void> saveBookingAndCustomer(
            String txnRef,
            String customerName,
            String email,
            String mobile,
            String govIdType,
            String govIdNumber,
            int adults,
            int children,
            long checkInMillis,
            long checkOutMillis,
            List<String> roomIds,
            List<HotelRoom> roomInventory,
            double totalAmount,
            String paymentMethod,
            String createdByRole
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String customerId = sanitizeCustomerId(email, mobile);

        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", customerName);
        customerMap.put("email", email);
        customerMap.put("mobile", mobile);
        customerMap.put("govIdType", govIdType);
        customerMap.put("govIdNumber", govIdNumber);
        customerMap.put("stayStatus", "in_house");
        customerMap.put("updatedAt", Timestamp.now());

        Map<String, Object> bookingMap = new HashMap<>();
        bookingMap.put("transactionRef", txnRef);
        bookingMap.put("customerName", customerName);
        bookingMap.put("customerId", customerId);
        bookingMap.put("rooms", new ArrayList<>(roomIds));
        bookingMap.put("totalAmount", totalAmount);
        bookingMap.put("amountPaid", totalAmount);
        bookingMap.put("balanceDue", 0.0);
        bookingMap.put("extrasNotes", "");
        bookingMap.put("checkIn", new Timestamp(new java.util.Date(checkInMillis)));
        bookingMap.put("checkOut", new Timestamp(new java.util.Date(checkOutMillis)));
        bookingMap.put("status", "confirmed");
        bookingMap.put("adults", adults);
        bookingMap.put("children", children);
        bookingMap.put("paymentMethod", paymentMethod);
        bookingMap.put("createdByRole", createdByRole);
        bookingMap.put("createdAt", Timestamp.now());

        String microCategory = buildRoomsMicroCategory(roomIds, roomInventory);
        Map<String, Object> financeMap = new HashMap<>();
        financeMap.put("type", "revenue");
        financeMap.put("amount", totalAmount);
        financeMap.put("date", Timestamp.now());
        financeMap.put("monthKey", AdminMonthKey.nowMonthKey());
        financeMap.put("category", "Revenue");
        financeMap.put("subCategory", "Room Booking");
        financeMap.put("microCategory", microCategory);
        financeMap.put("sourceBookingId", txnRef);
        financeMap.put("note", "Booking payment via " + paymentMethod);
        financeMap.put("createdBy", createdByRole);
        financeMap.put("createdAt", Timestamp.now());

        Task<Void> customerTask = db.collection("customers").document(customerId).set(customerMap, SetOptions.merge());
        Task<Void> bookingTask = db.collection("bookings").document(txnRef).set(bookingMap, SetOptions.merge());
        Task<Void> financeTask = db.collection("finance_transactions").document(txnRef).set(financeMap, SetOptions.merge());

        // We don't mark rooms 'occupied' until they actually arrive (check-in)
        // They will show as 'booked' in the UI because they are in the bookings collection.
        return Tasks.whenAll(customerTask, bookingTask, financeTask);
    }

    /**
     * Officially checks in the guest, marking status as in_house and rooms as occupied.
     */
    public static Task<Void> markArrived(String bookingId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("bookings").document(bookingId).get().continueWithTask(task -> {
            DocumentSnapshot snap = task.getResult();
            if (!snap.exists()) throw new Exception("Booking not found");

            List<String> roomIds = (List<String>) snap.get("rooms");
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "in_house");
            updates.put("checkInActual", Timestamp.now());
            updates.put("updatedAt", Timestamp.now());

            return db.collection("bookings").document(bookingId).update(updates).continueWithTask(t -> {
                if (roomIds == null) return Tasks.forResult(null);
                
                List<Task<Void>> rTasks = new ArrayList<>();
                for (String rLabel : roomIds) {
                    rTasks.add(db.collection("rooms")
                        .whereEqualTo("bookingRoomId", rLabel)
                        .get()
                        .continueWithTask(qr -> {
                            if (!qr.getResult().isEmpty()) {
                                return qr.getResult().getDocuments().get(0).getReference().update("housekeepingStatus", "occupied");
                            }
                            return Tasks.forResult(null);
                        }));
                }
                return Tasks.whenAll(rTasks);
            });
        });
    }

    /**
     * Records a payment against an existing booking (minibar, balance at checkout, etc.).
     */
    public static Task<Void> recordBalancePayment(
            String bookingId,
            double amount,
            String paymentMethod,
            String createdByRole
    ) {
        if (amount <= 0) {
            return Tasks.forException(new IllegalArgumentException("Amount must be positive"));
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference bookingRef = db.collection("bookings").document(bookingId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(bookingRef);
            if (!snap.exists()) {
                throw new IllegalStateException("Booking not found");
            }
            double balanceDue = readDouble(snap, "balanceDue", 0);
            double amountPaid = readDouble(snap, "amountPaid", readDouble(snap, "totalAmount", 0));
            if (balanceDue <= 0.01) {
                throw new IllegalStateException("No balance due");
            }
            double pay = Math.min(amount, balanceDue);
            amountPaid += pay;
            balanceDue = Math.max(0, balanceDue - pay);

            java.util.Map<String, Object> updates = new HashMap<>();
            updates.put("amountPaid", amountPaid);
            updates.put("balanceDue", balanceDue);
            updates.put("updatedAt", Timestamp.now());
            transaction.update(bookingRef, updates);

            String micro = "Balance payment";
            java.util.Map<String, Object> financeMap = new HashMap<>();
            financeMap.put("type", "revenue");
            financeMap.put("amount", pay);
            financeMap.put("date", Timestamp.now());
            financeMap.put("monthKey", AdminMonthKey.nowMonthKey());
            financeMap.put("category", "Revenue");
            financeMap.put("subCategory", "Room Booking");
            financeMap.put("microCategory", micro);
            financeMap.put("sourceBookingId", bookingId);
            financeMap.put("note", "Checkout balance via " + paymentMethod);
            financeMap.put("createdBy", createdByRole);
            financeMap.put("createdAt", Timestamp.now());
            String finId = bookingId + "-bal-" + System.currentTimeMillis();
            transaction.set(db.collection("finance_transactions").document(finId), financeMap, SetOptions.merge());
            return null;
        });
    }

    /**
     * Adds an extra charge (laundry, minibar) before checkout.
     */
    public static Task<Void> appendExtraCharge(String bookingId, double amount, String note) {
        if (amount <= 0) {
            return Tasks.forException(new IllegalArgumentException("Amount must be positive"));
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference bookingRef = db.collection("bookings").document(bookingId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(bookingRef);
            if (!snap.exists()) {
                throw new IllegalStateException("Booking not found");
            }
            double balanceDue = readDouble(snap, "balanceDue", 0);
            double totalAmount = readDouble(snap, "totalAmount", 0);
            balanceDue += amount;
            totalAmount += amount;
            String prev = snap.getString("extrasNotes");
            String extraLine = (note == null || note.trim().isEmpty())
                    ? ("+₹" + String.format(Locale.US, "%.0f", amount))
                    : ("+₹" + String.format(Locale.US, "%.0f", amount) + ": " + note.trim());
            String merged = (prev == null || prev.isEmpty()) ? extraLine : prev + "\n" + extraLine;

            java.util.Map<String, Object> updates = new HashMap<>();
            updates.put("totalAmount", totalAmount);
            updates.put("balanceDue", balanceDue);
            updates.put("extrasNotes", merged);
            updates.put("updatedAt", Timestamp.now());
            transaction.update(bookingRef, updates);
            return null;
        });
    }

    /**
     * Marks booking checked out only when balance is settled.
     */
    public static Task<Void> confirmCheckout(String bookingId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference bookingRef = db.collection("bookings").document(bookingId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(bookingRef);
            if (!snap.exists()) {
                throw new IllegalStateException("Booking not found");
            }
            double balanceDue = readDouble(snap, "balanceDue", 0);
            if (balanceDue > 0.01) {
                throw new IllegalStateException("Balance due");
            }
            String customerId = snap.getString("customerId");

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "checked_out");
            updates.put("actualCheckoutAt", Timestamp.now());
            updates.put("updatedAt", Timestamp.now());
            transaction.update(bookingRef, updates);

            if (customerId != null && !customerId.isEmpty()) {
                DocumentReference custRef = db.collection("customers").document(customerId);
                Map<String, Object> cust = new HashMap<>();
                cust.put("stayStatus", "checked_out");
                cust.put("updatedAt", Timestamp.now());
                transaction.set(custRef, cust, SetOptions.merge());
            }

            // Free up rooms
            Object roomsObj = snap.get("rooms");
            if (roomsObj instanceof List) {
                for (Object r : (List<?>) roomsObj) {
                    String bookingRoomId = String.valueOf(r);
                    // We need a way to find the doc ID from bookingRoomId. 
                    // This is slightly tricky in a transaction without a secondary query.
                    // For now, I'll update byroomId query after transaction or assume one-to-one.
                    // Actually, I'll do a separate update outside if I can't find docId here.
                }
            }

            return null;
        }).continueWithTask(task -> {
            if (task.isSuccessful()) {
                return freeRoomsAfterCheckout(bookingId);
            }
            return Tasks.forException(task.getException() != null ? task.getException() : new Exception("Checkout failed"));
        });
    }

    private static Task<Void> freeRoomsAfterCheckout(String bookingId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("bookings").document(bookingId).get().continueWithTask(t -> {
            if (!t.isSuccessful() || t.getResult() == null) return Tasks.forResult(null);
            List<String> roomIds = (List<String>) t.getResult().get("rooms");
            if (roomIds == null || roomIds.isEmpty()) return Tasks.forResult(null);

            List<Task<Void>> tasks = new ArrayList<>();
            for (String rid : roomIds) {
                // rid is the bookingRoomId (label)
                tasks.add(db.collection("rooms").whereEqualTo("bookingRoomId", rid).get().continueWithTask(q -> {
                    if (q.isSuccessful() && q.getResult() != null && !q.getResult().isEmpty()) {
                        DocumentReference dref = q.getResult().getDocuments().get(0).getReference();
                        return dref.update("housekeepingStatus", "cleaning");
                    }
                    return Tasks.forResult(null);
                }));
            }
            return Tasks.whenAll(tasks).continueWithTask(x -> maybeUpdateCustomerByName(bookingId));
        });
    }

    private static Task<Void> maybeUpdateCustomerByName(String bookingId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("bookings").document(bookingId).get().continueWithTask(t -> {
            if (!t.isSuccessful() || t.getResult() == null || !t.getResult().exists()) {
                return Tasks.forResult(null);
            }
            DocumentSnapshot snap = t.getResult();
            String customerId = snap.getString("customerId");
            if (customerId != null && !customerId.isEmpty()) {
                return Tasks.forResult(null);
            }
            String customerName = snap.getString("customerName");
            if (customerName == null || customerName.isEmpty()) {
                return Tasks.forResult(null);
            }
            return db.collection("customers").whereEqualTo("name", customerName).get().continueWithTask(q -> {
                if (!q.isSuccessful() || q.getResult() == null) {
                    return Tasks.forResult(null);
                }
                List<com.google.firebase.firestore.DocumentSnapshot> docs = q.getResult().getDocuments();
                List<Task<Void>> tasks = new ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot cdoc : docs) {
                    tasks.add(cdoc.getReference().update("stayStatus", "checked_out", "updatedAt", Timestamp.now()));
                }
                if (tasks.isEmpty()) {
                    return Tasks.forResult(null);
                }
                return Tasks.whenAll(tasks);
            });
        });
    }

    private static double readDouble(DocumentSnapshot snap, String key, double defaultVal) {
        Double d = snap.getDouble(key);
        return d != null ? d : defaultVal;
    }

    private static String sanitizeCustomerId(String email, String mobile) {
        if (email != null && !email.trim().isEmpty()) {
            return email.trim().toLowerCase(Locale.ROOT).replace(".", "_");
        }
        if (mobile != null && !mobile.trim().isEmpty()) {
            return "mob_" + mobile.trim();
        }
        return "guest_" + System.currentTimeMillis();
    }

    private static String buildRoomsMicroCategory(List<String> roomIds, List<HotelRoom> inventory) {
        List<String> labels = new ArrayList<>();
        for (String id : roomIds) {
            HotelRoom room = HotelRoom.findById(id, inventory);
            labels.add(room != null ? room.label : id);
        }
        return "Rooms " + String.join(",", labels);
    }

    private static class AdminMonthKey {
        static String nowMonthKey() {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM", Locale.US);
            return sdf.format(new java.util.Date());
        }
    }
}
