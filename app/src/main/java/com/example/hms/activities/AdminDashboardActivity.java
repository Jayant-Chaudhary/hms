package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize buttons
        Button btnAddCategory = findViewById(R.id.btnAddCategory);
        Button btnAddTransaction = findViewById(R.id.btnAddTransaction);

        // Set listeners
        btnAddCategory.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AddCategoryActivity.class);
            startActivity(intent);
        });

        btnAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });
    }
}