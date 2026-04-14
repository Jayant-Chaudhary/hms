package com.example.hms.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hms.R;
import com.example.hms.model.HotelRoom;
import com.example.hms.utils.CustomerBookingDraft;
import com.example.hms.utils.RoomInventoryLoader;
import com.example.hms.utils.ThemeManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CustomerRoomSelectionActivity extends AppCompatActivity {

    private final List<HotelRoom> inventory = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private TextView tvCapacityHint;
    private CustomerBookingDraft draft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_room_selection);

        draft = CustomerBookingDraft.get();
        ((TextView) findViewById(R.id.tvAdultRequirement))
                .setText("Adults to accommodate: " + draft.adults);
        tvCapacityHint = findViewById(R.id.tvCapacityHint);

        loadInventoryAndRender();

        findViewById(R.id.btnConfirmRooms).setOnClickListener(v -> confirmSelection());
    }

    private void loadInventoryAndRender() {
        LinearLayout llFloors = findViewById(R.id.llRoomFloors);
        RoomInventoryLoader.load(new RoomInventoryLoader.Callback() {
            @Override
            public void onLoaded(List<HotelRoom> rooms) {
                inventory.clear();
                inventory.addAll(rooms);
                selectedIds.clear();
                llFloors.removeAllViews();
                buildFloorUi(llFloors);
                updateCapacityHint();
                if (rooms.isEmpty()) {
                    Toast.makeText(CustomerRoomSelectionActivity.this,
                            "No active rooms found. Ask admin to configure room layout.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CustomerRoomSelectionActivity.this,
                        "Could not load room layout from server.",
                        Toast.LENGTH_LONG).show();
                inventory.clear();
                selectedIds.clear();
                llFloors.removeAllViews();
                updateCapacityHint();
            }
        });
    }

    private void buildFloorUi(LinearLayout root) {
        Map<Integer, List<HotelRoom>> byFloor = new TreeMap<>();
        for (HotelRoom r : inventory) {
            byFloor.computeIfAbsent(r.floor, k -> new ArrayList<>()).add(r);
        }

        float density = getResources().getDisplayMetrics().density;
        int margin = (int) (8 * density);
        int cellMinH = (int) (56 * density);

        for (Map.Entry<Integer, List<HotelRoom>> entry : byFloor.entrySet()) {
            TextView floorTitle = new TextView(this);
            floorTitle.setText("Floor " + entry.getKey());
            floorTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            floorTitle.setTypeface(null, Typeface.BOLD);
            floorTitle.setTextColor(0xFF1A1A2E);
            root.addView(floorTitle);

            List<HotelRoom> rooms = entry.getValue();
            LinearLayout row = null;
            for (int i = 0; i < rooms.size(); i++) {
                if (i % 2 == 0) {
                    row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    root.addView(row);
                }
                HotelRoom room = rooms.get(i);
                TextView cell = new TextView(this);
                cell.setTag(room.id);
                cell.setGravity(Gravity.CENTER);
                cell.setMinHeight(cellMinH);
                int pad = (int) (10 * density);
                cell.setPadding(pad, pad, pad, pad);
                cell.setText(room.label + "\nMax " + room.maxAdults + " adults" + (room.booked ? "\nBooked" : ""));
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                applyRoomStyle(cell, room);
                cell.setOnClickListener(v -> onRoomClicked(room));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(margin / 2, margin / 2, margin / 2, margin / 2);
                cell.setLayoutParams(lp);
                if (row != null) {
                    row.addView(cell);
                }
            }
        }
    }

    private void applyRoomStyle(TextView cell, HotelRoom room) {
        if (room.booked) {
            cell.setBackgroundResource(R.drawable.bg_room_booked);
            cell.setTextColor(0xFF888888);
            cell.setClickable(false);
            return;
        }
        boolean selected = selectedIds.contains(room.id);
        cell.setBackgroundResource(selected ? R.drawable.bg_room_selected : R.drawable.bg_room_available);
        cell.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void onRoomClicked(HotelRoom room) {
        if (room.booked) {
            return;
        }
        if (selectedIds.contains(room.id)) {
            selectedIds.remove(room.id);
        } else {
            selectedIds.add(room.id);
        }
        refreshCells((LinearLayout) findViewById(R.id.llRoomFloors));
        updateCapacityHint();
    }

    private void refreshCells(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            android.view.View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    android.view.View cellView = row.getChildAt(j);
                    if (cellView instanceof TextView && cellView.getTag() instanceof String) {
                        HotelRoom room = HotelRoom.findById((String) cellView.getTag(), inventory);
                        if (room != null) {
                            applyRoomStyle((TextView) cellView, room);
                        }
                    }
                }
            }
        }
    }

    private void updateCapacityHint() {
        int capacity = 0;
        for (String id : selectedIds) {
            HotelRoom room = HotelRoom.findById(id, inventory);
            if (room != null) {
                capacity += room.maxAdults;
            }
        }
        tvCapacityHint.setText("Selected room capacity: " + capacity + " (need at least " + draft.adults + " adults)");
    }

    private void confirmSelection() {
        int capacity = 0;
        for (String id : selectedIds) {
            HotelRoom room = HotelRoom.findById(id, inventory);
            if (room != null) {
                capacity += room.maxAdults;
            }
        }
        if (selectedIds.isEmpty() || capacity < draft.adults) {
            Toast.makeText(this, "Please select enough room capacity for adults.", Toast.LENGTH_SHORT).show();
            return;
        }
        draft.selectedRoomIds.clear();
        draft.selectedRoomIds.addAll(selectedIds);
        startActivity(new Intent(this, CustomerPaymentActivity.class));
    }
}
