package com.example.hms.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.model.admin.FinanceTransaction;
import com.example.hms.utils.MoneyFormat;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FinanceLedgerActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<FinanceTransaction> all = new ArrayList<>();
    private final List<FinanceTransaction> filtered = new ArrayList<>();
    private LedgerAdapter adapter;

    private EditText etSearch;
    private ChipGroup chipGroupType;
    private Spinner spinnerSort;

    private final SimpleDateFormat df = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finance_ledger);

        etSearch = findViewById(R.id.etSearchLedger);
        chipGroupType = findViewById(R.id.chipGroupLedgerType);
        spinnerSort = findViewById(R.id.spinnerLedgerSort);

        spinnerSort.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Date (newest)", "Date (oldest)", "Amount (high)", "Amount (low)"}));

        RecyclerView rv = findViewById(R.id.rvLedger);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LedgerAdapter(filtered);
        rv.setAdapter(adapter);

        findViewById(R.id.btnBackLedger).setOnClickListener(v -> finish());

        chipGroupType.setOnCheckedStateChangeListener((g, ids) -> apply());
        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { apply(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { apply(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        load();
    }

    private void load() {
        repo.finance()
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(500)
                .get()
                .addOnSuccessListener(snap -> {
                    all.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        FinanceTransaction t = d.toObject(FinanceTransaction.class);
                        t.id = d.getId();
                        all.add(t);
                    }
                    apply();
                });
    }

    private void apply() {
        String search = safe(etSearch.getText() == null ? "" : etSearch.getText().toString())
                .trim().toLowerCase(Locale.ROOT);
        String typeFilter = currentTypeFilter();

        filtered.clear();
        for (FinanceTransaction t : all) {
            boolean typeOk = "all".equals(typeFilter) || typeFilter.equalsIgnoreCase(safe(t.type));
            boolean searchOk = search.isEmpty()
                    || safe(t.category).toLowerCase(Locale.ROOT).contains(search)
                    || safe(t.subCategory).toLowerCase(Locale.ROOT).contains(search)
                    || safe(t.microCategory).toLowerCase(Locale.ROOT).contains(search)
                    || safe(t.note).toLowerCase(Locale.ROOT).contains(search)
                    || safe(t.sourceBookingId).toLowerCase(Locale.ROOT).contains(search);
            if (typeOk && searchOk) filtered.add(t);
        }

        sortFiltered();
        adapter.notifyDataSetChanged();
    }

    private void sortFiltered() {
        int pos = spinnerSort.getSelectedItemPosition();
        Comparator<FinanceTransaction> byDateAsc = (a, b) -> {
            long ta = a != null && a.date != null ? a.date.toDate().getTime() : 0;
            long tb = b != null && b.date != null ? b.date.toDate().getTime() : 0;
            return Long.compare(ta, tb);
        };
        Comparator<FinanceTransaction> byAmtAsc = (a, b) -> Double.compare(a == null ? 0 : a.amount, b == null ? 0 : b.amount);

        if (pos == 0) { // newest
            Collections.sort(filtered, byDateAsc.reversed());
        } else if (pos == 1) { // oldest
            Collections.sort(filtered, byDateAsc);
        } else if (pos == 2) { // amount high
            Collections.sort(filtered, byAmtAsc.reversed());
        } else { // amount low
            Collections.sort(filtered, byAmtAsc);
        }
    }

    private String currentTypeFilter() {
        int id = chipGroupType.getCheckedChipId();
        if (id == R.id.chipLedgerRevenue) return "revenue";
        if (id == R.id.chipLedgerExpense) return "expense";
        return "all";
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.Holder> {
        private final List<FinanceTransaction> items;
        LedgerAdapter(List<FinanceTransaction> items) { this.items = items; }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ledger_row, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            FinanceTransaction t = items.get(position);
            Date d = t != null && t.date != null ? t.date.toDate() : null;
            h.tvDate.setText(d == null ? "—" : df.format(d));
            h.tvType.setText(safe(t.type));
            h.tvCategory.setText(safe(t.category) + (safe(t.subCategory).isEmpty() ? "" : (" • " + safe(t.subCategory))));
            h.tvRemark.setText(safe(t.note).isEmpty() ? safe(t.microCategory) : safe(t.note));

            boolean isExpense = "expense".equalsIgnoreCase(safe(t.type));
            String amt = MoneyFormat.format(h.itemView.getContext(), t.amount);
            h.tvAmount.setText((isExpense ? "-" : "+") + amt);
            h.tvAmount.setTextColor(isExpense ? 0xFFB00020 : 0xFF2E7D32);
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final TextView tvDate, tvAmount, tvType, tvCategory, tvRemark;
            Holder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvColDate);
                tvAmount = itemView.findViewById(R.id.tvColAmount);
                tvType = itemView.findViewById(R.id.tvColType);
                tvCategory = itemView.findViewById(R.id.tvColCategory);
                tvRemark = itemView.findViewById(R.id.tvColRemark);
            }
        }
    }
}

