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
import com.example.hms.model.admin.BookingRecord;
import com.example.hms.utils.BookingDataSync;
import com.example.hms.utils.ThemeManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceptionCheckoutActivity extends AppCompatActivity {

    private final List<BookingRecord> inHouseGuests = new ArrayList<>();
    private CheckoutAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_arrivals); // Reuse arrivals layout structure (Title + RecyclerView)

        // Custom Title for this activity
        TextView tvTitle = findViewById(R.id.tvArrivalsTitle);
        if (tvTitle != null) tvTitle.setText("Guest Checkout");

        db = FirebaseFirestore.getInstance();
        RecyclerView rv = findViewById(R.id.rvArrivals);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CheckoutAdapter();
        rv.setAdapter(adapter);

        loadInHouseGuests();
    }

    private void loadInHouseGuests() {
        db.collection("bookings")
                .whereEqualTo("status", "in_house")
                .get()
                .addOnSuccessListener(snapshots -> {
                    inHouseGuests.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        BookingRecord b = doc.toObject(BookingRecord.class);
                        b.id = doc.getId();
                        inHouseGuests.add(b);
                    }
                    adapter.notifyDataSetChanged();
                    if (inHouseGuests.isEmpty()) {
                        Toast.makeText(this, "No guests currently in-house.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reception_due_checkout, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            BookingRecord b = inHouseGuests.get(position);
            h.title.setText(b.customerName != null ? b.customerName : "Anonymous Guest");
            
            String rooms = b.rooms != null ? String.join(", ", b.rooms) : "-";
            h.meta.setText("Rooms: " + rooms + "\nCheck-in: " + formatDate(b.checkIn));
            
            double balance = b.totalAmount - b.amountPaid;
            h.balance.setText(String.format(Locale.getDefault(), "Balance Due: ₹%,.2f", balance));
            h.balance.setVisibility(balance > 0 ? View.VISIBLE : View.GONE);

            h.btnPay.setOnClickListener(v -> {
                Intent i = new Intent(ReceptionCheckoutActivity.this, ReceptionCheckoutPaymentActivity.class);
                i.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_BOOKING_ID, b.id);
                i.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_AMOUNT, balance);
                i.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_GUEST_LABEL, b.customerName);
                startActivityForResult(i, 101);
            });

            h.btnConfirm.setOnClickListener(v -> {
                if (balance > 0) {
                    Toast.makeText(ReceptionCheckoutActivity.this, "Please settle balance first", Toast.LENGTH_SHORT).show();
                } else {
                    BookingDataSync.confirmCheckout(b.id)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(ReceptionCheckoutActivity.this, "Checkout successful", Toast.LENGTH_SHORT).show();
                                loadInHouseGuests();
                            })
                            .addOnFailureListener(e -> Toast.makeText(ReceptionCheckoutActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });

            // "Add Extra" functionality can be added here if needed later
            h.btnAddExtra.setOnClickListener(v -> Toast.makeText(ReceptionCheckoutActivity.this, "Feature coming soon: Add minibar/laundry", Toast.LENGTH_SHORT).show());
        }

        @Override public int getItemCount() { return inHouseGuests.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, meta, balance;
            Button btnAddExtra, btnPay, btnConfirm;
            Holder(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.tvDueTitle);
                meta = v.findViewById(R.id.tvDueMeta);
                balance = v.findViewById(R.id.tvBalanceDue);
                btnAddExtra = v.findViewById(R.id.btnAddExtra);
                btnPay = v.findViewById(R.id.btnPayBalance);
                btnConfirm = v.findViewById(R.id.btnConfirmCheckout);
            }
        }
    }

    private String formatDate(com.google.firebase.Timestamp ts) {
        if (ts == null) return "-";
        return new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(ts.toDate());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            loadInHouseGuests();
        }
    }
}
