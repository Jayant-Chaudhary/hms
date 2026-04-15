package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.auth.authManager;
import com.example.hms.utils.SessionManager;
import com.example.hms.utils.ThemeManager;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class loginPage extends AppCompatActivity {

    private static final String TAG = "loginPage";
    private static final int RC_SIGN_IN = 100;
    // Add your test emails here (lowercase). Bypass works only in DEBUG builds.
    private static final Set<String> TEST_BYPASS_EMAILS = new HashSet<>(Arrays.asList(
            "testexample2@gmail.com",
            "testexample3@gmail.com",
            "testexample4@gmail.com"
    ));

    EditText username, password;
    Button loginBtn, googleBtn;
    ProgressBar loginProgress;
    TextView goToRegister;
    TextView tvForgotPassword;
    CheckBox cbRememberMe;

    authManager auth;
    GoogleSignInClient googleSignInClient;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);

        // Resume session when "Remember me" was used (must run before any clearSession).
        if (sessionManager.isLoggedIn()) {
            redirectBasedOnRole(sessionManager.getRole());
            return;
        }

        setContentView(R.layout.activity_login);

        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        loginBtn = findViewById(R.id.btnLogin);
        googleBtn = findViewById(R.id.btnGoogle);
        loginProgress = findViewById(R.id.loginProgress);
        goToRegister = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        auth = new authManager();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        if (loginBtn != null) {
            loginBtn.setOnClickListener(v -> {
                String emailStr = username.getText().toString().trim();
                String passStr = password.getText().toString().trim();

                if (emailStr.isEmpty() || passStr.isEmpty()) {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                setLoading(true);

                auth.login(emailStr, passStr, new authManager.Authcallback() {
                    @Override
                    public void onSuccess() {
                        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                            setLoading(false);
                            Toast.makeText(loginPage.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FirebaseAuth.getInstance().getCurrentUser().reload()
                                .addOnCompleteListener(reloadTask -> {
                                    if (!reloadTask.isSuccessful()) {
                                        setLoading(false);
                                        Toast.makeText(loginPage.this, "Could not verify account state. Please retry.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                                    boolean emailVerified = FirebaseAuth.getInstance().getCurrentUser().isEmailVerified();
                                    if (!emailVerified && !isVerificationBypassed(userEmail)) {
                                        setLoading(false);
                                        showVerificationRequiredDialog();
                                        return;
                                    }
                                    checkRoleAndRoute(userEmail);
                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        setLoading(false);
                        Toast.makeText(loginPage.this, "Login Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (googleBtn != null) {
            googleBtn.setOnClickListener(v -> {
                setLoading(true);
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        }

        if (goToRegister != null) {
            goToRegister.setOnClickListener(v -> {
                startActivity(new Intent(this, registerPage.class));
            });
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }
    }

    private void redirectBasedOnRole(String role) {
        Intent intent;
        switch (role.toLowerCase()) {
            case "admin":
                intent = new Intent(loginPage.this, AdminDashboardActivity.class);
                break;
            case "receptionist":
            case "reception":
                intent = new Intent(loginPage.this, ReceptionDashboardActivity.class);
                break;
            default:
                intent = new Intent(loginPage.this, CustomerDashboardActivity.class);
                break;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loginBtn.setEnabled(false);
            if (googleBtn != null) googleBtn.setEnabled(false);
            loginBtn.setText("");
            if (loginProgress != null) loginProgress.setVisibility(View.VISIBLE);
        } else {
            loginBtn.setEnabled(true);
            if (googleBtn != null) googleBtn.setEnabled(true);
            loginBtn.setText("Login");
            if (loginProgress != null) loginProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            auth.loginwithgoogle(account.getIdToken(), new authManager.Authcallback() {
                @Override
                public void onSuccess() {
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                        checkRoleAndRoute(userEmail);
                    }
                }
                @Override
                public void onFailure(String error) {
                    setLoading(false);
                    Toast.makeText(loginPage.this, "Firebase Auth Failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (ApiException e) {
            setLoading(false);
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed (Code: " + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
        }
    }

    private void checkRoleAndRoute(String email) {
        FirebaseFirestore.getInstance()
                .collection("roles")
                .document(email.toLowerCase())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = "customer";
                    if (documentSnapshot.exists()) {
                        role = documentSnapshot.getString("role");
                        if (role == null) role = "customer";
                    }
                    Log.d("ROLE_CHECK", "Email: " + email + " Role: " + role);
                    if (cbRememberMe.isChecked()) {
                        sessionManager.saveSession(email, email, role);
                    }
                    redirectBasedOnRole(role);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(loginPage.this, "Error fetching role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showForgotPasswordDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter your registered email");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setText(username.getText().toString().trim());
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("We will send a password reset link to your email.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    auth.sendPasswordReset(email, new authManager.Authcallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(loginPage.this, "Reset link sent. Check your inbox.", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(loginPage.this, "Failed to send reset link: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    private void showVerificationRequiredDialog() {
        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : username.getText().toString().trim();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email not verified")
                .setMessage("Please verify your email before login.\n\nEmail: " + email)
                .setNegativeButton("OK", (dialog, which) -> FirebaseAuth.getInstance().signOut())
                .setPositiveButton("Resend verification", (dialog, which) -> {
                    auth.sendEmailVerification(new authManager.Authcallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(loginPage.this, "Verification email sent again.", Toast.LENGTH_LONG).show();
                            FirebaseAuth.getInstance().signOut();
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(loginPage.this, "Could not resend verification: " + message, Toast.LENGTH_LONG).show();
                            FirebaseAuth.getInstance().signOut();
                        }
                    });
                })
                .show();
    }

    private boolean isVerificationBypassed(String email) {
        boolean isDebuggable = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!isDebuggable || email == null) {
            return false;
        }
        return TEST_BYPASS_EMAILS.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}