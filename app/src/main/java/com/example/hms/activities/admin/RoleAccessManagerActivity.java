package com.example.hms.activities.admin;

import android.os.Bundle;
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
import com.example.hms.model.admin.AuthorizedUser;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoleAccessManagerActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<AuthorizedUser> items = new ArrayList<>();
    private RoleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_role_access);

        RecyclerView rv = findViewById(R.id.rvRoles);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoleAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.btnAddRole).setOnClickListener(v -> showDialog(null));
        loadRoles();
    }

    private void loadRoles() {
        repo.authorizedUsers().orderBy("email").get().addOnSuccessListener(snapshots -> {
            items.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                AuthorizedUser u = doc.toObject(AuthorizedUser.class);
                u.id = doc.getId();
                items.add(u);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showDialog(AuthorizedUser existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_admin_role, null, false);
        EditText etEmail = v.findViewById(R.id.etRoleEmail);
        Spinner spRole = v.findViewById(R.id.spinnerRole);
        spRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"admin", "reception", "customer"}));

        if (existing != null) {
            etEmail.setText(existing.email);
            for (int i = 0; i < spRole.getCount(); i++) {
                if (String.valueOf(spRole.getItemAtPosition(i)).equalsIgnoreCase(existing.role)) {
                    spRole.setSelection(i);
                    break;
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add authorized user" : "Edit role")
                .setView(v)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String email = etEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (!email.contains("@")) {
                        Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> map = new HashMap<>();
                    map.put("email", email);
                    map.put("role", String.valueOf(spRole.getSelectedItem()));
                    map.put("active", true);
                    map.put("updatedAt", Timestamp.now());

                    if (existing == null) {
                        repo.authorizedUsers().document(email).set(map).addOnSuccessListener(x -> {
                            // Keep compatibility with current login lookup that reads "roles".
                            repo.authorizedUsers().getFirestore().collection("roles").document(email).set(map);
                            loadRoles();
                        });
                    } else {
                        repo.authorizedUsers().document(existing.id).set(map).addOnSuccessListener(x -> {
                            repo.authorizedUsers().getFirestore().collection("roles").document(email).set(map);
                            loadRoles();
                        });
                    }
                }).show();
    }

    private class RoleAdapter extends RecyclerView.Adapter<RoleAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_role, parent, false);
            return new Holder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            AuthorizedUser u = items.get(position);
            h.email.setText(u.email);
            h.meta.setText("Role: " + u.role + " • Active: " + u.active);
            h.edit.setOnClickListener(v -> showDialog(u));
            h.delete.setOnClickListener(v ->
                    repo.authorizedUsers().document(u.id).delete().addOnSuccessListener(x -> loadRoles()));
        }
        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView email, meta;
            Button edit, delete;
            Holder(View itemView) {
                super(itemView);
                email = itemView.findViewById(R.id.tvRoleEmail);
                meta = itemView.findViewById(R.id.tvRoleMeta);
                edit = itemView.findViewById(R.id.btnEditRole);
                delete = itemView.findViewById(R.id.btnDeleteRole);
            }
        }
    }
}
