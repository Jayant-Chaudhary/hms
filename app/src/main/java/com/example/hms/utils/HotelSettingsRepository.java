package com.example.hms.utils;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Firestore: {@code settings/hotel} — display name and map coordinates for the customer dashboard.
 */
public final class HotelSettingsRepository {

    public static final String COLLECTION = "settings";
    public static final String DOC_HOTEL = "hotel";

    public static final String FIELD_DISPLAY_NAME = "displayName";
    public static final String FIELD_LATITUDE = "latitude";
    public static final String FIELD_LONGITUDE = "longitude";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public DocumentReference hotelDocument() {
        return db.collection(COLLECTION).document(DOC_HOTEL);
    }
}
