package com.example.hms.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.auth.authManager;

public class registerPage extends AppCompatActivity {
    EditText email, password, confirmPassword;
    Button registerBtn;
    ProgressBar registerProgress;
    TextView tvBackToLogin;
    authManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = findViewById(R.id.emailAddress);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        registerBtn = findViewById(R.id.registerBtn);
        registerProgress = findViewById(R.id.registerProgress);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        authManager = new authManager();

        registerBtn.setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String p = password.getText().toString().trim();
            String cp = confirmPassword.getText().toString().trim();

            if (e.isEmpty() || p.isEmpty() || cp.isEmpty()) {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!p.equals(cp)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);

            authManager.register(e, p, new authManager.Authcallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    Toast.makeText(registerPage.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    setLoading(false);
                    Toast.makeText(registerPage.this, "Registration failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            registerBtn.setEnabled(false);
            registerBtn.setText("");
            registerProgress.setVisibility(View.VISIBLE);
        } else {
            registerBtn.setEnabled(true);
            registerBtn.setText("Register");
            registerProgress.setVisibility(View.GONE);
        }
    }
}