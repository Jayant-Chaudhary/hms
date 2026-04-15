package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.utils.BookingDataSync;
import com.example.hms.utils.ThemeManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceptionArrivalsActivity extends AppCompatActivity {

    private final List<BookingItem> items = new ArrayList<>();
    private ArrivalsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_arrivals);

        RecyclerView rv = findViewById(R.id.rvArrivals);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArrivalsAdapter();
        rv.setAdapter(adapter);
        loadArrivals();

        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadArrivals();
    }

    private void loadArrivals() {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereIn("status", java.util.Arrays.asList("confirmed", "in_house", "due_checkout"))
                .get()
                .addOnSuccessListener(snapshots -> {
                    items.clear();
                    long now = System.currentTimeMillis();
                    long dueWindow = now + (6L * 60L * 60L * 1000L);
                    for (QueryDocumentSnapshot doc : snapshots) {
                        BookingItem b = new BookingItem();
                        b.id = doc.getId();
                        b.customerName = doc.getString("customerName");
                        b.status = doc.getString("status");
                        b.totalAmount = doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0;
                        double paid = doc.getDouble("amountPaid") != null ? doc.getDouble("amountPaid") : b.totalAmount;
                        Double bd = doc.getDouble("balanceDue");
                        b.balanceDue = bd != null ? bd : Math.max(0, b.totalAmount - paid);
                        b.checkOut = doc.getTimestamp("checkOut");

                        if ("in_house".equalsIgnoreCase(b.status) && b.checkOut != null) {
                            long out = b.checkOut.toDate().getTime();
                            if (out <= dueWindow) {
                                b.status = "due_checkout";
                                FirebaseFirestore.getInstance().collection("bookings").document(b.id)
                                        .update("status", "due_checkout");
                            }
                        }
                        items.add(b);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private static class BookingItem {
        String id;
        String customerName;
        String status;
        double totalAmount;
        double balanceDue;
        Timestamp checkOut;
    }

    private class ArrivalsAdapter extends RecyclerView.Adapter<ArrivalsAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reception_booking_action, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            BookingItem b = items.get(position);
            String out = b.checkOut == null ? "-" : new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(b.checkOut.toDate().getTime()));
            h.title.setText((b.customerName == null ? "Guest" : b.customerName) + " • " + (b.status == null ? "-" : b.status));
            String balLine = b.balanceDue > 0.01
                    ? (" | Balance due: ₹" + String.format(Locale.getDefault(), "%,.0f", b.balanceDue))
                    : " | Balance: settled";
            h.meta.setText("Checkout: " + out + " | Total: ₹" + String.format(Locale.getDefault(), "%,.0f", b.totalAmount) + balLine);

            if ("confirmed".equalsIgnoreCase(b.status)) {
                h.action.setText("Mark Arrived");
                h.action.setOnClickListener(v -> {
                    BookingDataSync.markArrived(b.id)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(ReceptionArrivalsActivity.this, "Guest checked in!", Toast.LENGTH_SHORT).show();
                                loadArrivals();
                            })
                            .addOnFailureListener(e -> Toast.makeText(ReceptionArrivalsActivity.this, "Check-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
            } else {
                h.action.setText(b.balanceDue > 0.01 ? "Pay balance" : "Confirm checkout");
                h.action.setEnabled(!"checked_out".equalsIgnoreCase(b.status));
                h.action.setOnClickListener(v -> {
                    if (b.balanceDue > 0.01) {
                        Intent pay = new Intent(ReceptionArrivalsActivity.this, ReceptionCheckoutPaymentActivity.class);
                        pay.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_BOOKING_ID, b.id);
                        pay.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_AMOUNT, b.balanceDue);
                        pay.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_GUEST_LABEL, b.customerName);
                        startActivity(pay);
                        return;
                    }
                    BookingDataSync.confirmCheckout(b.id)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(ReceptionArrivalsActivity.this, "Marked checked out", Toast.LENGTH_SHORT).show();
                                loadArrivals();
                            })
                            .addOnFailureListener(e -> Toast.makeText(ReceptionArrivalsActivity.this,
                                    e.getMessage() != null ? e.getMessage() : "Checkout failed", Toast.LENGTH_LONG).show());
                });
            }
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, meta;
            Button action;
            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvBookingTitle);
                meta = itemView.findViewById(R.id.tvBookingMeta);
                action = itemView.findViewById(R.id.btnAction);
            }
        }
    }
}
