package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.activities.admin.AnalyticsActivity;
import com.example.hms.activities.admin.FinanceManagerActivity;
import com.example.hms.activities.admin.RoleAccessManagerActivity;
import com.example.hms.activities.admin.RoomLayoutManagerActivity;
import com.example.hms.auth.authManager;
import com.example.hms.utils.SessionManager;
import com.example.hms.utils.ThemeManager;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private RecyclerView rvStaffActivity;
    private TextView tvGreetingTitle, tvFinanceAmount, tvOperatingExpensesAmount, 
                     tvOccupancyPct, tvSatisfactionScore, tvRoomReadyCount, 
                     tvRoomCleaningCount, tvRoomMaintCount;
    
    private EditText etHotelDisplayName, etHotelLatitude, etHotelLongitude;
    private EditText etGlobalUpiId, etGlobalPayeeName;

    private FirebaseFirestore db;
    private authManager auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = new authManager();
        sessionManager = new SessionManager(this);

        // Bind Views matching activity_admin_dashboard.xml
        tvGreetingTitle = findViewById(R.id.tvGreetingTitle);
        tvFinanceAmount = findViewById(R.id.tvFinanceAmount);
        tvOperatingExpensesAmount = findViewById(R.id.tvOperatingExpensesAmount);
        tvOccupancyPct = findViewById(R.id.tvOccupancyPct);
        tvSatisfactionScore = findViewById(R.id.tvSatisfactionScore);
        tvRoomReadyCount = findViewById(R.id.tvRoomReadyCount);
        tvRoomCleaningCount = findViewById(R.id.tvRoomCleaningCount);
        tvRoomMaintCount = findViewById(R.id.tvRoomMaintCount);
        rvStaffActivity = findViewById(R.id.rvStaffActivity);

        // Navigation Tabs - FIXED: Added all listeners and missing imports
        View navTabFinance = findViewById(R.id.navTabFinance);
        View navTabAnalytics = findViewById(R.id.navTabAnalytics);
        View navTabRooms = findViewById(R.id.navTabRooms);
        View navTabAccess = findViewById(R.id.navTabAccess);

        if (navTabFinance != null) {
            navTabFinance.setOnClickListener(v -> 
                startActivity(new Intent(this, FinanceManagerActivity.class)));
        }
        if (navTabAnalytics != null) {
            navTabAnalytics.setOnClickListener(v -> 
                startActivity(new Intent(this, AnalyticsActivity.class)));
        }
        if (navTabRooms != null) {
            navTabRooms.setOnClickListener(v -> 
                startActivity(new Intent(this, RoomLayoutManagerActivity.class)));
        }
        if (navTabAccess != null) {
            navTabAccess.setOnClickListener(v -> 
                startActivity(new Intent(this, RoleAccessManagerActivity.class)));
        }

        View titleLogo = findViewById(R.id.tvAdminTitle);
        if (titleLogo != null) {
            titleLogo.setOnClickListener(v -> 
                startActivity(new Intent(this, SettingsActivity.class)));
        }

        // FAB logic
        findViewById(R.id.fabAdmin).setOnClickListener(v -> {
            Toast.makeText(this, "Admin quick action", Toast.LENGTH_SHORT).show();
        });

        // Avatar Logout
        View avatar = findViewById(R.id.ivAdminAvatar);
        if (avatar != null) {
            avatar.setOnClickListener(v -> {
                auth.getAuth().signOut();
                sessionManager.clearSession();
                Intent i = new Intent(this, loginPage.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            });
        }

        setupUserInfo();
        setupRecyclerView();
        setupConfig();
        fetchDashboardStats();
    }

    private void setupConfig() {
        // Location binds
        etHotelDisplayName = findViewById(R.id.etHotelDisplayName);
        etHotelLatitude = findViewById(R.id.etHotelLatitude);
        etHotelLongitude = findViewById(R.id.etHotelLongitude);
        Button btnSaveLoc = findViewById(R.id.btnSaveHotelLocation);

        // UPI binds
        etGlobalUpiId = findViewById(R.id.etGlobalUpiId);
        etGlobalPayeeName = findViewById(R.id.etGlobalPayeeName);
        Button btnSaveUpi = findViewById(R.id.btnSavePaymentConfig);

        // Load existing
        db.collection("system_config").document("hotel_details").get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (etHotelDisplayName != null) etHotelDisplayName.setText(doc.getString("displayName"));
                if (etHotelLatitude != null) etHotelLatitude.setText(String.valueOf(doc.get("latitude")));
                if (etHotelLongitude != null) etHotelLongitude.setText(String.valueOf(doc.get("longitude")));
            }
        });

        db.collection("system_config").document("payment_settings").get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (etGlobalUpiId != null) etGlobalUpiId.setText(doc.getString("upiId"));
                if (etGlobalPayeeName != null) etGlobalPayeeName.setText(doc.getString("payeeName"));
            }
        });

        if (btnSaveLoc != null) {
            btnSaveLoc.setOnClickListener(v -> {
                Map<String, Object> data = new HashMap<>();
                data.put("displayName", etHotelDisplayName.getText().toString());
                try {
                    data.put("latitude", Double.parseDouble(etHotelLatitude.getText().toString()));
                    data.put("longitude", Double.parseDouble(etHotelLongitude.getText().toString()));
                } catch (Exception e) {}
                db.collection("system_config").document("hotel_details").set(data)
                        .addOnSuccessListener(a -> Toast.makeText(this, "Location saved", Toast.LENGTH_SHORT).show());
            });
        }

        if (btnSaveUpi != null) {
            btnSaveUpi.setOnClickListener(v -> {
                Map<String, Object> data = new HashMap<>();
                data.put("upiId", etGlobalUpiId.getText().toString());
                data.put("payeeName", etGlobalPayeeName.getText().toString());
                db.collection("system_config").document("payment_settings").set(data)
                        .addOnSuccessListener(a -> Toast.makeText(this, "UPI settings updated", Toast.LENGTH_SHORT).show());
            });
        }
    }

    private void setupUserInfo() {
        FirebaseUser user = auth.getAuth().getCurrentUser();
        if (user != null && tvGreetingTitle != null) {
            tvGreetingTitle.setText("Morning, Executive.");
        }
    }

    private void setupRecyclerView() {
        if (rvStaffActivity != null) {
            rvStaffActivity.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void fetchDashboardStats() {
        // Fetch financial totals
        db.collection("transactions").get().addOnSuccessListener(snapshots -> {
            double revenue = 0;
            double expenses = 0;
            for (QueryDocumentSnapshot doc : snapshots) {
                Double amt = doc.getDouble("amount");
                String tag = doc.getString("tag");
                if (amt != null && tag != null) {
                    if ("Revenue".equals(tag)) revenue += amt;
                    else if ("Expense".equals(tag)) expenses += amt;
                }
            }
            if (tvFinanceAmount != null) tvFinanceAmount.setText(String.format("₹%,.0f", revenue));
            if (tvOperatingExpensesAmount != null) tvOperatingExpensesAmount.setText(String.format("₹%,.0f", expenses));
        });

        // Fetch room status dynamically from room layout
        db.collection("rooms").get().addOnSuccessListener(snapshots -> {
            int ready = 0, cleaning = 0, maint = 0;
            for (QueryDocumentSnapshot doc : snapshots) {
                Boolean isMaint = doc.getBoolean("underMaintenance");
                String hk = doc.getString("housekeepingStatus");
                
                if (Boolean.TRUE.equals(isMaint)) {
                    maint++;
                } else if ("cleaning".equalsIgnoreCase(hk)) {
                    cleaning++;
                } else if ("ready".equalsIgnoreCase(hk)) {
                    ready++;
                }
            }
            if (tvRoomReadyCount != null) tvRoomReadyCount.setText(String.valueOf(ready));
            if (tvRoomCleaningCount != null) tvRoomCleaningCount.setText(String.valueOf(cleaning));
            if (tvRoomMaintCount != null) tvRoomMaintCount.setText(String.valueOf(maint));
            
            if (snapshots.size() > 0 && tvOccupancyPct != null) {
                // Occupied = Total - Ready - Cleaning - Maint (Simplified logic)
                int occupied = snapshots.size() - ready - cleaning - maint;
                int pct = (occupied * 100) / snapshots.size();
                tvOccupancyPct.setText(pct + "%");
            }
        });
    }
}