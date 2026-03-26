package com.example.hms.activities;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hms.R;
import com.example.hms.auth.authManager;

public class registerPage extends AppCompatActivity {
    EditText email,password;
    Button registerBtn;
    authManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        email=findViewById(R.id.emailAddress);
        password=findViewById(R.id.password);
        registerBtn=findViewById(R.id.registerBtn);
        authManager=new authManager();
        registerBtn.setOnClickListener(v-> {
            String e = email.getText().toString();
            String p = password.getText().toString();
            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "please fill all the fields", Toast.LENGTH_SHORT).show();
            } else {
                authManager.register(e, p, new authManager.Authcallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(registerPage.this, "registration successful", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(registerPage.this, "registration failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }


        });


}
}
