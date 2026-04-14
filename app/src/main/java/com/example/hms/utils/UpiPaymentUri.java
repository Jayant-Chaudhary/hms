package com.example.hms.utils;

import android.net.Uri;

import java.util.Locale;

/**
 * Builds a UPI deep link for QR display. Replace payee with your real merchant UPI id in production.
 */
public final class UpiPaymentUri {

    private UpiPaymentUri() {}

    public static String build(String payeeVpa, String payeeName, String amountInr, String transactionNote) {
        return new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", payeeVpa)
                .appendQueryParameter("pn", payeeName)
                .appendQueryParameter("am", amountInr)
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tn", transactionNote != null ? transactionNote : "Hotel booking")
                .build()
                .toString();
    }

    public static String formatAmount(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
