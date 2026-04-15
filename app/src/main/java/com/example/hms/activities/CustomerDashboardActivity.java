package com.example.hms.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hms.R;
import com.example.hms.utils.ReceptionBookingDraft;
import com.example.hms.utils.CustomerProfileStore;
import com.example.hms.utils.HotelLocationFallback;
import com.example.hms.utils.HotelSettingsRepository;
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

    private TextView tvHeaderGreeting;
    private TextView tvCustomerEmail;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvHotelLocationLabel;
    private TextView tvCurrentBookingsEmpty;
    private TextView tvPreviousBookingsEmpty;
    private TextView tvPendingBookingsEmpty;
    private LinearLayout llCurrentBookings;
    private LinearLayout llPreviousBookings;
    private LinearLayout llPendingBookings;

    private NestedScrollView scrollDashboard;
    private View sectionHome;
    private View sectionBookings;
    private View sectionProfile;
    private com.google.firebase.firestore.ListenerRegistration bookingListener;

    private LinearLayout navTabHome;
    private LinearLayout navTabBookings;
    private LinearLayout navTabProfile;
    private TextView tvNavHome;
    private TextView tvNavBookings;
    private TextView tvNavProfile;
    private ImageView navIconHome;
    private ImageView navIconBookings;
    private ImageView navIconProfile;
    private SwipeRefreshLayout swipeRefresh;

    private CustomerProfileStore profileStore;
    private String userEmail;

    private double hotelLat;
    private double hotelLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        SessionManager sessionManager = new SessionManager(this);
        profileStore = new CustomerProfileStore(this);

        tvHeaderGreeting = findViewById(R.id.tvHeaderGreeting);
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvHotelLocationLabel = findViewById(R.id.tvHotelLocationLabel);
        tvCurrentBookingsEmpty = findViewById(R.id.tvCurrentBookingsEmpty);
        tvPreviousBookingsEmpty = findViewById(R.id.tvPreviousBookingsEmpty);
        tvPendingBookingsEmpty = findViewById(R.id.tvPendingBookingsEmpty);
        llCurrentBookings = findViewById(R.id.llCurrentBookings);
        llPreviousBookings = findViewById(R.id.llPreviousBookings);
        llPendingBookings = findViewById(R.id.llPendingBookings);

        scrollDashboard = findViewById(R.id.scrollDashboard);
        sectionHome = findViewById(R.id.sectionHome);
        sectionBookings = findViewById(R.id.sectionBookings);
        sectionProfile = findViewById(R.id.sectionProfile);

        navTabHome = findViewById(R.id.navTabHome);
        navTabBookings = findViewById(R.id.navTabBookings);
        navTabProfile = findViewById(R.id.navTabProfile);
        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavBookings = findViewById(R.id.tvNavBookings);
        tvNavProfile = findViewById(R.id.tvNavProfile);
        navIconHome = findViewById(R.id.navIconHome);
        navIconBookings = findViewById(R.id.navIconBookings);
        navIconProfile = findViewById(R.id.navIconProfile);
        swipeRefresh = findViewById(R.id.swipeRefreshCustomer);

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadBookings();
                loadHotelLocation();
            });
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userEmail = user != null && user.getEmail() != null ? user.getEmail() : sessionManager.getEmail();
        if (userEmail != null && !userEmail.isEmpty()) {
            profileStore.setEmail(userEmail);
        }
        tvCustomerEmail.setText(userEmail == null || userEmail.isEmpty() ? "Guest" : userEmail);
        tvProfileEmail.setText(userEmail == null || userEmail.isEmpty() ? "—" : userEmail);

        ensureCustomerName();
        renderHeader();
        loadHotelLocation();
        loadBookings();

        findViewById(R.id.btnBookRoom).setOnClickListener(v -> {
            ReceptionBookingDraft.reset();
            ReceptionBookingDraft.get().createdByRole = "customer";
            ReceptionBookingDraft.get().email = userEmail;
            ReceptionBookingDraft.get().customerName = profileStore.getName();
            startActivity(new Intent(this, ReceptionCustomerRegistrationActivity.class));
        });
        
        View titleLogo = findViewById(R.id.tvCustomerAppTitle);
        if (titleLogo != null) {
            titleLogo.setOnClickListener(v -> 
                startActivity(new Intent(this, SettingsActivity.class)));
        }
        findViewById(R.id.btnShowLocation).setOnClickListener(v -> openHotelLocation());
        findViewById(R.id.btnCustomerSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.btnEditProfileHero).setOnClickListener(v -> showEditProfileDialog());

        navTabHome.setOnClickListener(v -> {
            setNavSelection(0);
            scrollToSection(sectionHome);
        });
        navTabBookings.setOnClickListener(v -> {
            setNavSelection(1);
            scrollToSection(sectionBookings);
        });
        navTabProfile.setOnClickListener(v -> {
            setNavSelection(2);
            scrollToSection(sectionProfile);
        });

        setNavSelection(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderHeader();
        loadBookings();
        loadHotelLocation();
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

    private void showEditProfileDialog() {
        final EditText input = new EditText(this);
        input.setHint("Your name");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        String current = profileStore.getName();
        input.setText(current.isEmpty() ? "" : current);

        new AlertDialog.Builder(this)
                .setTitle("Edit profile")
                .setMessage("Update the name shown on your dashboard.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
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
        tvProfileName.setText(name);
    }

    private String greetingForNow() {
        int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private void loadHotelLocation() {
        HotelLocationFallback.Pin fallback = HotelLocationFallback.forEmail(userEmail);
        hotelLat = fallback.lat;
        hotelLng = fallback.lng;
        tvHotelLocationLabel.setText(fallback.displayName);

        FirebaseFirestore.getInstance()
                .collection(HotelSettingsRepository.COLLECTION)
                .document(HotelSettingsRepository.DOC_HOTEL)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        return;
                    }
                    String label = snap.getString(HotelSettingsRepository.FIELD_DISPLAY_NAME);
                    if (label != null && !label.isEmpty()) {
                        tvHotelLocationLabel.setText(label);
                    }
                    if (snap.contains(HotelSettingsRepository.FIELD_LATITUDE)
                            && snap.contains(HotelSettingsRepository.FIELD_LONGITUDE)) {
                        hotelLat = snap.getDouble(HotelSettingsRepository.FIELD_LATITUDE);
                        hotelLng = snap.getDouble(HotelSettingsRepository.FIELD_LONGITUDE);
                    }
                })
                .addOnFailureListener(e -> { });
    }

    private void openHotelLocation() {
        Uri geoUri = Uri.parse("geo:" + hotelLat + "," + hotelLng + "?q="
                + hotelLat + "," + hotelLng + "(" + Uri.encode(tvHotelLocationLabel.getText().toString()) + ")");
        Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
        if (geoIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(geoIntent);
            return;
        }
        Uri webUri = Uri.parse("https://maps.google.com/?q=" + hotelLat + "," + hotelLng);
        startActivity(new Intent(Intent.ACTION_VIEW, webUri));
    }

    private void loadBookings() {
        if (bookingListener != null) bookingListener.remove();
        
        if (userEmail == null || userEmail.isEmpty()) {
            llCurrentBookings.removeAllViews();
            llPreviousBookings.removeAllViews();
            llPendingBookings.removeAllViews();
            tvCurrentBookingsEmpty.setVisibility(View.VISIBLE);
            tvPreviousBookingsEmpty.setVisibility(View.VISIBLE);
            tvPendingBookingsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        String emailId = userEmail.trim().toLowerCase(Locale.ROOT);
        String legacyId = emailId.replace(".", "_");

        bookingListener = FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereIn("customerId", java.util.Arrays.asList(emailId, legacyId))
                .addSnapshotListener((querySnapshot, e) -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    
                    if (e != null) {
                        Toast.makeText(this, "Sync Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (querySnapshot == null) return;

                    int totalFound = querySnapshot.size();
                    // Diagnostic Toast for debug
                    Toast.makeText(this, "Query ID: " + emailId + " | Found: " + totalFound, Toast.LENGTH_SHORT).show();

                    llCurrentBookings.removeAllViews();
                    llPreviousBookings.removeAllViews();
                    llPendingBookings.removeAllViews();

                    List<DocumentSnapshot> pending = new ArrayList<>();
                    List<DocumentSnapshot> current = new ArrayList<>();
                    List<DocumentSnapshot> previous = new ArrayList<>();
                    long now = System.currentTimeMillis();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Timestamp out = doc.getTimestamp("checkOut");
                        long checkoutMillis = out != null ? out.toDate().getTime() : 0L;
                        String status = doc.getString("status");
                        
                        if ("pending_validation".equalsIgnoreCase(status)) {
                            pending.add(doc);
                        } else if ("checked_out".equalsIgnoreCase(status)
                                || (checkoutMillis > 0 && checkoutMillis < now)) {
                            previous.add(doc);
                        } else {
                            current.add(doc);
                        }
                    }

                    renderBookingSection(llPendingBookings, tvPendingBookingsEmpty, pending,
                            getString(R.string.no_pending_bookings));
                    renderBookingSection(llCurrentBookings, tvCurrentBookingsEmpty, current,
                            getString(R.string.no_active_bookings));
                    renderBookingSection(llPreviousBookings, tvPreviousBookingsEmpty, previous,
                            getString(R.string.no_previous_bookings));
                });
    }

    private void renderBookingSection(LinearLayout container, TextView emptyView,
            List<DocumentSnapshot> docs, String emptyText) {
        container.removeAllViews();
        if (docs.isEmpty()) {
            emptyView.setText(emptyText);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DocumentSnapshot doc : docs) {
            View row = inflater.inflate(R.layout.item_customer_booking, container, false);
            TextView tvStatus = row.findViewById(R.id.tvBookingStatus);
            TextView tvRef = row.findViewById(R.id.tvBookingRef);
            TextView tvMeta = row.findViewById(R.id.tvBookingMeta);

            String status = doc.getString("status");
            if (status == null || status.isEmpty()) {
                status = "—";
            }
            String statusText = status.replace('_', ' ').toUpperCase(Locale.ROOT);
            tvStatus.setText(statusText);

            if ("pending_validation".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(0xFFFF9800)); // Warning Orange
                tvStatus.setTextColor(0xFFFFFFFF);
            } else if ("confirmed".equalsIgnoreCase(status) || "booked".equalsIgnoreCase(status) || "in_house".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(0xFF4CAF50)); // Success Green
                tvStatus.setTextColor(0xFFFFFFFF);
            }

            String ref = value(doc, "transactionRef");
            tvRef.setText(getString(R.string.booking_ref_line, ref));

            String rooms = value(doc, "rooms");
            Timestamp in = doc.getTimestamp("checkIn");
            Timestamp out = doc.getTimestamp("checkOut");
            StringBuilder meta = new StringBuilder();
            meta.append(getString(R.string.booking_rooms_line, rooms));
            if (in != null) {
                meta.append("\n").append(getString(R.string.booking_checkin_line, formatTs(in)));
            }
            if (out != null) {
                meta.append("\n").append(getString(R.string.booking_checkout_line, formatTs(out)));
            }
            tvMeta.setText(meta.toString());

            container.addView(row);
        }
    }

    private String formatTs(Timestamp ts) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(ts.toDate());
    }

    private String value(DocumentSnapshot doc, String key) {
        Object v = doc.get(key);
        return v == null ? "—" : v.toString();
    }

    @Override
    protected void onDestroy() {
        if (bookingListener != null) bookingListener.remove();
        super.onDestroy();
    }

    private void scrollToSection(View target) {
        if (target == null || scrollDashboard == null) {
            return;
        }
        scrollDashboard.post(() -> {
            int scrollY = 0;
            View v = target;
            while (true) {
                ViewParent p = v.getParent();
                if (p == scrollDashboard) {
                    scrollY += v.getTop();
                    break;
                }
                if (!(p instanceof View)) {
                    return;
                }
                scrollY += v.getTop();
                v = (View) p;
            }
            scrollDashboard.smoothScrollTo(0, Math.max(0, scrollY - 24));
        });
    }

    private void setNavSelection(int index) {
        int primary = ContextCompat.getColor(this, R.color.primary);
        int muted = ContextCompat.getColor(this, R.color.text_secondary);

        navTabHome.setBackgroundResource(index == 0 ? R.drawable.bg_nav_tab_selected : R.drawable.bg_nav_tab_unselected);
        navTabBookings.setBackgroundResource(index == 1 ? R.drawable.bg_nav_tab_selected : R.drawable.bg_nav_tab_unselected);
        navTabProfile.setBackgroundResource(index == 2 ? R.drawable.bg_nav_tab_selected : R.drawable.bg_nav_tab_unselected);

        tvNavHome.setTextColor(index == 0 ? primary : muted);
        tvNavBookings.setTextColor(index == 1 ? primary : muted);
        tvNavProfile.setTextColor(index == 2 ? primary : muted);
        tvNavHome.setTypeface(null, index == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tvNavBookings.setTypeface(null, index == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tvNavProfile.setTypeface(null, index == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        ImageViewCompat.setImageTintList(navIconHome, ColorStateList.valueOf(index == 0 ? primary : muted));
        ImageViewCompat.setImageTintList(navIconBookings, ColorStateList.valueOf(index == 1 ? primary : muted));
        ImageViewCompat.setImageTintList(navIconProfile, ColorStateList.valueOf(index == 2 ? primary : muted));
    }
}
