package com.example.hms.model.admin;

import com.google.firebase.Timestamp;

public class AuthorizedUser {
    public String id;
    public String email;
    public String role;
    public boolean active = true;
    public Timestamp updatedAt;

    public AuthorizedUser() {
    }
}
