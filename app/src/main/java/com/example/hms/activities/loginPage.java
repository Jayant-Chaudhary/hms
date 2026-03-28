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
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class loginPage extends AppCompatActivity {

    private static final String TAG = "loginPage";
    private static final int RC_SIGN_IN = 100;

    EditText username, password;
    Button loginBtn, googleBtn;
    ProgressBar loginProgress;
    TextView goToRegister;
    CheckBox cbRememberMe;

    authManager auth;
    GoogleSignInClient googleSignInClient;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        
        // 1. Check if user is already logged in via SessionManager
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
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                            checkRoleAndRoute(userEmail);
                        }
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
                .collection("user_roles")
                .document(email)
                .get()
                .addOnSuccessListener(document -> {
                    String role = "customer";
                    if (document.exists()) {
                        role = document.getString("role");
                        if (role == null) role = "customer";
                    }

                    // Only save the session if "Remember Me" is checked
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
}