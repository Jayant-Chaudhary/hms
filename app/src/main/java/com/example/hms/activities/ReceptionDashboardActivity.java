package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.utils.BookingDataSync;
import com.example.hms.utils.ReceptionBookingDraft;
import com.example.hms.utils.SessionManager;
import com.example.hms.utils.ThemeManager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceptionDashboardActivity extends AppCompatActivity {

    private static final long DUE_WINDOW_MS = 6L * 60L * 60L * 1000L;

    private SessionManager sessionManager;
    private NestedScrollView panelHome;
    private LinearLayout panelDue;
    private RecyclerView rvDue;
    private TextView tvDueEmpty;
    private final List<DueItem> dueItems = new ArrayList<>();
    private DueAdapter dueAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_dashboard);

        sessionManager = new SessionManager(this);

        TextView tvEmail = findViewById(R.id.tvReceptionistEmail);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null && user.getEmail() != null ? user.getEmail() : sessionManager.getEmail();
        if (email == null || email.isEmpty()) {
            email = "—";
        }
        tvEmail.setText(email);

        panelHome = findViewById(R.id.panelHome);
        panelDue = findViewById(R.id.panelDueCheckouts);
        rvDue = findViewById(R.id.rvDueCheckouts);
        tvDueEmpty = findViewById(R.id.tvDueEmpty);

        rvDue.setLayoutManager(new LinearLayoutManager(this));
        dueAdapter = new DueAdapter();
        rvDue.setAdapter(dueAdapter);

        TabLayout tabs = findViewById(R.id.tabReceptionSections);
        tabs.addTab(tabs.newTab().setText("Home"));
        tabs.addTab(tabs.newTab().setText("Due checkouts"));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    panelHome.setVisibility(View.VISIBLE);
                    panelDue.setVisibility(View.GONE);
                } else {
                    panelHome.setVisibility(View.GONE);
                    panelDue.setVisibility(View.VISIBLE);
                    loadDueCheckouts();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        findViewById(R.id.btnNewCustomer).setOnClickListener(v -> {
            ReceptionBookingDraft.reset();
            startActivity(new Intent(this, ReceptionCustomerRegistrationActivity.class));
        });

        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, ReceptionHistoryActivity.class)));

        findViewById(R.id.btnArrivals).setOnClickListener(v ->
                startActivity(new Intent(this, ReceptionArrivalsActivity.class)));
        findViewById(R.id.btnReceptionSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.tvLogout).setOnClickListener(v -> tryLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (panelDue != null && panelDue.getVisibility() == View.VISIBLE) {
            loadDueCheckouts();
        }
    }

    private void tryLogout() {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereIn("status", java.util.Arrays.asList("in_house", "due_checkout"))
                .get()
                .addOnSuccessListener(snapshots -> {
                    long now = System.currentTimeMillis();
                    int pending = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        DueItem d = parseDueItem(doc);
                        if (d != null && isDueForTab(d.status, d.checkOut, now)) {
                            pending++;
                        }
                    }
                    if (pending > 0) {
                        new AlertDialog.Builder(this)
                                .setTitle("Pending checkouts")
                                .setMessage("You have " + pending + " guest(s) due for checkout. Please confirm checkout or collect balance in the Due checkouts tab before signing out.")
                                .setPositiveButton("Open Due checkouts", (dialog, which) -> {
                                    TabLayout tabs = findViewById(R.id.tabReceptionSections);
                                    if (tabs != null && tabs.getTabCount() > 1) {
                                        TabLayout.Tab t = tabs.getTabAt(1);
                                        if (t != null) {
                                            t.select();
                                        }
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .setNeutralButton("Log out anyway", (dialog, which) -> performLogout())
                                .show();
                    } else {
                        performLogout();
                    }
                })
                .addOnFailureListener(e -> performLogout());
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        sessionManager.clearSession();
        Intent i = new Intent(this, loginPage.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void loadDueCheckouts() {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereIn("status", java.util.Arrays.asList("in_house", "due_checkout"))
                .get()
                .addOnSuccessListener(snapshots -> {
                    long now = System.currentTimeMillis();
                    long dueWindow = now + DUE_WINDOW_MS;
                    dueItems.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String id = doc.getId();
                        String st = doc.getString("status");
                        Timestamp checkOut = doc.getTimestamp("checkOut");

                        if ("in_house".equalsIgnoreCase(st) && checkOut != null) {
                            long out = checkOut.toDate().getTime();
                            if (out <= dueWindow) {
                                FirebaseFirestore.getInstance().collection("bookings").document(id)
                                        .update("status", "due_checkout");
                                st = "due_checkout";
                            }
                        }

                        DueItem d = parseDueItem(doc);
                        if (d != null && isDueForTab(st, checkOut, now)) {
                            d.status = st;
                            dueItems.add(d);
                        }
                    }
                    Collections.sort(dueItems, Comparator.comparingLong(a -> a.checkOut == null ? Long.MAX_VALUE : a.checkOut.toDate().getTime()));
                    dueAdapter.notifyDataSetChanged();
                    boolean empty = dueItems.isEmpty();
                    tvDueEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rvDue.setVisibility(empty ? View.GONE : View.VISIBLE);
                });
    }

    private static boolean isDueForTab(String status, Timestamp checkOut, long now) {
        if (status == null) {
            return false;
        }
        if ("checked_out".equalsIgnoreCase(status) || "booked".equalsIgnoreCase(status)) {
            return false;
        }
        long dueWindow = now + DUE_WINDOW_MS;
        if ("due_checkout".equalsIgnoreCase(status)) {
            return true;
        }
        if ("in_house".equalsIgnoreCase(status) && checkOut != null) {
            long out = checkOut.toDate().getTime();
            return out <= dueWindow;
        }
        return false;
    }

    private DueItem parseDueItem(QueryDocumentSnapshot doc) {
        try {
            DueItem d = new DueItem();
            d.id = doc.getId();
            d.customerName = doc.getString("customerName");
            d.status = doc.getString("status");
            d.checkOut = doc.getTimestamp("checkOut");
            double total = doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0;
            double paid = doc.getDouble("amountPaid") != null ? doc.getDouble("amountPaid") : total;
            Double bd = doc.getDouble("balanceDue");
            d.balanceDue = bd != null ? bd : Math.max(0, total - paid);
            d.totalAmount = total;
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    private void openPayBalance(DueItem item) {
        if (item.balanceDue <= 0.01) {
            Toast.makeText(this, "No balance due", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, ReceptionCheckoutPaymentActivity.class);
        i.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_BOOKING_ID, item.id);
        i.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_AMOUNT, item.balanceDue);
        i.putExtra(ReceptionCheckoutPaymentActivity.EXTRA_GUEST_LABEL, item.customerName);
        startActivity(i);
    }

    private void promptExtra(DueItem item) {
        final EditText etAmount = new EditText(this);
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setHint("Amount (₹)");
        final EditText etNote = new EditText(this);
        etNote.setHint("Note (optional)");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(48, 24, 48, 0);
        box.addView(etAmount);
        box.addView(etNote);

        new AlertDialog.Builder(this)
                .setTitle("Add extra charge")
                .setView(box)
                .setPositiveButton("Add", (d, w) -> {
                    String as = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                    double amt;
                    try {
                        amt = Double.parseDouble(as);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
                    BookingDataSync.appendExtraCharge(item.id, amt, note)
                            .addOnSuccessListener(unused -> loadDueCheckouts())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    e.getMessage() != null ? e.getMessage() : "Failed", Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmCheckout(DueItem item) {
        if (item.balanceDue > 0.01) {
            Toast.makeText(this, "Collect balance before checkout", Toast.LENGTH_LONG).show();
            openPayBalance(item);
            return;
        }
        BookingDataSync.confirmCheckout(item.id)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Checked out", Toast.LENGTH_SHORT).show();
                    loadDueCheckouts();
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "Checkout failed";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    if (msg.toLowerCase(Locale.ROOT).contains("balance")) {
                        openPayBalance(item);
                    }
                });
    }

    private static class DueItem {
        String id;
        String customerName;
        String status;
        Timestamp checkOut;
        double balanceDue;
        double totalAmount;
    }

    private class DueAdapter extends RecyclerView.Adapter<DueAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reception_due_checkout, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            DueItem item = dueItems.get(position);
            String name = item.customerName == null ? "Guest" : item.customerName;
            h.title.setText(name + " • " + (item.status == null ? "-" : item.status));
            String out = item.checkOut == null ? "-"
                    : new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(item.checkOut.toDate().getTime()));
            h.meta.setText("Checkout: " + out + " | Total: ₹" + String.format(Locale.getDefault(), "%,.0f", item.totalAmount));
            if (item.balanceDue > 0.01) {
                h.balance.setVisibility(View.VISIBLE);
                h.balance.setText("Balance due: ₹" + String.format(Locale.getDefault(), "%,.0f", item.balanceDue));
            } else {
                h.balance.setVisibility(View.GONE);
            }
            h.btnPay.setOnClickListener(v -> openPayBalance(item));
            h.btnExtra.setOnClickListener(v -> promptExtra(item));
            h.btnConfirm.setOnClickListener(v -> confirmCheckout(item));
        }

        @Override
        public int getItemCount() {
            return dueItems.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView meta;
            final TextView balance;
            final Button btnPay;
            final Button btnExtra;
            final Button btnConfirm;

            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvDueTitle);
                meta = itemView.findViewById(R.id.tvDueMeta);
                balance = itemView.findViewById(R.id.tvBalanceDue);
                btnPay = itemView.findViewById(R.id.btnPayBalance);
                btnExtra = itemView.findViewById(R.id.btnAddExtra);
                btnConfirm = itemView.findViewById(R.id.btnConfirmCheckout);
            }
        }
    }
}
