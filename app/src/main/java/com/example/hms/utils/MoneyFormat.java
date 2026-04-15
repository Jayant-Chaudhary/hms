package com.example.hms.utils;

import android.content.Context;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Indian Rupee currency formatting (₹ / INR).
 */
public final class MoneyFormat {

    private MoneyFormat() {}

    public static String format(Context context, double amount) {
        // Force INR formatting across the app.
        Locale india = new Locale("en", "IN");
        NumberFormat nf = NumberFormat.getCurrencyInstance(india);
        nf.setCurrency(Currency.getInstance("INR"));
        return nf.format(amount);
    }
}
