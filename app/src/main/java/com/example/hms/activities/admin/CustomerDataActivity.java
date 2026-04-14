package com.example.hms.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.model.admin.CustomerRecord;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerDataActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<CustomerRecord> all = new ArrayList<>();
    private final List<CustomerRecord> filtered = new ArrayList<>();
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_customer_data);

        RecyclerView rv = findViewById(R.id.rvCustomers);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomerAdapter();
        rv.setAdapter(adapter);

        EditText search = findViewById(R.id.etCustomerSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadCustomers();
    }

    private void loadCustomers() {
        repo.customers().get().addOnSuccessListener(snapshots -> {
            all.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                CustomerRecord c = doc.toObject(CustomerRecord.class);
                c.id = doc.getId();
                all.add(c);
            }
            applyFilter("");
        });
    }

    private void applyFilter(String q) {
        String query = q.toLowerCase(Locale.ROOT).trim();
        filtered.clear();
        for (CustomerRecord c : all) {
            String hay = (safe(c.name) + " " + safe(c.email) + " " + safe(c.mobile)).toLowerCase(Locale.ROOT);
            if (query.isEmpty() || hay.contains(query)) filtered.add(c);
        }
        adapter.notifyDataSetChanged();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_customer, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            CustomerRecord c = filtered.get(position);
            h.title.setText(safe(c.name) + " • " + safe(c.stayStatus));
            h.meta.setText("Email: " + safe(c.email) + "\nMobile: " + safe(c.mobile) +
                    "\nGov ID: " + safe(c.govIdType) + " - " + safe(c.govIdNumber));
        }

        @Override public int getItemCount() { return filtered.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, meta;
            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvCustomerTitle);
                meta = itemView.findViewById(R.id.tvCustomerMeta);
            }
        }
    }
}
