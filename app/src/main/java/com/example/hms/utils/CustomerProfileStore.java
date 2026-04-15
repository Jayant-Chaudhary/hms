package com.example.hms.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CustomerProfileStore {

    private static final String PREFS = "customer_profile_store";
    private static final String KEY_NAME = "name";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_GOV_ID_TYPE = "gov_id_type";
    private static final String KEY_GOV_ID_NUMBER = "gov_id_number";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_ADULTS = "adults";
    private static final String KEY_CHILDREN = "children";

    private final SharedPreferences prefs;

    public CustomerProfileStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getName() { return prefs.getString(KEY_NAME, ""); }
    public String getMobile() { return prefs.getString(KEY_MOBILE, ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getGovIdType() { return prefs.getString(KEY_GOV_ID_TYPE, "Aadhaar"); }
    public String getGovIdNumber() { return prefs.getString(KEY_GOV_ID_NUMBER, ""); }
    public String getGender() { return prefs.getString(KEY_GENDER, "Male"); }
    public int getAdults() { return prefs.getInt(KEY_ADULTS, 1); }
    public int getChildren() { return prefs.getInt(KEY_CHILDREN, 0); }

    public void setName(String name) {
        prefs.edit().putString(KEY_NAME, name).apply();
    }

    public void setEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email == null ? "" : email).apply();
    }

    public void saveProfile(
            String name,
            String mobile,
            String email,
            String govIdType,
            String govIdNumber,
            String gender,
            int adults,
            int children
    ) {
        prefs.edit()
                .putString(KEY_NAME, name)
                .putString(KEY_MOBILE, mobile)
                .putString(KEY_EMAIL, email)
                .putString(KEY_GOV_ID_TYPE, govIdType)
                .putString(KEY_GOV_ID_NUMBER, govIdNumber)
                .putString(KEY_GENDER, gender)
                .putInt(KEY_ADULTS, adults)
                .putInt(KEY_CHILDREN, children)
                .apply();
    }
}
