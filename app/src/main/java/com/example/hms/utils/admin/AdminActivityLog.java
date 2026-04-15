package com.example.hms.utils.admin;

import com.example.hms.model.admin.AdminActivityItem;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Append-only helper for {@code admin_activity}. Call from reception, housekeeping, or admin flows.
 */
public final class AdminActivityLog {

    private AdminActivityLog() {}

    /**
     * @param activityType {@link AdminActivityItem#TYPE_LOG} etc.
     */
    public static void append(String actorName, String summary, String activityType) {
        Map<String, Object> m = new HashMap<>();
        m.put("actorName", actorName == null ? "" : actorName);
        m.put("summary", summary == null ? "" : summary);
        String t = activityType == null ? AdminActivityItem.TYPE_LOG : activityType;
        m.put("activityType", t);
        m.put("createdAt", FieldValue.serverTimestamp());
        if (AdminActivityItem.TYPE_ACCESS_REQUEST.equalsIgnoreCase(t)) {
            m.put("status", AdminActivityItem.STATUS_PENDING);
        }
        FirebaseFirestore.getInstance()
                .collection(AdminFirestoreRepository.COL_ADMIN_ACTIVITY)
                .add(m);
    }
}
