package com.example.hms.model.admin;

public class RoomConfig {
    public String id;
    public String roomId;
    public int floor;
    public int capacityAdults;
    public int capacityChildren;
    public int pricePerNight;
    public boolean active = true;
    public int layoutOrder;
    /** Housekeeping: {@code ready} or {@code cleaning} (Firestore string). */
    public String housekeepingStatus = "ready";
    public boolean underMaintenance = false;

    public RoomConfig() {
    }
}
