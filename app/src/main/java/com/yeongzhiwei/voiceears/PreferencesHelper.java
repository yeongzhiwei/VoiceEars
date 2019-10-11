package com.yeongzhiwei.voiceears;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

class PreferencesHelper {
    private static final String sharedPreferencesName = "AFsvsVY0ja";

    enum Key {
        // WARNING: DO NOT CHANGE THE ORDER. ADD NEW KEY TO THE BOTTOM
        cognitiveServicesApiKeyKey,
        cognitiveServicesRegionKey,
        textViewSizeKey,
        genderKey,
        mirroredTextKey,
        mirroredTextViewSizeKey,
        mirroredMirrorMode,
        presentationMessagesKey,
        presentationSelectedMessageIndexKey,
        autoTTSKey
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(sharedPreferencesName, context.MODE_PRIVATE);
    }

    static void save(Context context, Key key, String newValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        sharedPreferences.edit().putString(key.toString(), newValue).commit();
    }

    static String loadString(Context context, Key key) {
        return loadString(context, key, null);
    }

    static String loadString(Context context, Key key, String defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        return sharedPreferences.getString(key.toString(), defaultValue);
    }

    static void save(Context context, Key key, ArrayList<String> newValues) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        if (!newValues.isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < newValues.size(); i++) {
                jsonArray.put(newValues.get(i));
            }
            sharedPreferences.edit().putString(key.toString(), jsonArray.toString()).commit();
        } else {
            sharedPreferences.edit().putString(key.toString(), null).commit();
        }
    }

    static ArrayList<String> loadStringArray(Context context, Key key, ArrayList<String> defaultValues) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        String json = sharedPreferences.getString(key.toString(), null);

        if (json != null) {
            try {
                ArrayList<String> values = new ArrayList<>();

                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    String value = jsonArray.optString(i);
                    values.add(value);
                }
                return values;
            } catch (JSONException e) {

            }
        }

        return defaultValues;
    }

    static void save(Context context, Key key, Integer newValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        sharedPreferences.edit().putInt(key.toString(), newValue).commit();
    }

    static int loadInt(Context context, Key key, Integer defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        return sharedPreferences.getInt(key.toString(), defaultValue);
    }
}