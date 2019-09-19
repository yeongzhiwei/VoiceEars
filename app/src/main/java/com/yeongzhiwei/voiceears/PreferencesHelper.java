package com.yeongzhiwei.voiceears;

import android.content.SharedPreferences;

class PreferencesHelper {
    static final String sharedPreferencesName = "hmLwWy669t";

    enum Key {
        // WARNING: DO NOT CHANGE THE ORDER. ADD NEW KEY TO THE BOTTOM
        cognitiveServicesApiKeyKey,
        cognitiveServicesRegionKey,
        textViewSizeKey,
        genderKey,
        mirroredTextKey,
        mirroredTextViewSizeKey,
        mirroredMirrorMode,
        audioSpeedKey;
    }

    static void save(SharedPreferences sharedPreferences, Key key, String newValue) {
        sharedPreferences.edit().putString(key.toString(), newValue).commit();
    }

    static String loadString(SharedPreferences sharedPreferences, Key key) {
        return loadString(sharedPreferences, key, null);
    }

    static String loadString(SharedPreferences sharedPreferences, Key key, String defaultValue) {
        return sharedPreferences.getString(key.toString(), defaultValue);
    }

    static void save(SharedPreferences sharedPreferences, Key key, Integer newValue) {
        sharedPreferences.edit().putInt(key.toString(), newValue).commit();
    }

    static Integer loadInt(SharedPreferences sharedPreferences, Key key, Integer defaultValue) {
        return sharedPreferences.getInt(key.toString(), defaultValue);
    }
}

/* Note

- Don't put MainActivity.sharedPreferences here for shared use among activities.
    - when the app is placed in the background, MainActivity may terminate and remove the sharedPreferences value from the namespace
 */