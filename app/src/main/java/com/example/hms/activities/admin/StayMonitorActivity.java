package com.example.hms.activities.admin;

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
import com.example.hms.model.admin.BookingRecord;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StayMonitorActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<BookingRecord> items = new ArrayList<>();
    private BookingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_stay_monitor);

        RecyclerView rv = findViewById(R.id.rvBookings);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter();
        rv.setAdapter(adapter);
        loadBookings();
    }

    private void loadBookings() {
        TextView summary = findViewById(R.id.tvStaySummary);
        repo.bookings().get().addOnSuccessListener(snapshots -> {
            items.clear();
            int inHouse = 0, due = 0, history = 0;
            for (QueryDocumentSnapshot doc : snapshots) {
                BookingRecord b = doc.toObject(BookingRecord.class);
                b.id = doc.getId();
                items.add(b);
                String status = b.status == null ? "" : b.status;
                if ("in_house".equalsIgnoreCase(status)) inHouse++;
                else if ("due_checkout".equalsIgnoreCase(status)) due++;
                else if ("checked_out".equalsIgnoreCase(status)) history++;
            }
            summary.setText("In-house: " + inHouse + "  |  Due checkout: " + due + "  |  History: " + history);
            adapter.notifyDataSetChanged();
        });
    }

    private class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_booking, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            BookingRecord b = items.get(position);
            h.title.setText((b.customerName == null ? "Guest" : b.customerName) + " • " + (b.status == null ? "-" : b.status));
            String in = b.checkIn == null ? "-" : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(b.checkIn.toDate());
            String out = b.checkOut == null ? "-" : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(b.checkOut.toDate());
            h.meta.setText("Check-in: " + in + "  |  Check-out: " + out + "\nTotal: ₹" + b.totalAmount);
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
