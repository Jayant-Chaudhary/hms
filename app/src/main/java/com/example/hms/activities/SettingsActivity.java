package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.hms.R;
import com.example.hms.utils.SessionManager;
import com.example.hms.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SwitchCompat switchDarkMode = findViewById(R.id.switchDarkMode);
        switchDarkMode.setChecked(ThemeManager.isDarkModeEnabled(this));
        switchDarkMode.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                ThemeManager.setDarkModeEnabled(this, isChecked));

        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());
        findViewById(R.id.btnLogoutSettings).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            new SessionManager(this).clearSession();
            Intent i = new Intent(this, loginPage.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}
