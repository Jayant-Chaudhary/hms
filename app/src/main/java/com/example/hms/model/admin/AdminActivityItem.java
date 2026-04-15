package com.example.hms.model.admin;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Firestore {@code admin_activity} documents.
 */
public class AdminActivityItem {

    public static final String TYPE_LOG = "log";
    public static final String TYPE_ACCESS_REQUEST = "access_request";
    public static final String TYPE_SYSTEM = "system";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_DENIED = "denied";

    public String id;
    public String actorName;
    public String summary;
    public Timestamp createdAt;
    /** {@link #TYPE_LOG}, {@link #TYPE_ACCESS_REQUEST}, {@link #TYPE_SYSTEM} */
    public String activityType = TYPE_LOG;
    /** For {@link #TYPE_ACCESS_REQUEST}: {@link #STATUS_PENDING}, etc. */
    public String status;

    public static AdminActivityItem fromSnapshot(DocumentSnapshot d) {
        AdminActivityItem i = new AdminActivityItem();
        i.id = d.getId();
        i.actorName = d.getString("actorName");
        i.summary = d.getString("summary");
        i.createdAt = d.getTimestamp("createdAt");
        i.activityType = d.getString("activityType");
        if (i.activityType == null || i.activityType.isEmpty()) {
            i.activityType = TYPE_LOG;
        }
        i.status = d.getString("status");
        return i;
    }

    public boolean isAccessRequestPending() {
        return TYPE_ACCESS_REQUEST.equalsIgnoreCase(activityType)
                && STATUS_PENDING.equalsIgnoreCase(status);
    }
}
