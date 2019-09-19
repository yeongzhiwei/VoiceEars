package com.yeongzhiwei.voiceears;

import android.content.SharedPreferences;

class Helper {
    static Boolean authenticateApiKey(SharedPreferences sharedPreferences) {
        final String cognitiveServicesApiKey = PreferencesHelper.loadString(sharedPreferences, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        final String cognitiveServicesRegion = PreferencesHelper.loadString(sharedPreferences, PreferencesHelper.Key.cognitiveServicesRegionKey);

        if (cognitiveServicesApiKey == null || cognitiveServicesRegion == null) {
            return false;
        }

        final Authentication authentication = new Authentication(cognitiveServicesApiKey, cognitiveServicesRegion, false);
        return authentication.getAccessToken() != null;
    }
}
