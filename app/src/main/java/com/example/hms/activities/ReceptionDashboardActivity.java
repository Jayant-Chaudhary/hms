package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.utils.ReceptionBookingDraft;
import com.example.hms.utils.SessionManager;
import com.example.hms.utils.ThemeManager;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceptionDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ReceptionDashboard";
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    
    private TextView tvReceptionWelcome, tvArrivalsCount, tvStayOversCount, tvDeparturesCount, tvAvailableRoomsCount;
    private RecyclerView rvTodayArrivals;
    private final List<ArrivalItem> arrivalList = new ArrayList<>();
    private ArrivalAdapter arrivalAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_dashboard);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Bind Views matching the new activity_reception_dashboard.xml
        tvReceptionWelcome = findViewById(R.id.tvReceptionWelcome);
        tvArrivalsCount = findViewById(R.id.tvArrivalsCount);
        tvStayOversCount = findViewById(R.id.tvStayOversCount);
        tvDeparturesCount = findViewById(R.id.tvDeparturesCount);
        tvAvailableRoomsCount = findViewById(R.id.tvAvailableRoomsCount);
        rvTodayArrivals = findViewById(R.id.rvTodayArrivals);

        // Set up RecyclerView
        if (rvTodayArrivals != null) {
            rvTodayArrivals.setLayoutManager(new LinearLayoutManager(this));
            arrivalAdapter = new ArrivalAdapter(arrivalList);
            rvTodayArrivals.setAdapter(arrivalAdapter);
        }

        // Header Greeting
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = user != null && user.getEmail() != null ? user.getEmail() : "Marcus";
        if (tvReceptionWelcome != null) {
            tvReceptionWelcome.setText("Welcome back, " + name + ". Loading today's schedule...");
        }

        // Button Listeners
        View btnNew = findViewById(R.id.btnNewReservation);
        if (btnNew != null) {
            btnNew.setOnClickListener(v -> {
                ReceptionBookingDraft.reset();
                startActivity(new Intent(this, ReceptionCustomerRegistrationActivity.class));
            });
        }

        // Bottom Navigation Listeners
        View navHistory = findViewById(R.id.navHistory);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> startActivity(new Intent(this, ReceptionHistoryActivity.class)));
        }

        View navCheckIn = findViewById(R.id.navCheckIn);
        if (navCheckIn != null) {
            navCheckIn.setOnClickListener(v -> {
                ReceptionBookingDraft.reset();
                startActivity(new Intent(this, ReceptionCustomerRegistrationActivity.class));
            });
        }

        View navCheckOut = findViewById(R.id.navCheckOut);
        if (navCheckOut != null) {
            navCheckOut.setOnClickListener(v -> {
                startActivity(new Intent(this, ReceptionCheckoutActivity.class));
            });
        }

        View navArrivals = findViewById(R.id.navArrivals);
        if (navArrivals != null) {
            navArrivals.setOnClickListener(v -> startActivity(new Intent(this, ReceptionArrivalsActivity.class)));
        }

        View titleLogo = findViewById(R.id.tvReceptionTitle);
        if (titleLogo != null) {
            titleLogo.setOnClickListener(v -> 
                startActivity(new Intent(this, SettingsActivity.class)));
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }

        // Avatar Logout (matches Image behavior)
        View avatar = findViewById(R.id.ivReceptionistAvatar);
        if (avatar != null) {
            avatar.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                sessionManager.clearSession();
                Intent i = new Intent(this, loginPage.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        }

        fetchDashboardData();
    }

    private void fetchDashboardData() {
        long startOfToday = getStartOfDay();
        long endOfToday = getEndOfDay();

        // 1. Fetch Arrivals
        db.collection("bookings")
                .whereEqualTo("status", "confirmed")
                .whereGreaterThanOrEqualTo("checkIn", new Timestamp(new Date(startOfToday)))
                .whereLessThanOrEqualTo("checkIn", new Timestamp(new Date(endOfToday)))
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();
                    if (tvArrivalsCount != null) tvArrivalsCount.setText(String.valueOf(count));
                    if (tvReceptionWelcome != null) {
                        tvReceptionWelcome.setText("Welcome back. You have " + count + " guest arrivals scheduled for today.");
                    }
                    
                    arrivalList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ArrivalItem item = new ArrivalItem();
                        item.bookingId = doc.getId();
                        item.name = doc.getString("customerName");
                        item.roomType = doc.getString("roomType");
                        item.resId = doc.getId().substring(0, Math.min(doc.getId().length(), 5)).toUpperCase();
                        Timestamp ts = doc.getTimestamp("checkIn");
                        if (ts != null) {
                            item.arrivalTime = new SimpleDateFormat("HH:mm a", Locale.getDefault()).format(ts.toDate());
                        }
                        arrivalList.add(item);
                    }
                    if (arrivalAdapter != null) arrivalAdapter.notifyDataSetChanged();
                });

        // 2. Fetch Departures (Active guests due today)
        db.collection("bookings")
                .whereIn("status", java.util.Arrays.asList("in_house", "due_checkout"))
                .whereGreaterThanOrEqualTo("checkOut", new Timestamp(new Date(startOfToday)))
                .whereLessThanOrEqualTo("checkOut", new Timestamp(new Date(endOfToday)))
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (tvDeparturesCount != null) tvDeparturesCount.setText(String.valueOf(snapshots.size()));
                });

        // 3. Fetch Stay Overs
        db.collection("bookings")
                .whereEqualTo("status", "in_house")
                .get()
                .addOnSuccessListener(snapshots -> {
                    int stayOvers = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Timestamp checkOut = doc.getTimestamp("checkOut");
                        if (checkOut != null && checkOut.toDate().getTime() > endOfToday) {
                            stayOvers++;
                        }
                    }
                    if (tvStayOversCount != null) tvStayOversCount.setText(String.format(Locale.getDefault(), "%02d", stayOvers));
                });

        // 4. Available Rooms
        db.collection("rooms")
                .whereEqualTo("status", "ready")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (tvAvailableRoomsCount != null) tvAvailableRoomsCount.setText(String.format(Locale.getDefault(), "%02d", snapshots.size()));
                });
    }

    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getEndOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    private static class ArrivalItem {
        String bookingId;
        String name;
        String roomType;
        String resId;
        String arrivalTime;
    }

    private class ArrivalAdapter extends RecyclerView.Adapter<ArrivalAdapter.Holder> {
        private final List<ArrivalItem> list;
        ArrivalAdapter(List<ArrivalItem> list) { this.list = list; }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guest_arrival_dashboard, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            ArrivalItem item = list.get(position);
            h.name.setText(item.name != null ? item.name : "Guest");
            h.roomType.setText(item.roomType != null ? item.roomType : "Room");
            h.resId.setText("• Res ID: #" + item.resId);
            h.time.setText(item.arrivalTime != null ? item.arrivalTime : "TBD");

            h.btnArrived.setOnClickListener(v -> {
                com.example.hms.utils.BookingDataSync.markArrived(item.bookingId)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(ReceptionDashboardActivity.this, "Guest checked in!", Toast.LENGTH_SHORT).show();
                        fetchDashboardData();
                    })
                    .addOnFailureListener(e -> Toast.makeText(ReceptionDashboardActivity.this, "Check-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView name, roomType, resId, time;
            View btnArrived;
            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.tvGuestName);
                roomType = v.findViewById(R.id.tvRoomType);
                resId = v.findViewById(R.id.tvResId);
                time = v.findViewById(R.id.tvArrivalTime);
                btnArrived = v.findViewById(R.id.btnMarkArrived);
            }
        }
    }
}
