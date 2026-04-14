package com.example.hms.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {

    private static final String PREFS = "hms_theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private ThemeManager() {}

    public static void applyTheme(Context context) {
        boolean dark = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean isDarkModeEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkModeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
