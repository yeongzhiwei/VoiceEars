package com.yeongzhiwei.voiceears;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

class TtsServiceClient {
    private static final String LOG_TAG = TtsServiceClient.class.getName();
    private static final String contentType = "application/ssml+xml";
    private static final String audioOutputFormat = "raw-16khz-16bit-mono-pcm";
    private final String serviceUri;
    private final Authentication authentication;
    private byte[] result;

    TtsServiceClient(String apiKey, String region) {
        authentication = new Authentication(apiKey, region);
        serviceUri = "https://" + region + ".tts.speech.microsoft.com/cognitiveservices/v1";
    }

    byte[] speakSSML(final String ssml) {
        Thread thread = new Thread(() -> {
            doWork(ssml);
        });

        try {
            thread.start();
            thread.join();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception error", e);
        }

        return result;
    }

    private void doWork(String ssml) {
        int code;
        synchronized(authentication) {
            String accessToken = authentication.getAccessToken();
            try {
                URL url = new URL(serviceUri);
                HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(15000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", contentType);
                urlConnection.setRequestProperty("X-MICROSOFT-OutputFormat", audioOutputFormat);
                urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
                urlConnection.setRequestProperty("X-Search-AppId", "07D3234E49CE426DAA29772419F436CA");
                urlConnection.setRequestProperty("X-Search-ClientID", "1ECFAE91408841A480F00935DC390960");
                urlConnection.setRequestProperty("User-Agent", "TTSAndroid");
                urlConnection.setRequestProperty("Accept", "*/*");
                byte[] ssmlBytes = ssml.getBytes();
                urlConnection.setRequestProperty("content-length", String.valueOf(ssmlBytes.length));
                urlConnection.connect();
                urlConnection.getOutputStream().write(ssmlBytes);
                code = urlConnection.getResponseCode();
                if (code == 200) {
                    InputStream in = urlConnection.getInputStream();
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    byte[] bytes = new byte[1024];
                    int ret = in.read(bytes);
                    while (ret > 0) {
                        bout.write(bytes, 0, ret);
                        ret = in.read(bytes);
                    }
                    result = bout.toByteArray();
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception error", e);
            }
        }
    }
}
