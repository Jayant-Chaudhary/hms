package com.example.hms.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.hms.R;
import com.example.hms.auth.authManager;
import com.example.hms.utils.ThemeManager;
import com.google.firebase.auth.FirebaseUser;

public class registerPage extends AppCompatActivity {
    EditText email, password, confirmPassword;
    Button registerBtn;
    ProgressBar registerProgress;
    TextView tvBackToLogin;
    authManager authManager;
    private boolean awaitingVerification = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
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
                    authManager.sendEmailVerification(new authManager.Authcallback() {
                        @Override
                        public void onSuccess() {
                            setLoading(false);
                            awaitingVerification = true;
                            showVerificationDialog("Verification email sent.");
                        }

                        @Override
                        public void onFailure(String message) {
                            setLoading(false);
                            awaitingVerification = true;
                            showVerificationDialog("Could not send verification email: " + message);
                        }
                    });
                }

                @Override
                public void onFailure(String message) {
                    setLoading(false);
                    Toast.makeText(registerPage.this, "Registration failed: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        tvBackToLogin.setOnClickListener(v -> {
            if (awaitingVerification) {
                showCancelRegistrationDialog();
                return;
            }
            finish();
        });
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

    private void showVerificationDialog(String statusMessage) {
        new AlertDialog.Builder(this)
                .setTitle("Verify your email")
                .setMessage(statusMessage + "\n\nOpen the verification link from your inbox (or spam folder), then tap \"I've verified\".")
                .setCancelable(false)
                .setNegativeButton("Cancel registration", (dialog, which) -> showCancelRegistrationDialog())
                .setNeutralButton("Resend email", (dialog, which) -> resendVerificationEmail())
                .setPositiveButton("I've verified", (dialog, which) -> checkVerificationAndComplete())
                .show();
    }

    private void resendVerificationEmail() {
        setLoading(true);
        authManager.sendEmailVerification(new authManager.Authcallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(registerPage.this, "Verification email sent again.", Toast.LENGTH_LONG).show();
                showVerificationDialog("Verification email resent.");
            }

            @Override
            public void onFailure(String message) {
                setLoading(false);
                showVerificationDialog("Resend failed: " + message);
            }
        });
    }

    private void checkVerificationAndComplete() {
        FirebaseUser user = authManager.getAuth().getCurrentUser();
        if (user == null) {
            showVerificationDialog("Session expired. Please register again.");
            return;
        }
        setLoading(true);
        user.reload().addOnCompleteListener(task -> {
            setLoading(false);
            if (!task.isSuccessful()) {
                showVerificationDialog("Could not refresh verification status. Please retry.");
                return;
            }
            if (authManager.getAuth().getCurrentUser() != null && authManager.getAuth().getCurrentUser().isEmailVerified()) {
                awaitingVerification = false;
                Toast.makeText(this, "Email verified. Registration complete.", Toast.LENGTH_LONG).show();
                authManager.getAuth().signOut();
                finish();
            } else {
                showVerificationDialog("Email still not verified.");
            }
        });
    }

    private void showCancelRegistrationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel registration?")
                .setMessage("Your account is not verified yet. Do you want to cancel and delete this pending registration?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, cancel", (dialog, which) -> cancelPendingRegistration())
                .show();
    }

    private void cancelPendingRegistration() {
        FirebaseUser user = authManager.getAuth().getCurrentUser();
        if (user == null) {
            awaitingVerification = false;
            finish();
            return;
        }
        user.delete().addOnCompleteListener(task -> {
            authManager.getAuth().signOut();
            awaitingVerification = false;
            Toast.makeText(this, "Pending registration cancelled.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}