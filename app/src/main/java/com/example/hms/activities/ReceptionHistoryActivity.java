package com.example.hms.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.hms.utils.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceptionHistoryActivity extends AppCompatActivity {

    private final List<BookingItem> items = new ArrayList<>();
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_history);

        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        rv.setAdapter(adapter);
        loadHistory();

        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());
    }

    private void loadHistory() {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("status", "checked_out")
                .orderBy("checkOut", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    items.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        BookingItem b = new BookingItem();
                        b.id = doc.getId();
                        b.customerName = doc.getString("customerName");
                        b.totalAmount = doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0;
                        b.checkOut = doc.getTimestamp("checkOut");
                        items.add(b);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private static class BookingItem {
        String id;
        String customerName;
        double totalAmount;
        Timestamp checkOut;
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_booking, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            BookingItem b = items.get(position);
            String out = b.checkOut == null ? "-" : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(b.checkOut.toDate());
            h.title.setText((b.customerName == null ? "Guest" : b.customerName) + " • Checked out");
            h.meta.setText("Checkout: " + out + " | Paid: ₹" + String.format(Locale.getDefault(), "%,.0f", b.totalAmount));
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, meta;
            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvBookingTitle);
                meta = itemView.findViewById(R.id.tvBookingMeta);
            }
        }
    }
}
