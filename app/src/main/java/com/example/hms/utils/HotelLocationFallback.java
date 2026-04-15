package com.example.hms.utils;

/**
 * When {@code settings/hotel} is missing in Firestore, show a stable demo pin per user email.
 */
public final class HotelLocationFallback {

    public static final class Pin {
        public final String displayName;
        public final double lat;
        public final double lng;

        public Pin(String displayName, double lat, double lng) {
            this.displayName = displayName;
            this.lat = lat;
            this.lng = lng;
        }
    }

    private static final Pin[] POOL = {
            new Pin("Marine Drive, Mumbai", 18.9442, 72.8236),
            new Pin("Gateway of India, Mumbai", 18.9217, 72.8347),
            new Pin("Bandra–Worli Sea Link, Mumbai", 19.0176, 72.8158),
            new Pin("Juhu Beach, Mumbai", 19.0884, 72.8267),
            new Pin("Phoenix Marketcity, Kurla", 19.0864, 72.8889),
    };

    private HotelLocationFallback() {}

    public static Pin forEmail(String email) {
        if (email == null || email.isEmpty()) {
            return POOL[0];
        }
        int i = Math.abs(email.hashCode()) % POOL.length;
        return POOL[i];
    }
}
