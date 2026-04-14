package com.example.hms.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.model.HotelRoom;
import com.example.hms.utils.BookingDataSync;
import com.example.hms.utils.CustomerBookingDraft;
import com.example.hms.utils.RoomInventoryLoader;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.UpiPaymentUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CustomerPaymentActivity extends AppCompatActivity {

    private CustomerBookingDraft draft;
    private double totalInr;
    private String txnRef;
    private List<HotelRoom> allRooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_payment);

        draft = CustomerBookingDraft.get();
        txnRef = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);

        TextView tvSummary = findViewById(R.id.tvBookingSummary);
        TextView tvTotal = findViewById(R.id.tvTotalAmount);
        allRooms = new ArrayList<>();
        RoomInventoryLoader.load(new RoomInventoryLoader.Callback() {
            @Override
            public void onLoaded(List<HotelRoom> rooms) {
                allRooms = rooms;
                recomputeSummary(tvSummary, tvTotal);
                if (rooms.isEmpty()) {
                    Toast.makeText(CustomerPaymentActivity.this,
                            "No active rooms found. Cannot continue payment.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CustomerPaymentActivity.this,
                        "Could not load room prices from server.",
                        Toast.LENGTH_LONG).show();
            }
        });

        ImageView imgQr = findViewById(R.id.imgUpiQr);
        TextView tvQrHint = findViewById(R.id.tvQrHint);
        Button btnDone = findViewById(R.id.btnPaymentDone);
        Button btnPayCash = findViewById(R.id.btnPayCash);
        Button btnPayUpi = findViewById(R.id.btnPayUpi);

        btnPayCash.setVisibility(View.GONE);
        imgQr.setVisibility(View.GONE);
        tvQrHint.setVisibility(View.GONE);
        btnPayUpi.setText("Pay online with UPI");
        btnPayUpi.setBackgroundResource(R.drawable.rounded_button);
        btnPayUpi.setTextColor(0xFFFFFFFF);

        btnPayUpi.setOnClickListener(v -> {
            String upiUri = UpiPaymentUri.build(
                    getString(R.string.upi_payee_vpa),
                    getString(R.string.upi_payee_name),
                    UpiPaymentUri.formatAmount(totalInr),
                    txnRef
            );
            Intent upiIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(upiUri));
            Intent chooser = Intent.createChooser(upiIntent, "Pay with");
            if (chooser.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
                btnDone.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Complete payment in your UPI app, then come back and tap Done.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No UPI app found on this device.", Toast.LENGTH_SHORT).show();
            }
        });

        btnDone.setOnClickListener(v -> {
            persistAndFinish();
        });
    }

    private void recomputeSummary(TextView tvSummary, TextView tvTotal) {
        int nights = draft.nights();
        long roomSubtotal = 0;
        StringBuilder roomsLine = new StringBuilder();
        for (String id : draft.selectedRoomIds) {
            HotelRoom r = HotelRoom.findById(id, allRooms);
            if (r != null) {
                roomSubtotal += (long) r.pricePerNight * nights;
                roomsLine.append("• Room ").append(r.label)
                        .append(" (₹").append(r.pricePerNight).append("/night × ").append(nights).append(")\n");
            } else {
                roomsLine.append("• Room ").append(id).append("\n");
            }
        }
        totalInr = roomSubtotal;
        tvSummary.setText("Customer: " + draft.customerName + "\n"
                + "Adults / children: " + draft.adults + " / " + draft.children + "\n"
                + "Stay: " + nights + " night(s)\n"
                + "Rooms:\n" + roomsLine
                + "Ref: " + txnRef);
        tvTotal.setText(String.format(Locale.getDefault(), "Total: ₹%,.0f", totalInr));
    }

    private void persistAndFinish() {
        if (totalInr <= 0) {
            Toast.makeText(this, "Unable to calculate total from live room layout.", Toast.LENGTH_LONG).show();
            return;
        }
        BookingDataSync.saveBookingAndCustomer(
                txnRef,
                draft.customerName,
                draft.email,
                draft.mobile,
                draft.govIdType,
                draft.govIdNumber,
                draft.adults,
                draft.children,
                draft.checkInMillis,
                draft.checkOutMillis,
                draft.selectedRoomIds,
                allRooms,
                totalInr,
                "upi",
                "customer"
        ).addOnSuccessListener(unused -> {
            CustomerBookingDraft.reset();
            Intent i = new Intent(this, CustomerDashboardActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Could not save booking: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
