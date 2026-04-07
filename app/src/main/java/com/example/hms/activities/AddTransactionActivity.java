package com.example.hms.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.model.CategoryNode;
import com.example.hms.utils.FirestoreHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {

    private TextInputEditText etAmount, etRemark, etTags;
    private RadioGroup rgType;
    private Spinner spinnerCategory;
    private Button btnSaveTransaction;

    private FirebaseFirestore db;
    private FirestoreHelper firestoreHelper;
    private List<CategoryNode> categoryList = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Initialize Views
        etAmount = findViewById(R.id.etAmount);
        rgType = findViewById(R.id.rgType);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etRemark = findViewById(R.id.etRemark);
        etTags = findViewById(R.id.etTags);
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);

        db = FirebaseFirestore.getInstance();
        firestoreHelper = new FirestoreHelper();

        loadCategories();

        btnSaveTransaction.setOnClickListener(v -> saveTransaction());
    }

    private void loadCategories() {
        // Fetch all categories (both parent and subcategories) so the user can select exactly what they want
        db.collection(FirestoreHelper.COL_CATEGORIES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    categoryNames.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        CategoryNode category = doc.toObject(CategoryNode.class);
                        categoryList.add(category);
                        // Optional: Format the name so subcategories look indented in the dropdown
                        String displayName = category.getParentId() == null ? category.getName() : "   ↳ " + category.getName();
                        categoryNames.add(displayName);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            categoryNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show());
    }

    private void saveTransaction() {
        // 1. Get Amount
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etAmount.setError("Amount required");
            return;
        }
        double amount = Double.parseDouble(amountStr);

        // 2. Get Type (Income or Expense)
        String type = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "INCOME" : "EXPENSE";

        // 3. Get Category
        int selectedCatPos = spinnerCategory.getSelectedItemPosition();
        if (categoryList.isEmpty()) {
            Toast.makeText(this, "Please create a category first", Toast.LENGTH_SHORT).show();
            return;
        }
        String categoryId = categoryList.get(selectedCatPos).getId();

        // 4. Get Remark
        String remark = etRemark.getText().toString().trim();

        // 5. Process Tags (Split comma-separated string into a List)
        String tagsRaw = etTags.getText().toString().trim();
        List<String> tagsList = new ArrayList<>();
        if (!tagsRaw.isEmpty()) {
            // Split by comma and remove extra whitespace around tags
            String[] splitArray = tagsRaw.split(",");
            for (String tag : splitArray) {
                if (!tag.trim().isEmpty()) {
                    tagsList.add(tag.trim().toLowerCase()); // Lowercase makes searching easier later
                }
            }
        }

        // 6. Custom Attributes (Hardcoded example: You could add dynamic inputs for this later!)
        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("Device Platform", "Android Admin App");

        // TODO: Get real admin ID from your authManager/SessionManager
// Replace the hardcoded string with this:

        String currentAdminId = "unknown";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentAdminId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // 7. Save via Helper
        btnSaveTransaction.setEnabled(false); // Prevent double-clicking
        btnSaveTransaction.setText("Saving...");

        firestoreHelper.addTransaction(
                amount,
                type,
                categoryId,
                remark,
                tagsList,
                customAttributes,
                currentAdminId,
                new FirestoreHelper.OnTransactionAddListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(AddTransactionActivity.this, "Transaction Saved!", Toast.LENGTH_SHORT).show();
                        finish(); // Close screen
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(AddTransactionActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSaveTransaction.setEnabled(true);
                        btnSaveTransaction.setText("Save Transaction");
                    }
                }
        );
    }
}