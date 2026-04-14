package com.example.hms.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hms.R;
import com.example.hms.utils.SessionManager;
import com.example.hms.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CustomerNotificationsActivity extends AppCompatActivity {

    private LinearLayout llNotifications;
    private TextView tvNotificationsEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_notifications);

        llNotifications = findViewById(R.id.llNotifications);
        tvNotificationsEmpty = findViewById(R.id.tvNotificationsEmpty);
        findViewById(R.id.btnBackNotifications).setOnClickListener(v -> finish());

        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null && user.getEmail() != null
                ? user.getEmail()
                : new SessionManager(this).getEmail();

        if (email == null || email.isEmpty()) {
            tvNotificationsEmpty.setText("No notifications available.");
            return;
        }
        String customerId = email.trim().toLowerCase(Locale.ROOT).replace(".", "_");
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(qs -> {
                    llNotifications.removeAllViews();
                    if (qs.isEmpty()) {
                        tvNotificationsEmpty.setVisibility(View.VISIBLE);
                        tvNotificationsEmpty.setText("No notifications yet.");
                        return;
                    }
                    tvNotificationsEmpty.setVisibility(View.GONE);
                    long now = System.currentTimeMillis();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        String bookingRef = value(doc.get("transactionRef"));
                        String status = value(doc.get("status"));
                        Timestamp checkOut = doc.getTimestamp("checkOut");
                        long daysLeft = -1;
                        if (checkOut != null) {
                            daysLeft = TimeUnit.MILLISECONDS.toDays(checkOut.toDate().getTime() - now);
                        }
                        String message = "Booking " + bookingRef + " is currently " + status + ".";
                        if (daysLeft >= 0 && daysLeft <= 2) {
                            message += " Checkout is in " + daysLeft + " day(s).";
                        }
                        addNotificationCard(message);
                    }
                })
                .addOnFailureListener(e -> {
                    tvNotificationsEmpty.setVisibility(View.VISIBLE);
                    tvNotificationsEmpty.setText("Unable to load notifications.");
                });
    }

    private void addNotificationCard(String message) {
        TextView item = new TextView(this);
        item.setBackgroundResource(R.drawable.bg_dashboard_card);
        item.setText(message);
        item.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        item.setTextSize(14f);
        item.setPadding(22, 16, 22, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = 12;
        item.setLayoutParams(lp);
        llNotifications.addView(item);
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }
}
