package com.example.hms.utils;

import com.example.hms.model.HotelRoom;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RoomInventoryLoader {

    public interface Callback {
        void onLoaded(List<HotelRoom> rooms);
        void onError(Exception e);
    }

    private RoomInventoryLoader() {}

    public static void load(Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rooms")
                .get()
                .addOnSuccessListener(roomSnapshots -> {
                    if (roomSnapshots == null || roomSnapshots.isEmpty()) {
                        callback.onLoaded(HotelRoom.mockInventory());
                        return;
                    }
                    List<RoomSeed> seeds = new ArrayList<>();
                    for (DocumentSnapshot doc : roomSnapshots.getDocuments()) {
                        // Backward compatibility: if "active" is missing, treat room as active.
                        Object activeRaw = doc.get("active");
                        boolean isActive = !(activeRaw instanceof Boolean) || (Boolean) activeRaw;
                        if (!isActive) {
                            continue;
                        }
                        String roomId = asString(doc.get("roomId"));
                        if (roomId == null || roomId.isEmpty()) {
                            continue;
                        }
                        RoomSeed seed = new RoomSeed();
                        seed.id = doc.getId();
                        seed.bookingRoomId = roomId;
                        seed.label = roomId;
                        seed.floor = asInt(doc.get("floor"), 0);
                        seed.maxAdults = Math.max(1, asInt(doc.get("capacityAdults"), 1));
                        int parsedPrice = asInt(doc.get("pricePerNight"), 0);
                        seed.pricePerNight = parsedPrice > 0 ? parsedPrice : defaultPrice(seed.maxAdults);
                        seeds.add(seed);
                    }
                    if (seeds.isEmpty()) {
                        callback.onLoaded(new ArrayList<>());
                        return;
                    }
                    db.collection("bookings")
                            .whereIn("status", Arrays.asList("booked", "in_house", "due_checkout"))
                            .get()
                            .addOnSuccessListener(bookingSnapshots -> {
                                Set<String> occupied = new HashSet<>();
                                if (bookingSnapshots != null) {
                                    for (DocumentSnapshot b : bookingSnapshots.getDocuments()) {
                                        Object roomsObj = b.get("rooms");
                                        if (roomsObj instanceof List) {
                                            for (Object r : (List<?>) roomsObj) {
                                                if (r != null) {
                                                    occupied.add(String.valueOf(r).trim().toLowerCase(Locale.ROOT));
                                                }
                                            }
                                        }
                                    }
                                }

                                List<HotelRoom> rooms = new ArrayList<>();
                                Map<String, Integer> labelCounts = new HashMap<>();
                                for (RoomSeed seed : seeds) {
                                    boolean booked = false;
                                    int duplicateCount = labelCounts.getOrDefault(seed.label, 0);
                                    labelCounts.put(seed.label, duplicateCount + 1);
                                    String safeLabel = duplicateCount == 0 ? seed.label : seed.label + " (" + (duplicateCount + 1) + ")";
                                    rooms.add(new HotelRoom(
                                            seed.id,
                                            seed.bookingRoomId,
                                            seed.floor,
                                            safeLabel,
                                            seed.maxAdults,
                                            seed.pricePerNight,
                                            booked
                                    ));
                                }
                                Collections.sort(rooms, Comparator
                                        .comparingInt((HotelRoom r) -> r.floor)
                                        .thenComparing(r -> r.label));
                                callback.onLoaded(rooms);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private static int defaultPrice(int maxAdults) {
        return Math.max(1800, maxAdults * 1200);
    }

    private static class RoomSeed {
        String id;
        String bookingRoomId;
        String label;
        int floor;
        int maxAdults;
        int pricePerNight;
    }
}
