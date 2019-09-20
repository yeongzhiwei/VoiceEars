package com.yeongzhiwei.voiceears;

import android.content.Context;

class Helper {
    static Boolean authenticateApiKey(Context context) {
        final String cognitiveServicesApiKey = PreferencesHelper.loadString(context, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        final String cognitiveServicesRegion = PreferencesHelper.loadString(context, PreferencesHelper.Key.cognitiveServicesRegionKey);

        if (cognitiveServicesApiKey == null || cognitiveServicesRegion == null) {
            return false;
        }

        final Authentication authentication = new Authentication(cognitiveServicesApiKey, cognitiveServicesRegion, false);
        return authentication.getAccessToken() != null;
    }
}
