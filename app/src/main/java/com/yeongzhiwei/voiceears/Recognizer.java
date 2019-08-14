package com.yeongzhiwei.voiceears;

import android.text.TextUtils;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class Recognizer {
    private static final String LOG_TAG = Recognizer.class.getSimpleName();

    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    private SpeechConfig speechConfig;
    private boolean continuousListeningStarted = false;
    private SpeechRecognizer speechRecognizer = null;
    private ArrayList<String> content = new ArrayList<>();

    private static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    private RecognizerUI recognizerUI;
    private Runnable runOnStop;

    Recognizer(String speechSubscriptionKey, String speechRegion, RecognizerUI recognizerUI, Runnable runOnStop) {
        speechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, speechRegion);

        this.recognizerUI = recognizerUI;
        this.runOnStop = runOnStop;
    }


    // Adapted from Sample code for the Microsoft Cognitive Services Speech SDK
    // https://github.com/Azure-Samples/cognitive-services-speech-sdk
    synchronized void startSpeechToText() {
        Log.d(LOG_TAG, "started Speech to Text");

        if (continuousListeningStarted) {
            return;
        }

        try {
            content.clear();

            AudioConfig audioConfig = AudioConfig.fromStreamInput(createMicrophoneStream());
            speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);

            speechRecognizer.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                content.add(s);
                String message = TextUtils.join(" ", content).trim();
                if (message.length() != 0) {
                    recognizerUI.update(message);
                }
                content.remove(content.size() - 1);
            });

            speechRecognizer.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                content.add(s);
                String message = TextUtils.join(" ", content).trim();
                if (message.length() != 0) {
                    recognizerUI.update(message);
                }
            });

            final Future<Void> task = speechRecognizer.startContinuousRecognitionAsync();
            setOnTaskCompletedListener(task, result -> {
                continuousListeningStarted = true;
            });
        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.toString());
        }
    }

    synchronized void stopSpeechToText() {
        Log.d(LOG_TAG, "stopped Speech to Text");

        if (speechRecognizer != null) {
            final Future<Void> task = speechRecognizer.stopContinuousRecognitionAsync();
            setOnTaskCompletedListener(task, result -> {
                continuousListeningStarted = false;
                if (runOnStop != null) {
                    runOnStop.run();
                }
            });
        } else {
            continuousListeningStarted = false;
            if (runOnStop != null) {
                runOnStop.run();
            }
        }
    }

    public interface RecognizerUI {
        void update(String result);
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }
}
