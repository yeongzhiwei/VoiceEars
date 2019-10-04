package com.yeongzhiwei.voiceears;

import android.text.TextUtils;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

class Recognizer {
    private static final String LOG_TAG = Recognizer.class.getSimpleName();
    private static ExecutorService s_executorService = Executors.newCachedThreadPool();

    public interface RecognizerUI {
        void update(Integer order, String result);
    }

    private SpeechConfig speechConfig;
    private RecognizerUI recognizerUI;


    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    private boolean continuousListeningStarted = false;
    private SpeechRecognizer speechRecognizer = null;
    private ArrayList<String> content = new ArrayList<>();
    private int counter = 0;
    private ReentrantLock lock = new ReentrantLock(); // mutual exclusion: content & counter

    private LoopHandler resetLoopHandler = new LoopHandler(3000, () -> {
        lock.lock();
        try {
            content.clear();
            counter++;
        } finally {
            lock.unlock();
        }
    });

    Recognizer(String cognitiveServicesApiKey, String cognitiveServicesRegion, RecognizerUI recognizerUI) {
        speechConfig = SpeechConfig.fromSubscription(cognitiveServicesApiKey, cognitiveServicesRegion);

        this.recognizerUI = recognizerUI;
    }

    // Adapted from Sample code for the Microsoft Cognitive Services Speech SDK
    // https://github.com/Azure-Samples/cognitive-services-speech-sdk
    synchronized void startSpeechToText() {
        if (continuousListeningStarted) {
            return;
        }

        try {
            lock.lock();
            try {
                content.clear();
                counter++;
            } finally {
                lock.unlock();
            }

            AudioConfig audioConfig = AudioConfig.fromStreamInput(createMicrophoneStream());
            speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);

            speechRecognizer.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                String message;
                int order = -1;  // Note: will crash if not updated to non-negative
                lock.lock();
                try {
                    message = TextUtils.join(" ", content).trim() + " " + s;
                    order = counter;
                } finally {
                    lock.unlock();
                }

                if (message.length() != 0) {
                    resetLoopHandler.reset();
                    recognizerUI.update(order, message);
                }
            });

            speechRecognizer.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                String message;
                int order = -1; // Note: will crash if not updated to non-negative
                lock.lock();
                try {
                    content.add(s);
                    message = TextUtils.join(" ", content).trim();
                    order = counter;
                } finally {
                    lock.unlock();
                }

                if (message.length() != 0) {
                    resetLoopHandler.reset();
                    recognizerUI.update(order, message);
                }
            });

            final Future<Void> task = speechRecognizer.startContinuousRecognitionAsync();
            setOnTaskCompletedListener(task, result -> {
                continuousListeningStarted = true;
                Log.d(LOG_TAG, "Started Speech to Text");
            });
        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.toString());
        }
    }

    synchronized void stopSpeechToText() {
        resetLoopHandler.stop();
        if (speechRecognizer != null) {
            final Future<Void> task = speechRecognizer.stopContinuousRecognitionAsync();
            setOnTaskCompletedListener(task, result -> {
                continuousListeningStarted = false;
                Log.d(LOG_TAG, "Stopped Speech to Text");
            });
        } else {
            continuousListeningStarted = false;
            Log.d(LOG_TAG, "Stopped Speech to Text");
        }
    }

    void stopSpeechToTextAndReleaseMicrophone() {
        stopSpeechToText();
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
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
