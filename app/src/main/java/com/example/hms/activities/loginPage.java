package com.example.hms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.MainActivity;
import com.example.hms.R;
import com.example.hms.auth.authManager;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class loginPage extends AppCompatActivity {

    private static final String TAG = "loginPage";
    private static final int RC_SIGN_IN = 100;

    EditText email, password;
    Button loginBtn, googleBtn;
    TextView goToRegister;

    authManager authManager;
    GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        googleBtn = findViewById(R.id.googleBtn);
        goToRegister = findViewById(R.id.goToRegister);

        authManager = new authManager();

        // Google setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Login
        loginBtn.setOnClickListener(v -> {
            String emailStr = email.getText().toString().trim();
            String passStr = password.getText().toString().trim();

            if (emailStr.isEmpty() || passStr.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            authManager.login(emailStr, passStr, new authManager.Authcallback() {
                @Override
                public void onSuccess() {
                    startActivity(new Intent(loginPage.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(loginPage.this, "Login Failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Google button
        googleBtn.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        // Go to register
        goToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, registerPage.class));
        });
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

            // Signed in successfully, now authenticate with Firebase
            authManager.loginwithgoogle(account.getIdToken(), new authManager.Authcallback() {
                @Override
                public void onSuccess() {
                    startActivity(new Intent(loginPage.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(loginPage.this, "Firebase Auth Failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Common codes: 10 (Developer Error - usually SHA-1/Client ID), 12500, etc.
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed (Code: " + e.getStatusCode() + "). Check Logcat for details.", Toast.LENGTH_LONG).show();
        }
    }
}