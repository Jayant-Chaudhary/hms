package com.example.hms.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.utils.ReceptionBookingDraft;
import com.example.hms.utils.ThemeManager;

import java.util.Calendar;
import java.util.Locale;

public class ReceptionCustomerRegistrationActivity extends AppCompatActivity {

    private EditText etName, etMobile, etEmail, etGovIdNumber, etAdults, etChildren, etCheckIn, etCheckOut, etOtherGovIdType;
    private Spinner spinnerGender, spinnerGovIdType;

    private final Calendar checkInCal = Calendar.getInstance();
    private final Calendar checkOutCal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reception_customer_registration);

        etName = findViewById(R.id.etName);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etGovIdNumber = findViewById(R.id.etGovIdNumber);
        etAdults = findViewById(R.id.etAdults);
        etChildren = findViewById(R.id.etChildren);
        etCheckIn = findViewById(R.id.etCheckIn);
        etCheckOut = findViewById(R.id.etCheckOut);
        etOtherGovIdType = findViewById(R.id.etOtherGovIdType);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerGovIdType = findViewById(R.id.spinnerGovIdType);

        // Pre-fill from draft (if initiated from Customer Dashboard)
        ReceptionBookingDraft draft = ReceptionBookingDraft.get();
        if (draft.email != null && !draft.email.isEmpty()) {
            etEmail.setText(draft.email);
        }
        if (draft.customerName != null && !draft.customerName.isEmpty()) {
            etName.setText(draft.customerName);
        }

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Male", "Female", "Other"});
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<String> govAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Aadhaar", "Passport", "Driving license", "Other"});
        spinnerGovIdType.setAdapter(govAdapter);

        spinnerGovIdType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if ("Other".equals(parent.getItemAtPosition(position).toString())) {
                    etOtherGovIdType.setVisibility(View.VISIBLE);
                } else {
                    etOtherGovIdType.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        checkOutCal.add(Calendar.DAY_OF_MONTH, 1);
        refreshDateLabels();

        etCheckIn.setOnClickListener(v -> showPicker(checkInCal, true));
        etCheckOut.setOnClickListener(v -> showPicker(checkOutCal, false));

        Button btn = findViewById(R.id.btnConfirmSelectRoom);
        btn.setOnClickListener(v -> attemptContinue());
    }

    private void showPicker(Calendar target, boolean isCheckIn) {
        DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            target.set(Calendar.YEAR, year);
            target.set(Calendar.MONTH, month);
            target.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            if (isCheckIn && !checkOutCal.after(checkInCal)) {
                checkOutCal.setTimeInMillis(checkInCal.getTimeInMillis());
                checkOutCal.add(Calendar.DAY_OF_MONTH, 1);
            }
            if (!isCheckIn && checkOutCal.before(checkInCal)) {
                Toast.makeText(this, "Check-out must be after check-in", Toast.LENGTH_SHORT).show();
                return;
            }
            refreshDateLabels();
        }, target.get(Calendar.YEAR), target.get(Calendar.MONTH), target.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void refreshDateLabels() {
        etCheckIn.setText(formatDate(checkInCal));
        etCheckOut.setText(formatDate(checkOutCal));
    }

    private static String formatDate(Calendar c) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private void attemptContinue() {
        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String govNum = etGovIdNumber.getText().toString().trim();
        String govType = spinnerGovIdType.getSelectedItem().toString();
        String adultsStr = etAdults.getText().toString().trim();
        String childrenStr = etChildren.getText().toString().trim();

        if (name.isEmpty() || mobile.isEmpty() || email.isEmpty() || govNum.isEmpty() || adultsStr.isEmpty() || childrenStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("Other".equals(govType)) {
            govType = etOtherGovIdType.getText().toString().trim();
            if (govType.isEmpty()) {
                Toast.makeText(this, "Please specify the Government ID type", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (mobile.length() < 10) {
            Toast.makeText(this, "Enter a valid mobile number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!email.contains("@")) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        int adults;
        try {
            adults = Integer.parseInt(adultsStr);
        } catch (NumberFormatException e) {
            adults = 0;
        }
        if (adults < 1) {
            Toast.makeText(this, "At least 1 adult required", Toast.LENGTH_SHORT).show();
            return;
        }

        int children;
        try {
            children = Integer.parseInt(childrenStr);
        } catch (NumberFormatException e) {
            children = 0;
        }

        if (!checkOutCal.after(checkInCal)) {
            Toast.makeText(this, "Check-out must be after check-in", Toast.LENGTH_SHORT).show();
            return;
        }

        ReceptionBookingDraft d = ReceptionBookingDraft.get();
        d.customerName = name;
        d.gender = spinnerGender.getSelectedItem().toString();
        d.mobile = mobile;
        d.email = email;
        d.govIdType = govType;
        d.govIdNumber = govNum;
        d.adults = adults;
        d.children = children;
        d.checkInMillis = startOfDay(checkInCal);
        d.checkOutMillis = startOfDay(checkOutCal);

        startActivity(new Intent(this, ReceptionRoomSelectionActivity.class));
    }

    private static long startOfDay(Calendar c) {
        Calendar x = (Calendar) c.clone();
        x.set(Calendar.HOUR_OF_DAY, 0);
        x.set(Calendar.MINUTE, 0);
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);
        return x.getTimeInMillis();
    }
}
