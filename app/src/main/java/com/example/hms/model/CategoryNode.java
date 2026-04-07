package com.example.hms.model;

public class CategoryNode {
    private String id;
    private String name;
    private String parentId; // If null, it's a main category. If it has an ID, it's a subcategory.
    private String icon;

    // Required empty constructor for Firebase Firestore serialization
    public CategoryNode() {}

    public CategoryNode(String id, String name, String parentId, String icon) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.icon = icon;
    }

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
