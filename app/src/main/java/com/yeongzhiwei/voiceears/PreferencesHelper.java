package com.yeongzhiwei.voiceears;

class PreferencesHelper {
    static final String sharedPreferencesName = "hmLwWy669t";

    enum Key {
        cognitiveServicesApiKeyKey,
        cognitiveServicesRegionKey,
        textViewSizeKey,
        genderKey,
        mirroredTextKey,
        mirroredTextViewSizeKey,
        mirroredMirrorMode;
    }

    static void save(Key key, String newValue) {
        MainActivity.sharedPreferences.edit().putString(key.toString(), newValue).commit();
    }

    static String loadString(Key key) {
        return loadString(key, null);
    }

    static String loadString(Key key, String defaultValue) {
        return MainActivity.sharedPreferences.getString(key.toString(), defaultValue);
    }

    static void save(Key key, Integer newValue) {
        MainActivity.sharedPreferences.edit().putInt(key.toString(), newValue).commit();
    }

    static Integer loadInt(Key key) {
        return loadInt(key, null);
    }

    static Integer loadInt(Key key, Integer defaultValue) {
        return MainActivity.sharedPreferences.getInt(key.toString(), defaultValue);
    }
}
