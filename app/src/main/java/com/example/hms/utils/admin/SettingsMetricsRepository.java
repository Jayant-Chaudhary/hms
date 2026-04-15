package com.example.hms.utils.admin;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code settings/metrics} — guest satisfaction and other dashboard KPIs.
 */
public final class SettingsMetricsRepository {

    public static final String FIELD_GUEST_SATISFACTION = "guestSatisfaction";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    private final DocumentReference metricsRef =
            FirebaseFirestore.getInstance().collection("settings").document("metrics");

    public DocumentReference metricsSettings() {
        return metricsRef;
    }

    /** Ensures guestSatisfaction exists (merge; does not overwrite if already set). */
    public Task<Void> ensureDefaultGuestSatisfactionIfMissing() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put(FIELD_GUEST_SATISFACTION, 4.8);
        defaults.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
        return metricsRef.set(defaults, SetOptions.merge());
    }

    public Task<Void> mergeGuestSatisfaction(double value) {
        Map<String, Object> m = new HashMap<>();
        m.put(FIELD_GUEST_SATISFACTION, value);
        m.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
        return metricsRef.set(m, SetOptions.merge());
    }
}
