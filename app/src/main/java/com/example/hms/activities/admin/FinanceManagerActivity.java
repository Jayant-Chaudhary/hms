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
import com.example.hms.model.admin.FinanceCategory;
import com.example.hms.model.admin.FinanceTransaction;
import com.example.hms.utils.MoneyFormat;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.example.hms.utils.admin.FinanceCategoryRepository;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FinanceManagerActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<FinanceTransaction> allItems = new ArrayList<>();
    private final List<FinanceTransaction> filteredItems = new ArrayList<>();
    private FinanceAdapter adapter;
    private ChipGroup chipGroupType;
    private EditText etSearch;

    // Hardcoded Categories as per User Request
    private static final String[] EXPENSE_CATEGORIES = {
            "Groceries", "Electricity", "Gas", "Staff Salary", "Maintenance", "Other Expenses"
    };
    private static final String[] REVENUE_CATEGORIES = {
            "Room Booking", "Food & Beverage", "Laundry", "Miscellaneous Revenue"
    };

    private TextView tvTotalRevenue;
    private TextView tvTotalExpenses;
    private TextView tvMargin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_finance_manager);

        etSearch = findViewById(R.id.etSearchFinance);
        chipGroupType = findViewById(R.id.chipGroupType);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvMargin = findViewById(R.id.tvMargin);

        RecyclerView rv = findViewById(R.id.rvFinance);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FinanceAdapter(filteredItems);
        rv.setAdapter(adapter);

        chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        View btnAdd = findViewById(R.id.btnAddFinance);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showEditDialog(null));
        }

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
        String typeFilter = currentTypeFilter();
        String search = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        filteredItems.clear();
        for (FinanceTransaction t : allItems) {
            boolean typeOk = "all".equals(typeFilter) || typeFilter.equalsIgnoreCase(safe(t.type));
            boolean searchOk = search.isEmpty() ||
                    safe(t.category).toLowerCase(Locale.ROOT).contains(search) ||
                    safe(t.subCategory).toLowerCase(Locale.ROOT).contains(search) ||
                    safe(t.note).toLowerCase(Locale.ROOT).contains(search);
            if (typeOk && searchOk) filteredItems.add(t);
        }
        adapter.notifyDataSetChanged();
        updateSummaryForCurrentMonth(filteredItems);
    }

    private String currentTypeFilter() {
        int id = chipGroupType.getCheckedChipId();
        if (id == R.id.chipRevenue) return "revenue";
        if (id == R.id.chipExpense) return "expense";
        return "all";
    }

    private void updateSummaryForCurrentMonth(List<FinanceTransaction> list) {
        String mk = AdminFirestoreRepository.monthKeyNow();
        double rev = 0;
        double exp = 0;
        for (FinanceTransaction t : list) {
            if (t == null) continue;
            if (t.monthKey == null || !mk.equals(t.monthKey)) continue;
            if ("expense".equalsIgnoreCase(safe(t.type))) exp += t.amount;
            else rev += t.amount;
        }
        tvTotalRevenue.setText(MoneyFormat.format(this, rev));
        tvTotalExpenses.setText(MoneyFormat.format(this, exp));
        double margin = rev > 0 ? ((rev - exp) / rev) * 100d : 0d;
        tvMargin.setText(String.format(Locale.getDefault(), "MARGIN %.1f%%", margin));
    }

    private void showEditDialog(FinanceTransaction existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_admin_finance_transaction, null, false);
        Spinner spinnerType = v.findViewById(R.id.spinnerType);
        EditText etAmount = v.findViewById(R.id.etAmount);
        Spinner spinnerCategory = v.findViewById(R.id.spinnerCategory);
        EditText etNote = v.findViewById(R.id.etNote); // This is now "Details"
        EditText etRemark = v.findViewById(R.id.etRemark);

        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"revenue", "expense"}));

        spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String type = spinnerType.getSelectedItem().toString().toLowerCase(Locale.ROOT);
                String[] cats = "revenue".equals(type) ? REVENUE_CATEGORIES : EXPENSE_CATEGORIES;
                
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(FinanceManagerActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, cats);
                spinnerCategory.setAdapter(catAdapter);
                
                if (existing != null && type.equalsIgnoreCase(existing.type)) {
                    setSpinner(spinnerCategory, existing.category);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        if (existing != null) {
            setSpinner(spinnerType, existing.type);
            etAmount.setText(String.valueOf(existing.amount));
            etNote.setText(safe(existing.note));
            etRemark.setText(safe(existing.remark));
        } else {
            spinnerType.setSelection(0);
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
                    map.put("note", etNote.getText().toString().trim());
                    map.put("remark", etRemark.getText().toString().trim());
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finance_transaction_modern, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            FinanceTransaction t = items.get(position);
            String type = safe(t.type).toLowerCase(Locale.ROOT);
            boolean isExpense = "expense".equals(type);
            h.tvTitle.setText(safe(t.category).isEmpty() ? (isExpense ? "Expense" : "Revenue") : safe(t.category));
            String amt = MoneyFormat.format(h.itemView.getContext(), t.amount);
            h.tvAmount.setText((isExpense ? "-" : "+") + amt);
            h.tvAmount.setTextColor(isExpense ? 0xFFB00020 : 0xFF2E7D32);

            String when = "";
            if (t.date != null) {
                long diff = System.currentTimeMillis() - t.date.toDate().getTime();
                if (diff < TimeUnit.DAYS.toMillis(7)) {
                    long days = Math.max(0, TimeUnit.MILLISECONDS.toDays(diff));
                    when = days == 0 ? "Today" : (days + "d ago");
                } else {
                    when = t.monthKey == null ? "" : t.monthKey;
                }
            }
            
            // Show merged note (Details)
            String sub = safe(t.note);
            if (!when.isEmpty() && !sub.isEmpty()) {
                h.tvSub.setText(when + " • " + sub);
            } else {
                h.tvSub.setText(when.isEmpty() ? sub : when);
            }
            
            // Show Remark in the note field
            h.tvNote.setText(safe(t.remark).isEmpty() ? "—" : t.remark);

            h.btnEdit.setOnClickListener(v -> showEditDialog(t));
            h.btnDelete.setOnClickListener(v ->
                    repo.finance().document(t.id).delete().addOnSuccessListener(x -> loadFinance()));
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvAmount, tvSub, tvNote;
            Button btnEdit, btnDelete;
            Holder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTxnTitle);
                tvAmount = itemView.findViewById(R.id.tvTxnAmount);
                tvSub = itemView.findViewById(R.id.tvTxnSub);
                tvNote = itemView.findViewById(R.id.tvTxnNote);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

}
