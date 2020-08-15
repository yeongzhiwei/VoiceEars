package com.yeongzhiwei.voiceears.ttsstt;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Authentication {
    private static final String LOG_TAG = Authentication.class.getName();

    private String cognitiveServicesApiKey;
    private String cognitiveServicesAccessTokenUri;
    private String cognitiveServicesAccessToken;

    public static Boolean authenticate(String cognitiveServicesApiKey, String cognitiveServicesRegion) {
        Authentication authentication = new Authentication(cognitiveServicesApiKey, cognitiveServicesRegion);
        return authentication.cognitiveServicesAccessToken != null;
    }

    private Authentication(String cognitiveServicesApiKey, String cognitiveServicesRegion) {
        this.cognitiveServicesApiKey = cognitiveServicesApiKey;
        this.cognitiveServicesAccessTokenUri = "https://" + cognitiveServicesRegion + ".api.cognitive.microsoft.com/sts/v1.0/issueToken";

        Thread th = new Thread(() -> {
            getAccessToken();
        });

        try {
            th.start();
            th.join();
        } catch (Exception ex) {

        }
    }

    private void getAccessToken() {
        InputStream inputStream = null;
        HttpsURLConnection httpsURLConnection = null;

        //Prepare OAuth request
        try{
            URL url = new URL(this.cognitiveServicesAccessTokenUri);
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setConnectTimeout(5000);
            httpsURLConnection.setReadTimeout(5000);
            httpsURLConnection.setRequestProperty("Ocp-Apim-Subscription-Key", this.cognitiveServicesApiKey);
            httpsURLConnection.setRequestMethod("POST");

            String request = "";
            byte[] bytes = request.getBytes();
            httpsURLConnection.setRequestProperty("content-length", String.valueOf(bytes.length));
            httpsURLConnection.connect();

            DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
            dataOutputStream.write(bytes);
            dataOutputStream.flush();
            dataOutputStream.close();

            inputStream = httpsURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            httpsURLConnection.disconnect();

            this.cognitiveServicesAccessToken = stringBuffer.toString();

        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception error", e);
        }
    }
}
