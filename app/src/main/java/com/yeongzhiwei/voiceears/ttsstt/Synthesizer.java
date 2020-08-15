package com.yeongzhiwei.voiceears.ttsstt;

import android.util.Log;

import com.microsoft.cognitiveservices.speech.*;

import java.util.concurrent.ExecutionException;

public class Synthesizer {

    private static final String LOG_TAG = Synthesizer.class.getSimpleName();

    private static final String MALE_VOICE_NAME = "en-US-GuyNeural";
    private static final String FEMALE_VOICE_NAME = "en-US-JessaNeural";

    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;

    public Synthesizer(String cognitiveServicesApiKey, String cognitiveServicesRegion, Gender gender) {
        speechConfig = SpeechConfig.fromSubscription(cognitiveServicesApiKey, cognitiveServicesRegion);
        speechConfig.setSpeechSynthesisVoiceName(getVoiceName(gender));

        synthesizer = new SpeechSynthesizer(speechConfig);
    }

    synchronized public void speak(String text, Runnable callOnStart, Runnable callOnEnd, Runnable callOnError) {
        try {
            if (callOnStart != null) {
                callOnStart.run();
            }

            SpeechSynthesisResult result = synthesizer.SpeakTextAsync(text).get();

            if (callOnEnd != null) {
                callOnEnd.run();
            }

            if (result.getReason() == ResultReason.Canceled && callOnError != null) {
                callOnError.run();
            }

            result.close();
        } catch (InterruptedException ex) {
            Log.e(LOG_TAG, "InterruptedException: " + ex.getMessage());
        } catch (ExecutionException ex) {
            Log.e(LOG_TAG, "ExecutionException: " + ex.getMessage());
        }
    }

    private static String getVoiceName(Gender gender) {
        switch (gender) {
            case Male:
                return MALE_VOICE_NAME;
            case Female:
                return FEMALE_VOICE_NAME;
            default:
                return "";
        }
    }

}

