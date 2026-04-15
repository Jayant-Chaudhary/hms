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
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 1. Fetch Real-time Notifications
        db.collection("notifications")
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(nq -> {
                    llNotifications.removeAllViews();
                    boolean hasContent = !nq.isEmpty();
                    
                    for (com.google.firebase.firestore.DocumentSnapshot d : nq.getDocuments()) {
                        addNotificationCard(d.getString("title"), d.getString("message"));
                    }

                    // 2. Fetch Booking Status Alerts (Legacy)
                    db.collection("bookings")
                            .whereEqualTo("customerId", customerId)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (qs.isEmpty() && !hasContent) {
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
                                    
                                    if ("pending_validation".equalsIgnoreCase(status)) {
                                         addNotificationCard("Pending Validation", "Your booking " + bookingRef + " is waiting for reception approval.");
                                    } else if (daysLeft >= 0 && daysLeft <= 2) {
                                         addNotificationCard("Stay Status", "Booking " + bookingRef + " is active. Checkout is in " + daysLeft + " day(s).");
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    tvNotificationsEmpty.setVisibility(View.VISIBLE);
                    tvNotificationsEmpty.setText("Unable to load notifications.");
                });
    }

    private void addNotificationCard(String title, String message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_dashboard_card);
        card.setPadding(32, 24, 32, 24);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = 16;
        card.setLayoutParams(lp);

        if (title != null) {
            TextView tvTitle = new TextView(this);
            tvTitle.setText(title.toUpperCase(Locale.ROOT));
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvTitle.setTextSize(12);
            tvTitle.setAlpha(0.8f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(tvTitle);
        }

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tvMsg.setTextSize(14);
        tvMsg.setLineSpacing(0, 1.2f);
        if (title != null) tvMsg.setPadding(0, 4, 0, 0);
        card.addView(tvMsg);

        llNotifications.addView(card);
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }
}
