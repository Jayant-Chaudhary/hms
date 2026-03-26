package com.example.hms.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME  = "GymSession";
    private static final String KEY_EMAIL  = "email";
    private static final String KEY_ROLE   = "role";
    private static final String KEY_NAME   = "name";
    private static final String KEY_UID    = "uid";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(String uid, String email, String name, String role) {
        editor.putString(KEY_UID,   uid);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_NAME,  name);
        editor.putString(KEY_ROLE,  role);
        editor.apply();
    }

    public String getUid()   { return prefs.getString(KEY_UID,   ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getName()  { return prefs.getString(KEY_NAME,  ""); }
    public String getRole()  { return prefs.getString(KEY_ROLE,  ""); }

    public boolean isAdmin()     { return FirestoreHelper.ROLE_ADMIN.equals(getRole()); }
    public boolean isReception() { return FirestoreHelper.ROLE_RECEPTION.equals(getRole()); }

    public void clearSession() { editor.clear().apply(); }
}
