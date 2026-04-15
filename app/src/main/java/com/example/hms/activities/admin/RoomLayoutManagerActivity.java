package com.example.hms.activities.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hms.R;
import com.example.hms.model.admin.AdminActivityItem;
import com.example.hms.model.admin.RoomConfig;
import com.example.hms.utils.MoneyFormat;
import com.example.hms.utils.SessionManager;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.admin.AdminActivityLog;
import com.example.hms.utils.admin.AdminFirestoreRepository;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomLayoutManagerActivity extends AppCompatActivity {

    private final AdminFirestoreRepository repo = new AdminFirestoreRepository();
    private final List<RoomConfig> items = new ArrayList<>();
    private RoomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_room_layout);

        RecyclerView rv = findViewById(R.id.rvRooms);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoomAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.btnAddRoom).setOnClickListener(v -> showEditDialog(null));
        loadRooms();
    }

    private void loadRooms() {
        // Avoid filtering out legacy docs that don't have layoutOrder.
        repo.rooms().orderBy("floor")
                .get()
                .addOnSuccessListener(snapshots -> {
                    items.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        RoomConfig r = doc.toObject(RoomConfig.class);
                        r.id = doc.getId();
                        items.add(r);
                    }
                    Collections.sort(items, Comparator
                            .comparingInt((RoomConfig r) -> r.floor)
                            .thenComparingInt(r -> r.layoutOrder > 0
                                    ? r.layoutOrder
                                    : parseInt((r.roomId == null ? "" : r.roomId).replaceAll("[^0-9]", ""), 0)));
                    adapter.notifyDataSetChanged();
                    if (items.isEmpty()) {
                        Toast.makeText(this, "No rooms found. Tap Add / Edit Room to create one.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Failed to load rooms: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void showEditDialog(RoomConfig existing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_admin_room, null, false);
        EditText etRoomId = v.findViewById(R.id.etRoomId);
        EditText etFloor = v.findViewById(R.id.etFloor);
        EditText etCapA = v.findViewById(R.id.etCapAdults);
        EditText etCapC = v.findViewById(R.id.etCapChildren);
        EditText etPrice = v.findViewById(R.id.etPricePerNight);
        EditText etHousekeeping = v.findViewById(R.id.etHousekeeping);
        com.google.android.material.switchmaterial.SwitchMaterial swMaint =
                v.findViewById(R.id.swUnderMaintenance);

        if (existing != null) {
            etRoomId.setText(existing.roomId);
            etFloor.setText(String.valueOf(existing.floor));
            etCapA.setText(String.valueOf(existing.capacityAdults));
            etCapC.setText(String.valueOf(existing.capacityChildren));
            etPrice.setText(String.valueOf(existing.pricePerNight));
            etHousekeeping.setText(existing.housekeepingStatus != null ? existing.housekeepingStatus : "ready");
            swMaint.setChecked(existing.underMaintenance);
        } else {
            etHousekeeping.setText("ready");
            swMaint.setChecked(false);
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add room" : "Edit room")
                .setView(v)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String roomId = etRoomId.getText().toString().trim();
                    int floor = parseInt(etFloor.getText().toString(), -1);
                    int capA = parseInt(etCapA.getText().toString(), -1);
                    int capC = parseInt(etCapC.getText().toString(), 0);
                    int price = parseInt(etPrice.getText().toString(), -1);
                    String hkRaw = etHousekeeping.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (hkRaw.isEmpty()) {
                        hkRaw = "ready";
                    }
                    if (!"ready".equals(hkRaw) && !"cleaning".equals(hkRaw)) {
                        Toast.makeText(this, "Housekeeping must be \"ready\" or \"cleaning\"", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (roomId.isEmpty() || floor < 0 || capA < 1 || price < 0) {
                        Toast.makeText(this, "Invalid room details", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> map = new HashMap<>();
                    map.put("roomId", roomId);
                    map.put("floor", floor);
                    map.put("capacityAdults", capA);
                    map.put("capacityChildren", capC);
                    map.put("pricePerNight", price);
                    map.put("active", true);
                    final String hk = hkRaw;
                    map.put("housekeepingStatus", hk);
                    map.put("underMaintenance", swMaint.isChecked());
                    map.put("layoutOrder", floor * 1000 + parseInt(roomId.replaceAll("[^0-9]", ""), 0));
                    SessionManager sm = new SessionManager(this);
                    String actor = sm.getName();
                    if (actor == null || actor.isEmpty()) {
                        actor = "Admin";
                    }
                    String finalActor = actor;
                    if (existing == null) {
                        String docId = roomId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
                        repo.rooms().document(docId)
                                .set(map)
                                .addOnSuccessListener(x -> {
                                    AdminActivityLog.append(finalActor,
                                            "Room " + roomId + " created (HK: " + hk + ")",
                                            AdminActivityItem.TYPE_LOG);
                                    loadRooms();
                                })
                                .addOnFailureListener(e -> Toast.makeText(
                                        this,
                                        "Could not save room: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show());
                    } else {
                        repo.rooms().document(existing.id)
                                .update(map)
                                .addOnSuccessListener(x -> {
                                    AdminActivityLog.append(finalActor,
                                            "Room " + roomId + " updated (HK: " + hk + ", maint: "
                                                    + swMaint.isChecked() + ")",
                                            AdminActivityItem.TYPE_LOG);
                                    loadRooms();
                                })
                                .addOnFailureListener(e -> Toast.makeText(
                                        this,
                                        "Could not update room: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show());
                    }
                }).show();
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return fallback; }
    }

    private class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_room, parent, false);
            return new Holder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            RoomConfig r = items.get(position);
            h.title.setText("Room " + r.roomId + " • Floor " + r.floor);
            String hk = r.housekeepingStatus != null ? r.housekeepingStatus : "ready";
            h.meta.setText(String.format(Locale.getDefault(),
                    "Adults: %d, Children: %d, Price: %s · HK: %s · Maint: %s",
                    r.capacityAdults,
                    r.capacityChildren,
                    MoneyFormat.format(h.itemView.getContext(), r.pricePerNight),
                    hk,
                    r.underMaintenance ? "yes" : "no"));
            h.btnEdit.setOnClickListener(v -> showEditDialog(r));
            h.btnDelete.setOnClickListener(v ->
                    repo.rooms().document(r.id).delete().addOnSuccessListener(x -> loadRooms()));
        }
        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView title, meta;
            Button btnEdit, btnDelete;
            Holder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvRoomTitle);
                meta = itemView.findViewById(R.id.tvRoomMeta);
                btnEdit = itemView.findViewById(R.id.btnEditRoom);
                btnDelete = itemView.findViewById(R.id.btnDeleteRoom);
            }
        }
    }
}
