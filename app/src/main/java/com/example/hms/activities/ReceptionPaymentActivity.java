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
import com.example.hms.model.HotelRoom;
import com.example.hms.utils.BookingDataSync;
import com.example.hms.utils.QrBitmapEncoder;
import com.example.hms.utils.ReceptionBookingDraft;
import com.example.hms.utils.RoomInventoryLoader;
import com.example.hms.utils.ThemeManager;
import com.example.hms.utils.UpiPaymentUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ReceptionPaymentActivity extends AppCompatActivity {

    private ReceptionBookingDraft draft;
    private double totalInr;
    private String txnRef;
    private String paymentMethod = "cash";
    private List<HotelRoom> allRooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_payment);

        draft = ReceptionBookingDraft.get();
        txnRef = "HMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);

        TextView tvSummary = findViewById(R.id.tvBookingSummary);
        TextView tvTotal = findViewById(R.id.tvTotalAmount);
        allRooms = new ArrayList<>();
        RoomInventoryLoader.load(new RoomInventoryLoader.Callback() {
            @Override
            public void onLoaded(List<HotelRoom> rooms) {
                allRooms = rooms;
                recomputeSummary(tvSummary, tvTotal);
                if (rooms.isEmpty()) {
                    Toast.makeText(ReceptionPaymentActivity.this,
                            "No active rooms found. Cannot continue payment.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ReceptionPaymentActivity.this,
                        "Could not load room prices from server. Using offline room prices.",
                        Toast.LENGTH_LONG).show();
                allRooms = HotelRoom.mockInventory();
                recomputeSummary(tvSummary, tvTotal);
            }
        });

        ImageView imgQr = findViewById(R.id.imgUpiQr);
        TextView tvQrHint = findViewById(R.id.tvQrHint);
        Button btnDone = findViewById(R.id.btnPaymentDone);

        findViewById(R.id.btnPayCash).setOnClickListener(v -> {
            paymentMethod = "cash";
            Toast.makeText(this, "Recorded as cash at desk.", Toast.LENGTH_SHORT).show();
            showDone(btnDone);
        });

        findViewById(R.id.btnPayUpi).setOnClickListener(v -> {
            paymentMethod = "upi";
            String amount = UpiPaymentUri.formatAmount(totalInr);
            String vpa = getString(R.string.upi_payee_vpa);
            String name = getString(R.string.upi_payee_name);
            String uri = UpiPaymentUri.build(vpa, name, amount, txnRef);
            int size = (int) (getResources().getDisplayMetrics().density * 240);
            Bitmap bmp = QrBitmapEncoder.encode(uri, size);
            if (bmp != null) {
                imgQr.setImageBitmap(bmp);
                imgQr.setVisibility(View.VISIBLE);
                tvQrHint.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Could not create QR", Toast.LENGTH_SHORT).show();
            }
            showDone(btnDone);
        });

        btnDone.setOnClickListener(v -> persistAndReturnToDashboard());
    }

    private void recomputeSummary(TextView tvSummary, TextView tvTotal) {
        int nights = draft.nights();
        long roomSubtotal = 0;
        StringBuilder roomsLine = new StringBuilder();
        for (String id : draft.selectedRoomIds) {
            HotelRoom r = HotelRoom.findById(id, allRooms);
            if (r != null) {
                long line = (long) r.pricePerNight * nights;
                roomSubtotal += line;
                roomsLine.append("• Room ").append(r.label)
                        .append(" (₹").append(r.pricePerNight).append("/night × ").append(nights).append(")\n");
            } else {
                roomsLine.append("• Room ").append(id).append("\n");
            }
        }
        totalInr = roomSubtotal;
        String summary = "Guest: " + draft.customerName + "\n"
                + "Email: " + draft.email + "\n"
                + "Mobile: " + draft.mobile + "\n"
                + "Adults / children: " + draft.adults + " / " + draft.children + "\n"
                + "Stay: " + nights + " night(s)\n"
                + "Rooms:\n" + roomsLine
                + "Ref: " + txnRef;
        tvSummary.setText(summary);
        tvTotal.setText(String.format(Locale.getDefault(), "Total: ₹%,.0f", totalInr));
    }

    private void showDone(Button btnDone) {
        btnDone.setVisibility(View.VISIBLE);
    }

    private void persistAndReturnToDashboard() {
        if (totalInr <= 0) {
            Toast.makeText(this, "Unable to calculate total from live room layout.", Toast.LENGTH_LONG).show();
            return;
        }
        List<String> bookingRoomIds = new ArrayList<>();
        for (String selectedId : draft.selectedRoomIds) {
            HotelRoom room = HotelRoom.findById(selectedId, allRooms);
            bookingRoomIds.add(room != null ? room.bookingRoomId : selectedId);
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
                bookingRoomIds,
                allRooms,
                totalInr,
                paymentMethod,
                "reception"
        ).addOnSuccessListener(unused -> {
            ReceptionBookingDraft.reset();
            Intent i = new Intent(this, ReceptionDashboardActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Could not save booking: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
