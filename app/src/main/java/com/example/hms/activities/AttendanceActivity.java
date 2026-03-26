package com.example.hms.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.hms.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AttendanceActivity extends AppCompatActivity {

    private EditText etStaffName;
    private Button btnMarkPresent;
    private ListView attendanceListView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        db = FirebaseFirestore.getInstance();
        etStaffName = findViewById(R.id.etStaffName);
        btnMarkPresent = findViewById(R.id.btnMarkPresent);
        attendanceListView = findViewById(R.id.attendanceListView);

        btnMarkPresent.setOnClickListener(v -> {
            String name = etStaffName.getText().toString().trim();
            if (!name.isEmpty()) {
                markAttendance(name);
            } else {
                Toast.makeText(this, "Enter staff name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAttendance(String name) {
        Map<String, Object> attendance = new HashMap<>();
        attendance.put("name", name);
        attendance.put("status", "Present");
        attendance.put("timestamp", System.currentTimeMillis());

        db.collection("attendance").add(attendance)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Attendance marked", Toast.LENGTH_SHORT).show();
                    etStaffName.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error marking attendance", Toast.LENGTH_SHORT).show();
                });
    }
}