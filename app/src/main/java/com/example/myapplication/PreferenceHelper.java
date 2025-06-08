package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {
    private static final String PREF_NAME = "app_preferences";
    private static final String KEY_FINGERPRINT_ENABLED = "fingerprint_enabled";

    public static void setFingerprintEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FINGERPRINT_ENABLED, enabled).apply();
    }

    public static boolean isFingerprintEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FINGERPRINT_ENABLED, false);
    }
}
