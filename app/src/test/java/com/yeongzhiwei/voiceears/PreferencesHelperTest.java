package com.yeongzhiwei.voiceears;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PreferencesHelperTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void saveAndLoadStringShouldReturnSameString() {
        String expected = "alpha";
        PreferencesHelper.save(context, PreferencesHelper.Key.cognitiveServicesApiKeyKey, expected);
        String actual = PreferencesHelper.loadString(context, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        assertEquals(expected, actual);
    }

    @Test
    public void saveAndLoadArrayListShouldReturnSameArrayList() {
        ArrayList<String> expected = new ArrayList<>();
        expected.add("alpha");
        expected.add("beta");
        PreferencesHelper.save(context, PreferencesHelper.Key.presentationMessagesKey, expected);
        ArrayList<String> actual = PreferencesHelper.loadStringArray(context, PreferencesHelper.Key.presentationMessagesKey, new ArrayList<>());
        assertEquals(expected, actual);
    }

    @Test
    public void saveAnLoaIntShouldReturnSameInt() {
        int expected = 888;
        PreferencesHelper.save(context, PreferencesHelper.Key.textViewSizeKey, expected);
        int actual = PreferencesHelper.loadInt(context, PreferencesHelper.Key.textViewSizeKey, 0);
        assertEquals(expected, actual);
    }

}
