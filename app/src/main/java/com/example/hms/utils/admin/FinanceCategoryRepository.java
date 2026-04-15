package com.example.hms.utils.admin;

import com.example.hms.model.admin.FinanceCategory;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FinanceCategoryRepository {

    public static final String COL = "finance_categories";

    private final CollectionReference ref = FirebaseFirestore.getInstance().collection(COL);

    public Task<Void> ensureDefaultsIfMissing() {
        return ref.limit(1).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null && !t.getResult().isEmpty()) {
                return Tasks.forResult(null);
            }
            List<Task<?>> writes = new ArrayList<>();
            writes.add(upsert(new FinanceCategory(null, "revenue", "Miscellaneous Revenue",
                    Arrays.asList("Cash", "UPI", "Other"))));
            writes.add(upsert(new FinanceCategory(null, "revenue", "Room Booking",
                    Arrays.asList("Cash", "UPI", "Card", "Online", "Other"))));
            writes.add(upsert(new FinanceCategory(null, "revenue", "Food",
                    Arrays.asList("Restaurant", "Room Service", "Other"))));
            writes.add(upsert(new FinanceCategory(null, "revenue", "Laundry",
                    Arrays.asList("Per Item", "Package", "Other"))));
            writes.add(upsert(new FinanceCategory(null, "expense", "Utilities",
                    Arrays.asList("Electricity", "Water", "Internet", "Other"))));
            writes.add(upsert(new FinanceCategory(null, "expense", "Maintenance",
                    Arrays.asList("Repairs", "Supplies", "Vendor", "Other"))));
            writes.add(upsert(new FinanceCategory(null, "expense", "Payroll",
                    Arrays.asList("Salaries", "Incentives", "Other"))));
            return Tasks.whenAll(writes);
        });
    }

    public Task<List<FinanceCategory>> getAll() {
        return ref.orderBy("type", Query.Direction.ASCENDING)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .continueWith(t -> {
                    List<FinanceCategory> out = new ArrayList<>();
                    if (!t.isSuccessful() || t.getResult() == null) return out;
                    for (DocumentSnapshot d : t.getResult().getDocuments()) {
                        FinanceCategory c = d.toObject(FinanceCategory.class);
                        if (c == null) c = new FinanceCategory();
                        c.id = d.getId();
                        out.add(c);
                    }
                    return out;
                });
    }

    public Task<Void> upsert(FinanceCategory c) {
        String type = c.type == null ? "" : c.type.trim().toLowerCase(Locale.ROOT);
        String name = c.name == null ? "" : c.name.trim();
        List<String> subs = c.subCategories == null ? new ArrayList<>() : c.subCategories;
        if (subs.isEmpty()) subs = Arrays.asList("Other");

        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("name", name);
        m.put("subCategories", subs);
        m.put("updatedAt", FieldValue.serverTimestamp());

        if (c.id == null || c.id.trim().isEmpty()) {
            String id = (type + "_" + name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
            return ref.document(id).set(m, SetOptions.merge());
        }
        return ref.document(c.id).set(m, SetOptions.merge());
    }

    /** Adds a frequent reason suggestion under a category (arrayUnion). */
    public Task<Void> addReasonSuggestion(String type, String categoryName, String reason) {
        String t = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        String n = categoryName == null ? "" : categoryName.trim();
        String r = reason == null ? "" : reason.trim();
        if (t.isEmpty() || n.isEmpty() || r.isEmpty()) {
            return Tasks.forResult(null);
        }
        String id = (t + "_" + n).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        Map<String, Object> m = new HashMap<>();
        m.put("subCategories", FieldValue.arrayUnion(r));
        m.put("updatedAt", FieldValue.serverTimestamp());
        return ref.document(id).set(m, SetOptions.merge());
    }

    public static List<String> parseCommaSeparated(String input) {
        List<String> out = new ArrayList<>();
        if (input == null) return out;
        String[] parts = input.split(",");
        for (String p : parts) {
            String s = p == null ? "" : p.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
}

