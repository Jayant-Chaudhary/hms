package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.auth.authManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private BarChart barChart;
    private RecyclerView rvPresentStaff;
    private Button btnViewFinance, btnViewAttendance, btnManageRoles, btnLogout;
    private TextView tvWelcome, tvRole, tvTotalRevenue, tvTotalExpense, tvNetBalance, tvPresentCount;
    private ProgressBar progressFinance;

    private FirebaseFirestore db;
    private authManager auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = new authManager();

        // Bind Views
        tvWelcome = findViewById(R.id.tvWelcome);
        tvRole = findViewById(R.id.tvRole);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        
        progressFinance = findViewById(R.id.progressFinance);
        barChart = findViewById(R.id.barChart);
        rvPresentStaff = findViewById(R.id.rvPresentStaff);
        
        btnViewFinance = findViewById(R.id.btnViewFinance);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnManageRoles = findViewById(R.id.btnManageRoles);
        btnLogout = findViewById(R.id.btnLogout);

        setupUserInfo();
        setupRecyclerView();
        fetchFinanceData();
        fetchAttendanceData();

        // Click Listeners
        btnViewFinance.setOnClickListener(v -> startActivity(new Intent(this, RevenueActivity.class)));
        btnViewAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        btnManageRoles.setOnClickListener(v -> startActivity(new Intent(this, ManageRolesActivity.class)));
        
        btnLogout.setOnClickListener(v -> {
            auth.getAuth().signOut();
            startActivity(new Intent(this, loginPage.class));
            finish();
        });
    }

    private void setupUserInfo() {
        FirebaseUser user = auth.getAuth().getCurrentUser();
        if (user != null) {
            tvWelcome.setText("Welcome, Admin");
            tvRole.setText("ADMIN");
        }
    }

    private void setupRecyclerView() {
        rvPresentStaff.setLayoutManager(new LinearLayoutManager(this));
        // You would typically set an adapter here
    }

    private void fetchFinanceData() {
        progressFinance.setVisibility(View.VISIBLE);
        db.collection("transactions")
                .get()
                .addOnCompleteListener(task -> {
                    progressFinance.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        double totalRevenue = 0;
                        double totalExpense = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            double amount = document.getDouble("amount");
                            String tag = document.getString("tag");

                            if ("Revenue".equals(tag)) {
                                totalRevenue += amount;
                            } else if ("Expense".equals(tag)) {
                                totalExpense += amount;
                            }
                        }

                        updateFinanceUI(totalRevenue, totalExpense);
                    } else {
                        Toast.makeText(this, "Error fetching finance data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateFinanceUI(double revenue, double expense) {
        tvTotalRevenue.setText(String.format("₹%.0f", revenue));
        tvTotalExpense.setText(String.format("₹%.0f", expense));
        tvNetBalance.setText(String.format("₹%.0f", revenue - expense));

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) revenue));
        entries.add(new BarEntry(1f, (float) expense));

        BarDataSet set = new BarDataSet(entries, "Revenue vs Expenses");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextSize(12f);

        BarData data = new BarData(set);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void fetchAttendanceData() {
        db.collection("attendance")
                .whereEqualTo("status", "Present")
                // In a real app, you'd filter by today's date
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        tvPresentCount.setText(count + " staff present today");
                    }
                });
    }
}