package com.example.hms.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.model.admin.FinanceTransaction;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinanceManagerActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<FinanceTransaction> allItems = new ArrayList<>();
    private final List<FinanceTransaction> filteredItems = new ArrayList<>();
    private FinanceAdapter adapter;
    private Spinner spinnerTypeFilter;
    private EditText etSearch;

    private static final Map<String, List<String>> SUB_CATEGORIES = new HashMap<>();
    static {
        SUB_CATEGORIES.put("expense", Arrays.asList("Groceries", "Utilities", "Maintenance", "Payroll", "Other"));
        SUB_CATEGORIES.put("revenue", Arrays.asList("Room Booking", "Food", "Laundry", "Service", "Other"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_finance_manager);

        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter);
        etSearch = findViewById(R.id.etSearchFinance);
        RecyclerView rv = findViewById(R.id.rvFinance);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FinanceAdapter(filteredItems);
        rv.setAdapter(adapter);

        spinnerTypeFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"all", "revenue", "expense"}));
        spinnerTypeFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { applyFilters(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnAddFinance).setOnClickListener(v -> showEditDialog(null));
        loadFinance();
    }

    private void loadFinance() {
        repo.finance().orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    allItems.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        FinanceTransaction t = doc.toObject(FinanceTransaction.class);
                        t.id = doc.getId();
                        allItems.add(t);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void applyFilters() {
        String typeFilter = spinnerTypeFilter.getSelectedItem().toString();
        String search = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        filteredItems.clear();
        for (FinanceTransaction t : allItems) {
            boolean typeOk = "all".equals(typeFilter) || typeFilter.equalsIgnoreCase(safe(t.type));
            boolean searchOk = search.isEmpty() ||
                    safe(t.category).toLowerCase(Locale.ROOT).contains(search) ||
                    safe(t.subCategory).toLowerCase(Locale.ROOT).contains(search) ||
                    safe(t.microCategory).toLowerCase(Locale.ROOT).contains(search) ||
                    safe(t.note).toLowerCase(Locale.ROOT).contains(search);
            if (typeOk && searchOk) filteredItems.add(t);
        }
        adapter.notifyDataSetChanged();
    }

    private void showEditDialog(FinanceTransaction existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_admin_finance_transaction, null, false);
        Spinner spinnerType = v.findViewById(R.id.spinnerType);
        EditText etAmount = v.findViewById(R.id.etAmount);
        Spinner spinnerCategory = v.findViewById(R.id.spinnerCategory);
        Spinner spinnerSubCategory = v.findViewById(R.id.spinnerSubCategory);
        EditText etMicro = v.findViewById(R.id.etMicroCategory);
        EditText etNote = v.findViewById(R.id.etNote);
        EditText etBooking = v.findViewById(R.id.etSourceBooking);

        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"revenue", "expense"}));

        android.widget.AdapterView.OnItemSelectedListener typeListener = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String type = spinnerType.getSelectedItem().toString();
                List<String> cats = SUB_CATEGORIES.get(type);
                if (cats == null) cats = Arrays.asList("Other");
                spinnerCategory.setAdapter(new ArrayAdapter<>(FinanceManagerActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, cats));
                spinnerSubCategory.setAdapter(new ArrayAdapter<>(FinanceManagerActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, cats));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        };
        spinnerType.setOnItemSelectedListener(typeListener);
        typeListener.onItemSelected(null, null, 0, 0);

        if (existing != null) {
            setSpinner(spinnerType, safe(existing.type));
            etAmount.setText(String.valueOf(existing.amount));
            etMicro.setText(safe(existing.microCategory));
            etNote.setText(safe(existing.note));
            etBooking.setText(safe(existing.sourceBookingId));
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add transaction" : "Edit transaction")
                .setView(v)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    double amount;
                    try { amount = Double.parseDouble(etAmount.getText().toString().trim()); }
                    catch (Exception e) { amount = 0; }
                    if (amount <= 0) {
                        Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", spinnerType.getSelectedItem().toString());
                    map.put("amount", amount);
                    map.put("category", spinnerCategory.getSelectedItem().toString());
                    map.put("subCategory", spinnerSubCategory.getSelectedItem().toString());
                    map.put("microCategory", etMicro.getText().toString().trim());
                    map.put("note", etNote.getText().toString().trim());
                    map.put("sourceBookingId", etBooking.getText().toString().trim());
                    map.put("date", Timestamp.now());
                    map.put("monthKey", AdminFirestoreRepository.monthKeyNow());
                    if (existing == null) {
                        repo.finance().add(map).addOnSuccessListener(x -> loadFinance());
                    } else {
                        repo.finance().document(existing.id).update(map).addOnSuccessListener(x -> loadFinance());
                    }
                }).show();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static void setSpinner(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equalsIgnoreCase(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private class FinanceAdapter extends RecyclerView.Adapter<FinanceAdapter.Holder> {
        private final List<FinanceTransaction> items;
        FinanceAdapter(List<FinanceTransaction> items) { this.items = items; }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_finance_transaction, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            FinanceTransaction t = items.get(position);
            h.tvTitle.setText(String.format(Locale.getDefault(), "%s • ₹%,.0f",
                    safe(t.type).toUpperCase(Locale.ROOT), t.amount));
            h.tvMeta.setText(String.format(Locale.getDefault(), "%s > %s > %s\n%s",
                    safe(t.category), safe(t.subCategory), safe(t.microCategory), safe(t.note)));
            h.btnEdit.setOnClickListener(v -> showEditDialog(t));
            h.btnDelete.setOnClickListener(v ->
                    repo.finance().document(t.id).delete().addOnSuccessListener(x -> loadFinance()));
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMeta;
            Button btnEdit, btnDelete;
            Holder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvMeta = itemView.findViewById(R.id.tvMeta);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
