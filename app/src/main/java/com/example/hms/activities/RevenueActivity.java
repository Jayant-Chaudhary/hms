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

public class RevenueActivity extends AppCompatActivity {

    private EditText etAmount;
    private Spinner spinnerTag;
    private Button btnAddTransaction;
    private ListView transactionListView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue);

        db = FirebaseFirestore.getInstance();
        etAmount = findViewById(R.id.etAmount);
        spinnerTag = findViewById(R.id.spinnerTag);
        btnAddTransaction = findViewById(R.id.btnAddTransaction);
        transactionListView = findViewById(R.id.transactionListView);

        String[] tags = {"Revenue", "Expense"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tags);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTag.setAdapter(adapter);

        btnAddTransaction.setOnClickListener(v -> {
            String amount = etAmount.getText().toString();
            String tag = spinnerTag.getSelectedItem().toString();

            if (!amount.isEmpty()) {
                addTransaction(amount, tag);
            } else {
                Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTransaction(String amount, String tag) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("amount", Double.parseDouble(amount));
        transaction.put("tag", tag);
        transaction.put("timestamp", System.currentTimeMillis());

        db.collection("transactions").add(transaction)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Transaction added", Toast.LENGTH_SHORT).show();
                    etAmount.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding transaction", Toast.LENGTH_SHORT).show();
                });
    }
}