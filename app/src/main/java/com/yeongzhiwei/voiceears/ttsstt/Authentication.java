package com.yeongzhiwei.voiceears.ttsstt;

import android.util.Log;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Authentication {

    private static final String LOG_TAG = Authentication.class.getName();

    private static boolean isAuthenticated = false;

    public static boolean authenticate(String cognitiveServicesApiKey, String cognitiveServicesRegion) {
        isAuthenticated = false;

        Thread th = new Thread() {
            public void run() {
                try {
                    URL url = new URL("https://" + cognitiveServicesRegion + ".api.cognitive.microsoft.com/sts/v1.0/issueToken");
                    HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                    httpsURLConnection.setRequestProperty("Ocp-Apim-Subscription-Key", cognitiveServicesApiKey);
                    httpsURLConnection.setRequestMethod("POST");
                    httpsURLConnection.connect();
                    int responseCode = httpsURLConnection.getResponseCode();
                    isAuthenticated = responseCode == 200;
                } catch (Exception ex) {
                    Log.d(LOG_TAG, "Exception occurred when attempting to get the token", ex);
                }
            }
        };

        try {
            th.start();
            th.join();
        } catch (Exception ex) {
            Log.d(LOG_TAG, "Exception occurred when starting or joining the thread to get the token", ex);
        }

        return isAuthenticated;
    }

}
