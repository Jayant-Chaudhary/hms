package com.example.hms.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hms.R;
import com.example.hms.utils.CustomerBookingDraft;
import com.example.hms.utils.CustomerProfileStore;
import com.example.hms.utils.SessionManager;
import com.example.hms.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerDashboardActivity extends AppCompatActivity {

    private static final double HOTEL_LAT = 19.0760;
    private static final double HOTEL_LNG = 72.8777;

    private TextView tvHeaderGreeting;
    private TextView tvCustomerEmail;
    private TextView tvCurrentBookingsEmpty;
    private TextView tvPreviousBookingsEmpty;
    private LinearLayout llCurrentBookings;
    private LinearLayout llPreviousBookings;
    private CustomerProfileStore profileStore;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        SessionManager sessionManager = new SessionManager(this);
        profileStore = new CustomerProfileStore(this);
        tvHeaderGreeting = findViewById(R.id.tvHeaderGreeting);
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail);
        tvCurrentBookingsEmpty = findViewById(R.id.tvCurrentBookingsEmpty);
        tvPreviousBookingsEmpty = findViewById(R.id.tvPreviousBookingsEmpty);
        llCurrentBookings = findViewById(R.id.llCurrentBookings);
        llPreviousBookings = findViewById(R.id.llPreviousBookings);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userEmail = user != null && user.getEmail() != null ? user.getEmail() : sessionManager.getEmail();
        tvCustomerEmail.setText(userEmail == null || userEmail.isEmpty() ? "Guest" : userEmail);

        ensureCustomerName();
        renderHeader();
        loadBookings();

        findViewById(R.id.btnBookRoom).setOnClickListener(v -> {
            CustomerBookingDraft.reset();
            startActivity(new Intent(this, CustomerRegistrationActivity.class));
        });
        findViewById(R.id.btnShowLocation).setOnClickListener(v -> openHotelLocation());
        findViewById(R.id.btnCustomerSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, CustomerNotificationsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderHeader();
        loadBookings();
    }

    private void ensureCustomerName() {
        if (!profileStore.getName().isEmpty()) {
            return;
        }
        final EditText input = new EditText(this);
        input.setHint("Enter your name");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        new AlertDialog.Builder(this)
                .setTitle("Welcome")
                .setMessage("Please tell us your name for a personalized dashboard.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    profileStore.setName(name.isEmpty() ? "Guest" : name);
                    renderHeader();
                })
                .show();
    }

    private void renderHeader() {
        String name = profileStore.getName();
        if (name.isEmpty()) {
            name = "Guest";
        }
        tvHeaderGreeting.setText(greetingForNow() + ", " + name);
    }

    private String greetingForNow() {
        int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private void openHotelLocation() {
        Uri geoUri = Uri.parse("geo:" + HOTEL_LAT + "," + HOTEL_LNG + "?q=" + HOTEL_LAT + "," + HOTEL_LNG + "(Demo Hotel)");
        Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
        if (geoIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(geoIntent);
            return;
        }
        Uri webUri = Uri.parse("https://maps.google.com/?q=" + HOTEL_LAT + "," + HOTEL_LNG);
        startActivity(new Intent(Intent.ACTION_VIEW, webUri));
    }

    private void loadBookings() {
        llCurrentBookings.removeAllViews();
        llPreviousBookings.removeAllViews();
        if (userEmail == null || userEmail.isEmpty()) {
            tvCurrentBookingsEmpty.setText("No active bookings found.");
            tvPreviousBookingsEmpty.setText("No previous bookings found.");
            return;
        }
        String customerId = userEmail.trim().toLowerCase(Locale.ROOT).replace(".", "_");
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> current = new ArrayList<>();
                    List<DocumentSnapshot> previous = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Timestamp out = doc.getTimestamp("checkOut");
                        long checkoutMillis = out != null ? out.toDate().getTime() : 0L;
                        String status = doc.getString("status");
                        if ("checked_out".equalsIgnoreCase(status) || (checkoutMillis > 0 && checkoutMillis < now)) {
                            previous.add(doc);
                        } else {
                            current.add(doc);
                        }
                    }
                    renderBookingSection(llCurrentBookings, tvCurrentBookingsEmpty, current, "No active bookings found.");
                    renderBookingSection(llPreviousBookings, tvPreviousBookingsEmpty, previous, "No previous bookings found.");
                })
                .addOnFailureListener(e -> {
                    tvCurrentBookingsEmpty.setText("Unable to load current bookings.");
                    tvPreviousBookingsEmpty.setText("Unable to load previous bookings.");
                    Toast.makeText(this, "Failed to load bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void renderBookingSection(LinearLayout container, TextView emptyView, List<DocumentSnapshot> docs, String emptyText) {
        container.removeAllViews();
        if (docs.isEmpty()) {
            emptyView.setText(emptyText);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        for (DocumentSnapshot doc : docs) {
            TextView item = new TextView(this);
            item.setBackgroundResource(R.drawable.bg_dashboard_card);
            item.setPadding(24, 18, 24, 18);
            item.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = 12;
            item.setLayoutParams(lp);

            String ref = value(doc, "transactionRef");
            String rooms = value(doc, "rooms");
            String status = value(doc, "status");
            item.setText("Booking Ref: " + ref + "\nRooms: " + rooms + "\nStatus: " + status);
            container.addView(item);
        }
    }

    private String value(DocumentSnapshot doc, String key) {
        Object v = doc.get(key);
        return v == null ? "-" : v.toString();
    }
}
