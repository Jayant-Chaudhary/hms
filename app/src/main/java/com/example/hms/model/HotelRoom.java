package com.example.hms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Room definition for reception selection. Replace mockInventory() with Firestore later.
 */
public final class HotelRoom {

    public final String id;
    public final int floor;
    public final String label;
    public final int maxAdults;
    public final int pricePerNight;
    public final boolean booked;

    public HotelRoom(String id, int floor, String label, int maxAdults, int pricePerNight, boolean booked) {
        this.id = id;
        this.floor = floor;
        this.label = label;
        this.maxAdults = maxAdults;
        this.pricePerNight = pricePerNight;
        this.booked = booked;
    }

    /** Demo inventory: some rooms booked (greyed). */
    public static List<HotelRoom> mockInventory() {
        List<HotelRoom> list = new ArrayList<>();
        list.add(new HotelRoom("f1_101", 1, "101", 2, 2200, false));
        list.add(new HotelRoom("f1_102", 1, "102", 3, 3000, true));
        list.add(new HotelRoom("f1_103", 1, "103", 1, 1500, false));
        list.add(new HotelRoom("f2_201", 2, "201", 2, 2400, false));
        list.add(new HotelRoom("f2_202", 2, "202", 2, 2400, false));
        list.add(new HotelRoom("f2_203", 2, "203", 4, 4200, false));
        list.add(new HotelRoom("f2_204", 2, "204", 3, 3200, true));
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
