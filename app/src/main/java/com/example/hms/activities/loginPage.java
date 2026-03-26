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
import com.google.firebase.auth.FirebaseUser;

public class loginPage extends AppCompatActivity {

    private static final String TAG = "loginPage";
    private static final int RC_SIGN_IN = 100;
    private static final String ADMIN_EMAIL = "jayant10chaudhary@gmail.com"; // Set your specific admin email here

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
                    handleUserRedirection();
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

    private void handleUserRedirection() {
        FirebaseUser user = authManager.getAuth().getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            if (userEmail != null && userEmail.equalsIgnoreCase(ADMIN_EMAIL)) {
                startActivity(new Intent(loginPage.this, AdminDashboardActivity.class));
            } else {
                startActivity(new Intent(loginPage.this, MainActivity.class)); // Customer Dashboard
            }
            finish();
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

            authManager.loginwithgoogle(account.getIdToken(), new authManager.Authcallback() {
                @Override
                public void onSuccess() {
                    handleUserRedirection();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(loginPage.this, "Firebase Auth Failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (ApiException e) {
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed (Code: " + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
        }
    }
}