package com.yeongzhiwei.voiceears;

import android.os.Handler;
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
    private Counter counter = new Counter();

    private Handler handler = new Handler();
    private int handlerDelay = 3000; // 1 second

    private static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    private RecognizerUI recognizerUI;

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
            content.clear();
            counter.increment();

            AudioConfig audioConfig = AudioConfig.fromStreamInput(createMicrophoneStream());
            speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);

            speechRecognizer.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                content.add(s);
                String message = TextUtils.join(" ", content).trim();
                if (message.length() != 0) {
                    resetRepeatingTask();
                    recognizerUI.update(counter.get(), message);
                }
                content.remove(content.size() - 1);
            });

            speechRecognizer.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                Log.d(LOG_TAG, s);
                content.add(s);
                String message = TextUtils.join(" ", content).trim();
                if (message.length() != 0) { // && continuousListeningStarted
                    resetRepeatingTask();
                    recognizerUI.update(counter.get(), message);
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
        stopRepeatingTask();
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

    public interface RecognizerUI {
        void update(Integer order, String result);
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

    private Runnable resetContent = new Runnable() {
        @Override
        public void run() {
            try {
                content.clear();
                counter.increment();
            } finally {
                handler.postDelayed(resetContent, handlerDelay);
            }
        }
    };

    private void startRepeatingTask() {
        resetContent.run();
    }
    private void resetRepeatingTask() {
        handler.removeCallbacks(resetContent);
        handler.postDelayed(resetContent, handlerDelay);
    }

    private void stopRepeatingTask() {
        handler.removeCallbacks(resetContent);
    }
}
