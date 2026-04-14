package com.example.hms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Room definition for reception selection. Replace mockInventory() with Firestore later.
 */
public final class HotelRoom {

    public final String id;
    public final String bookingRoomId;
    public final int floor;
    public final String label;
    public final int maxAdults;
    public final int pricePerNight;
    public final boolean booked;

    public HotelRoom(String id, int floor, String label, int maxAdults, int pricePerNight, boolean booked) {
        this(id, id, floor, label, maxAdults, pricePerNight, booked);
    }

    public HotelRoom(String id, String bookingRoomId, int floor, String label, int maxAdults, int pricePerNight, boolean booked) {
        this.id = id;
        this.bookingRoomId = bookingRoomId;
        this.floor = floor;
        this.label = label;
        this.maxAdults = maxAdults;
        this.pricePerNight = pricePerNight;
        this.booked = booked;
    }

    /** Default inventory: 3 floors x 6 rooms, all unbooked. */
    public static List<HotelRoom> mockInventory() {
        List<HotelRoom> list = new ArrayList<>();
        // Ground floor (0): G01-G06
        list.add(new HotelRoom("g_001", 0, "G01", 2, 2200, false));
        list.add(new HotelRoom("g_002", 0, "G02", 2, 2200, false));
        list.add(new HotelRoom("g_003", 0, "G03", 3, 2600, false));
        list.add(new HotelRoom("g_004", 0, "G04", 2, 2400, false));
        list.add(new HotelRoom("g_005", 0, "G05", 3, 2800, false));
        list.add(new HotelRoom("g_006", 0, "G06", 4, 3400, false));

        // First floor (1): 101-106
        list.add(new HotelRoom("f1_101", 1, "101", 2, 2400, false));
        list.add(new HotelRoom("f1_102", 1, "102", 2, 2400, false));
        list.add(new HotelRoom("f1_103", 1, "103", 3, 2800, false));
        list.add(new HotelRoom("f1_104", 1, "104", 2, 2600, false));
        list.add(new HotelRoom("f1_105", 1, "105", 3, 3000, false));
        list.add(new HotelRoom("f1_106", 1, "106", 4, 3600, false));

        // Second floor (2): 201-206
        list.add(new HotelRoom("f2_201", 2, "201", 2, 2600, false));
        list.add(new HotelRoom("f2_202", 2, "202", 2, 2600, false));
        list.add(new HotelRoom("f2_203", 2, "203", 3, 3000, false));
        list.add(new HotelRoom("f2_204", 2, "204", 2, 2800, false));
        list.add(new HotelRoom("f2_205", 2, "205", 3, 3200, false));
        list.add(new HotelRoom("f2_206", 2, "206", 4, 3800, false));
        return Collections.unmodifiableList(list);
    }

    public static HotelRoom findById(String id, List<HotelRoom> rooms) {
        for (HotelRoom r : rooms) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }
}
