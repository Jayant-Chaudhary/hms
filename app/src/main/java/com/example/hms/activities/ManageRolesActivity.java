package com.example.hms.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.hms.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ManageRolesActivity extends AppCompatActivity {

    private EditText etEmail;
    private Spinner roleSpinner;
    private Button btnAddUser;
    private ListView userRolesListView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_roles);

        db = FirebaseFirestore.getInstance();
        etEmail = findViewById(R.id.etEmail);
        roleSpinner = findViewById(R.id.roleSpinner);
        btnAddUser = findViewById(R.id.btnAddUser);
        userRolesListView = findViewById(R.id.userRolesListView);

        String[] roles = {"Admin", "Receptionist", "Staff"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        btnAddUser.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString();

            if (!email.isEmpty()) {
                assignRole(email, role);
            } else {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void assignRole(String email, String role) {
        Map<String, Object> userRole = new HashMap<>();
        userRole.put("email", email);
        userRole.put("role", role);

        db.collection("user_roles").document(email)
                .set(userRole)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ManageRolesActivity.this, "Role assigned successfully", Toast.LENGTH_SHORT).show();
                    etEmail.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ManageRolesActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}