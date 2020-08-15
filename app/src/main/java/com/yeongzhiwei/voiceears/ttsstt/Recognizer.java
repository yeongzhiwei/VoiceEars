package com.yeongzhiwei.voiceears.ttsstt;

import android.util.Log;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Recognizer {

    public interface Recognition {
        void recognize(String text, Boolean isFinal);
    }

    private static final String LOG_TAG = Recognizer.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private SpeechConfig speechConfig;
    private SpeechRecognizer speechRecognizer = null;
    private Recognition recognition;
    private MicrophoneStream microphoneStream;

    private boolean continuousListeningStarted = false;

    public Recognizer(String cognitiveServicesApiKey, String cognitiveServicesRegion, Recognition recognition) {
        speechConfig = SpeechConfig.fromSubscription(cognitiveServicesApiKey, cognitiveServicesRegion);

        this.recognition = recognition;
    }

    synchronized public void startSpeechToText() {
        if (continuousListeningStarted) {
            return;
        }

        AudioConfig audioConfig = AudioConfig.fromStreamInput(createMicrophoneStream());
        speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);

        speechRecognizer.recognizing.addEventListener((o, speechRecognitionEventArgs) -> {
            final String s = speechRecognitionEventArgs.getResult().getText();
            Log.d(LOG_TAG, "recognizing: " + s);

            if (!s.isEmpty()) {
                recognition.recognize(s, false);
            }

        });

        speechRecognizer.recognized.addEventListener((o, speechRecognitionEventArgs) -> {
            final String s = speechRecognitionEventArgs.getResult().getText();
            Log.d(LOG_TAG, "recognized: " + s);

            if (!s.isEmpty()) {
                recognition.recognize(s, true);
            }

        });

        final Future<Void> task = speechRecognizer.startContinuousRecognitionAsync();

        setOnTaskCompletedListener(task, result -> {
            continuousListeningStarted = true;
            Log.d(LOG_TAG, "Started Speech to Text");
        });
    }

    synchronized public void stopSpeechToText() {
        if (speechRecognizer != null) {
            final Future<Void> task = speechRecognizer.stopContinuousRecognitionAsync();

            setOnTaskCompletedListener(task, result -> {
                continuousListeningStarted = false;
                Log.d(LOG_TAG, "Stopped Speech to Text");
            });
        } else {
            continuousListeningStarted = false;
            Log.d(LOG_TAG, "Speech to Text was not started");
        }
    }

    public void stopSpeechToTextAndReleaseMicrophone() {
        stopSpeechToText();
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
    }

    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

}
