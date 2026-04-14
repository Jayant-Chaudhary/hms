package com.example.hms.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hms.R;
import com.example.hms.model.HotelRoom;
import com.example.hms.utils.ReceptionBookingDraft;
import com.example.hms.utils.RoomInventoryLoader;
import com.example.hms.utils.ThemeManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ReceptionRoomSelectionActivity extends AppCompatActivity {

    private final List<HotelRoom> inventory = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();

    private TextView tvCapacityHint;
    private ReceptionBookingDraft draft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_room_selection);

        draft = ReceptionBookingDraft.get();

        TextView tvAdults = findViewById(R.id.tvAdultRequirement);
        tvCapacityHint = findViewById(R.id.tvCapacityHint);
        tvAdults.setText("Adults to accommodate: " + draft.adults);

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
                    Toast.makeText(ReceptionRoomSelectionActivity.this,
                            "No active rooms found. Ask admin to configure room layout.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ReceptionRoomSelectionActivity.this,
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

        for (Map.Entry<Integer, List<HotelRoom>> e : byFloor.entrySet()) {
            TextView floorTitle = new TextView(this);
            floorTitle.setText("Floor " + e.getKey());
            floorTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            floorTitle.setTypeface(null, Typeface.BOLD);
            floorTitle.setTextColor(0xFF1A1A2E);
            LinearLayout.LayoutParams ftp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            ftp.topMargin = root.getChildCount() > 0 ? (int) (16 * density) : 0;
            floorTitle.setLayoutParams(ftp);
            root.addView(floorTitle);

            List<HotelRoom> rooms = e.getValue();
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
                applyRoomStyle(cell, room);
                cell.setText(buildRoomLabel(room));
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                cell.setOnClickListener(v -> onRoomClicked(room));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(margin / 2, margin / 2, margin / 2, margin / 2);
                cell.setLayoutParams(lp);
                row.addView(cell);
            }
        }
    }

    private static String buildRoomLabel(HotelRoom r) {
        return r.label + "\nMax " + r.maxAdults + " adults" + (r.booked ? "\nBooked" : "");
    }

    private void applyRoomStyle(TextView cell, HotelRoom room) {
        if (room.booked) {
            cell.setBackgroundResource(R.drawable.bg_room_booked);
            cell.setTextColor(0xFF888888);
            cell.setClickable(false);
            return;
        }
        boolean sel = selectedIds.contains(room.id);
        cell.setBackgroundResource(sel ? R.drawable.bg_room_selected : R.drawable.bg_room_available);
        cell.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        cell.setClickable(true);
    }

    private void onRoomClicked(HotelRoom room) {
        if (room.booked) {
            return;
        }
        TextView cell = findCell(room.id);
        if (selectedIds.contains(room.id)) {
            selectedIds.remove(room.id);
        } else {
            selectedIds.add(room.id);
        }
        if (cell != null) {
            applyRoomStyle(cell, room);
        }
        updateCapacityHint();
    }

    private TextView findCell(String roomId) {
        LinearLayout ll = findViewById(R.id.llRoomFloors);
        return findCellIn(ll, roomId);
    }

    private TextView findCellIn(LinearLayout parent, String roomId) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            android.view.View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                if (roomId.equals(child.getTag())) {
                    return (TextView) child;
                }
            } else if (child instanceof LinearLayout) {
                TextView t = findCellIn((LinearLayout) child, roomId);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private void updateCapacityHint() {
        int cap = 0;
        for (String id : selectedIds) {
            HotelRoom r = HotelRoom.findById(id, inventory);
            if (r != null) {
                cap += r.maxAdults;
            }
        }
        tvCapacityHint.setText("Selected room capacity: " + cap + " (need at least " + draft.adults + " adults)");
    }

    private void confirmSelection() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "Select at least one room", Toast.LENGTH_SHORT).show();
            return;
        }
        int cap = 0;
        for (String id : selectedIds) {
            HotelRoom r = HotelRoom.findById(id, inventory);
            if (r != null) {
                cap += r.maxAdults;
            }
        }
        if (cap < draft.adults) {
            Toast.makeText(this,
                    "Selected rooms only fit " + cap + " adults. Add more rooms or pick larger rooms.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        draft.selectedRoomIds.clear();
        draft.selectedRoomIds.addAll(selectedIds);
        startActivity(new Intent(this, ReceptionPaymentActivity.class));
    }
}
