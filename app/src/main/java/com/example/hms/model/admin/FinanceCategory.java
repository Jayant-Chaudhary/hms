package com.example.hms.model.admin;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore: {@code finance_categories} documents.
 * Fields: type ("revenue"|"expense"), name, subCategories (array)
 */
public class FinanceCategory {
    public String id;
    public String type;
    public String name;
    public List<String> subCategories = new ArrayList<>();

    public FinanceCategory() {}

    public FinanceCategory(String id, String type, String name, List<String> subCategories) {
        this.id = id;
        this.type = type;
        this.name = name;
        if (subCategories != null) this.subCategories = subCategories;
    }
}

