package com.yeongzhiwei.voiceears;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

class Authentication {
    private static final String LOG_TAG = Authentication.class.getName();

    private String apiKey;
    private String accessTokenUri;
    private String accessToken;
    private Timer accessTokenRenewer;

    // Access Token expires every 10 minutes. Renew it every 9 minutes only.
    private TimerTask nineMinitesTask = null;
    private final int RefreshTokenDuration = 9 * 60 * 1000;

    Authentication(String apiKey, String region) {
        this.apiKey = apiKey;
        this.accessTokenUri = "https://" + region + ".api.cognitive.microsoft.com/sts/v1.0/issueToken";

        Thread thread = new Thread(() -> {
            RenewAccessToken();
        });

        try {
            thread.start();
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // renew the accessToken every specified minutes
        accessTokenRenewer = new Timer();
        nineMinitesTask = new TimerTask() {
            public void run() {
                RenewAccessToken();
            }
        };

        accessTokenRenewer.schedule(nineMinitesTask, RefreshTokenDuration, RefreshTokenDuration);
    }

    String getAccessToken() {
        return this.accessToken;
    }

    private void RenewAccessToken() {
        synchronized(this) {
            HttpPost(accessTokenUri, this.apiKey);

            if (this.accessToken != null) {
                Log.d(LOG_TAG, "new Access Token: " + this.accessToken);
            }
        }
    }

    private void HttpPost(String accessTokenUri, String apiKey) {
        InputStream inSt = null;
        HttpsURLConnection webRequest = null;

        this.accessToken = null;
        //Prepare OAuth request
        try{
            URL url = new URL(accessTokenUri);
            webRequest = (HttpsURLConnection) url.openConnection();
            webRequest.setDoInput(true);
            webRequest.setDoOutput(true);
            webRequest.setConnectTimeout(5000);
            webRequest.setReadTimeout(5000);
            webRequest.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey);
            webRequest.setRequestMethod("POST");

            String request = "";
            byte[] bytes = request.getBytes();
            webRequest.setRequestProperty("content-length", String.valueOf(bytes.length));
            webRequest.connect();

            DataOutputStream dop = new DataOutputStream(webRequest.getOutputStream());
            dop.write(bytes);
            dop.flush();
            dop.close();

            inSt = webRequest.getInputStream();
            InputStreamReader in = new InputStreamReader(inSt);
            BufferedReader bufferedReader = new BufferedReader(in);
            StringBuffer strBuffer = new StringBuffer();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                strBuffer.append(line);
            }

            bufferedReader.close();
            in.close();
            inSt.close();
            webRequest.disconnect();

            this.accessToken = strBuffer.toString();

        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception error", e);
        }
    }
}
