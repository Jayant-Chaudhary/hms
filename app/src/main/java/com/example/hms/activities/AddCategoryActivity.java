package com.example.hms.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.model.CategoryNode;
import com.example.hms.utils.FirestoreHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddCategoryActivity extends AppCompatActivity {

    private TextInputEditText etCategoryName;
    private Spinner spinnerParentCategory;
    private Button btnSaveCategory;

    private FirebaseFirestore db;
    private List<CategoryNode> parentCategoriesList = new ArrayList<>();
    private List<String> parentCategoryNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        // Initialize Views
        etCategoryName = findViewById(R.id.etCategoryName);
        spinnerParentCategory = findViewById(R.id.spinnerParentCategory);
        btnSaveCategory = findViewById(R.id.btnSaveCategory);

        db = FirebaseFirestore.getInstance();

        // Load categories into the spinner
        loadParentCategories();

        btnSaveCategory.setOnClickListener(v -> saveCategoryToFirestore());
    }

    private void loadParentCategories() {
        // Fetch only categories that DO NOT have a parent (meaning they are main categories)
        db.collection(FirestoreHelper.COL_CATEGORIES)
                .whereEqualTo("parentId", null)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    parentCategoriesList.clear();
                    parentCategoryNames.clear();

                    // Add a default option for creating a Main Category
                    parentCategoryNames.add("-- None (Create Main Category) --");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        CategoryNode category = doc.toObject(CategoryNode.class);
                        parentCategoriesList.add(category);
                        parentCategoryNames.add(category.getName());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            parentCategoryNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerParentCategory.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show());
    }

    private void saveCategoryToFirestore() {
        String name = etCategoryName.getText().toString().trim();
        if (name.isEmpty()) {
            etCategoryName.setError("Name is required");
            return;
        }

        int selectedPosition = spinnerParentCategory.getSelectedItemPosition();
        String parentId = null; // Default to null (Main category)

        // If user selected anything other than the first option, it's a subcategory
        if (selectedPosition > 0) {
            // Subtract 1 because the first item in the names list is the "-- None --" option
            parentId = parentCategoriesList.get(selectedPosition - 1).getId();
        }

        String categoryId = UUID.randomUUID().toString();
        CategoryNode newCategory = new CategoryNode(categoryId, name, parentId, ""); // Empty string for icon for now

        db.collection(FirestoreHelper.COL_CATEGORIES).document(categoryId)
                .set(newCategory)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Category Saved!", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}