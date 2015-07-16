package com.github.mjhassanpur.spotifystreamer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.github.mjhassanpur.spotifystreamer.R;

public class PreferenceHelper {

    public static String getCountry(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_key_country),
                context.getString(R.string.pref_country_default));
    }

    public static boolean isNotificationsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_key_notifications), true);
    }
}
