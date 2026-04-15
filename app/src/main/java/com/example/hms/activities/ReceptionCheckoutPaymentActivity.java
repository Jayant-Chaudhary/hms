package com.example.hms.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.utils.BookingDataSync;
import com.example.hms.utils.QrBitmapEncoder;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.UpiPaymentUri;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ReceptionCheckoutPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "bookingId";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_GUEST_LABEL = "guestLabel";

    private String bookingId;
    private double amountInr;
    private String paymentMethod = "cash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_checkout_payment);

        Intent in = getIntent();
        bookingId = in.getStringExtra(EXTRA_BOOKING_ID);
        amountInr = in.getDoubleExtra(EXTRA_AMOUNT, 0);
        String guest = in.getStringExtra(EXTRA_GUEST_LABEL);
        if (bookingId == null || bookingId.isEmpty() || amountInr <= 0) {
            Toast.makeText(this, "Invalid payment request", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView tvSummary = findViewById(R.id.tvCheckoutSummary);
        TextView tvBalance = findViewById(R.id.tvBalanceAmount);
        tvSummary.setText("Booking: " + bookingId + "\nGuest: " + (guest != null ? guest : "—"));
        tvBalance.setText(String.format(Locale.getDefault(), "Balance due: ₹%,.0f", amountInr));

        ImageView imgQr = findViewById(R.id.imgUpiQr);
        TextView tvQrHint = findViewById(R.id.tvQrHint);
        Button btnDone = findViewById(R.id.btnPaymentDone);

        findViewById(R.id.btnPayCash).setOnClickListener(v -> {
            paymentMethod = "cash";
            Toast.makeText(this, "Recorded as cash at desk.", Toast.LENGTH_SHORT).show();
            btnDone.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btnPayUpi).setOnClickListener(v -> {
            paymentMethod = "upi";
            FirebaseFirestore.getInstance().collection("system_config").document("payment_settings").get()
                .addOnSuccessListener(doc -> {
                    String vpa = doc.getString("upiId");
                    String name = doc.getString("payeeName");
                    
                    if (vpa == null || vpa.isEmpty()) {
                        Toast.makeText(this, "Admin has not configured UPI ID. Using default.", Toast.LENGTH_SHORT).show();
                        vpa = getString(R.string.upi_payee_vpa);
                        name = getString(R.string.upi_payee_name);
                    }

                    String amount = UpiPaymentUri.formatAmount(amountInr);
                    String uri = UpiPaymentUri.build(vpa, name, amount, bookingId + "-bal");
                    int size = (int) (getResources().getDisplayMetrics().density * 240);
                    Bitmap bmp = QrBitmapEncoder.encode(uri, size);
                    if (bmp != null) {
                        imgQr.setImageBitmap(bmp);
                        imgQr.setVisibility(View.VISIBLE);
                        tvQrHint.setVisibility(View.VISIBLE);
                        btnDone.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Could not create QR", Toast.LENGTH_SHORT).show();
                    }
                });
        });

        btnDone.setOnClickListener(v -> persist());
    }

    private void persist() {
        BookingDataSync.recordBalancePayment(bookingId, amountInr, paymentMethod, "reception")
                .addOnSuccessListener(unused -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Payment failed", Toast.LENGTH_LONG).show());
    }
}
