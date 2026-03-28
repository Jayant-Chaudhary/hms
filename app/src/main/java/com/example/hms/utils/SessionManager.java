package com.example.hms.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    public static final String PREF_NAME="HMS";
    public static final String KEY_EMAIL="email";
    public static final String KEY_ROLE="role";
    public  static  final String KEY_NAME="name";
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(String email, String name, String role) {
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_NAME,  name);
        editor.putString(KEY_ROLE,  role);
        editor.apply();
    }

    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getName()  { return prefs.getString(KEY_NAME,  ""); }
    public String getRole()  { return prefs.getString(KEY_ROLE,  ""); }

    public boolean isLoggedIn() { return !getRole().isEmpty(); }

    public void clearSession() { editor.clear().apply(); }
}




