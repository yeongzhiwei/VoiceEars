package com.yeongzhiwei.voiceears;

import android.util.Log;

import com.microsoft.cognitiveservices.speech.*;

import java.util.concurrent.ExecutionException;

// https://github.com/Azure-Samples/cognitive-services-speech-sdk/blob/master/samples/java/jre/console/src/com/microsoft/cognitiveservices/speech/samples/console/SpeechSynthesisSamples.java
class Synthesizer {
    enum Gender {
        Male, Female;

        Gender toggle() {
            return (this.equals(Male)) ? Female : Male;
        }
    }

    private static final String LOG_TAG = Synthesizer.class.getSimpleName();

    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;

    Synthesizer(String cognitiveServicesApiKey, String cognitiveServicesRegion, Gender gender) {
        speechConfig = SpeechConfig.fromSubscription(cognitiveServicesApiKey, cognitiveServicesRegion);
        setVoiceGender(gender);

        synthesizer = new SpeechSynthesizer(speechConfig);
    }

    synchronized void speak(String text, Runnable callOnStart, Runnable callOnEnd, Runnable callOnError) {
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

    void setVoiceGender(Gender gender) {
        if (speechConfig == null) {
            return;
        }

        if (synthesizer != null) {
            synthesizer.close();
        }

        if (gender == Gender.Male) {
            speechConfig.setSpeechSynthesisVoiceName("en-US-GuyNeural");
        } else {
            speechConfig.setSpeechSynthesisVoiceName("en-US-JessaNeural");
        }

        synthesizer = new SpeechSynthesizer(speechConfig);
    }
}

