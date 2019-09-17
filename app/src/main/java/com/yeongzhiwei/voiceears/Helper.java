package com.yeongzhiwei.voiceears;

class Helper {
    static Boolean authenticateApiKey() {
        final String cognitiveServicesApiKey = PreferencesHelper.loadString(PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        final String cognitiveServicesRegion = PreferencesHelper.loadString(PreferencesHelper.Key.cognitiveServicesRegionKey);

        if (cognitiveServicesApiKey == null || cognitiveServicesRegion == null) {
            return false;
        }

        final Authentication authentication = new Authentication(cognitiveServicesApiKey, cognitiveServicesRegion, false);
        return authentication.getAccessToken() != null;
    }
}
