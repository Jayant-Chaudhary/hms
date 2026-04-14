package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.activities.admin.AnalyticsActivity;
import com.example.hms.activities.admin.CustomerDataActivity;
import com.example.hms.activities.admin.FinanceManagerActivity;
import com.example.hms.activities.admin.RoleAccessManagerActivity;
import com.example.hms.activities.admin.RoomLayoutManagerActivity;
import com.example.hms.activities.admin.StayMonitorActivity;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        findViewById(R.id.btnFinanceManager).setOnClickListener(v ->
                startActivity(new Intent(this, FinanceManagerActivity.class)));
        findViewById(R.id.btnAnalytics).setOnClickListener(v ->
                startActivity(new Intent(this, AnalyticsActivity.class)));
        findViewById(R.id.btnRoomLayout).setOnClickListener(v ->
                startActivity(new Intent(this, RoomLayoutManagerActivity.class)));
        findViewById(R.id.btnRoleAccess).setOnClickListener(v ->
                startActivity(new Intent(this, RoleAccessManagerActivity.class)));
        findViewById(R.id.btnStayMonitor).setOnClickListener(v ->
                startActivity(new Intent(this, StayMonitorActivity.class)));
        findViewById(R.id.btnCustomerData).setOnClickListener(v ->
                startActivity(new Intent(this, CustomerDataActivity.class)));

        loadKpis();
    }

    private void loadKpis() {
        TextView tvRev = findViewById(R.id.tvTodayRevenue);
        TextView tvExp = findViewById(R.id.tvTodayExpense);
        TextView tvInHouse = findViewById(R.id.tvInHouse);
        TextView tvDue = findViewById(R.id.tvDueCheckout);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp todayStart = new Timestamp(new Date(cal.getTimeInMillis()));

        repo.finance()
                .whereGreaterThanOrEqualTo("date", todayStart)
                .get()
                .addOnSuccessListener(snapshots -> {
                    double rev = 0;
                    double exp = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String type = doc.getString("type");
                        Double amount = doc.getDouble("amount");
                        double a = amount == null ? 0 : amount;
                        if ("expense".equalsIgnoreCase(type)) exp += a;
                        else rev += a;
                    }
                    tvRev.setText(String.format(Locale.getDefault(), "Today Revenue\n₹%,.0f", rev));
                    tvExp.setText(String.format(Locale.getDefault(), "Today Expense\n₹%,.0f", exp));
                });

        repo.bookings()
                .whereEqualTo("status", "in_house")
                .get()
                .addOnSuccessListener(s -> tvInHouse.setText("In-house\n" + s.size()));

        repo.bookings()
                .whereEqualTo("status", "due_checkout")
                .get()
                .addOnSuccessListener(s -> tvDue.setText("Due Checkouts\n" + s.size()));
    }
}