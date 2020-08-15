package com.yeongzhiwei.voiceears.ttsstt;

import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.util.EventHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Recognizer {

    private static final String LOG_TAG = Recognizer.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private SpeechConfig speechConfig;
    private SpeechRecognizer speechRecognizer;
    private MicrophoneStream microphoneStream;

    private EventHandler<SpeechRecognitionEventArgs> recognizingEventHandler;
    private EventHandler<SpeechRecognitionEventArgs> recognizedEventHandler;

    private boolean continuousRecognitionStarted = false;

    public Recognizer(@NonNull String cognitiveServicesApiKey, @NonNull String cognitiveServicesRegion, @NonNull Recognition recognition) {
        this.speechConfig = SpeechConfig.fromSubscription(cognitiveServicesApiKey, cognitiveServicesRegion);

        this.recognizingEventHandler = (o, speechRecognitionEventArgs) -> {
            final String text = speechRecognitionEventArgs.getResult().getText();
            Log.d(LOG_TAG, "recognizing: " + text);

            if (!text.isEmpty()) {
                recognition.recognizing(text);
            }
        };

        this.recognizedEventHandler = (o, speechRecognitionEventArgs) -> {
            final String text = speechRecognitionEventArgs.getResult().getText();
            Log.d(LOG_TAG, "recognized: " + text);

            if (!text.isEmpty()) {
                recognition.recognized(text);
            }
        };
    }

    synchronized public void startSpeechToText() {
        if (continuousRecognitionStarted) {
            return;
        }

        microphoneStream = new MicrophoneStream();
        AudioConfig audioConfig = AudioConfig.fromStreamInput(microphoneStream);
        speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);
        speechRecognizer.recognizing.addEventListener(recognizingEventHandler);
        speechRecognizer.recognized.addEventListener(recognizedEventHandler);

        final Future<Void> task = speechRecognizer.startContinuousRecognitionAsync();
        setOnTaskCompletedListener(task, result -> {
            continuousRecognitionStarted = true;
            Log.d(LOG_TAG, "Started Speech to Text");
        });
    }

    synchronized public void stopSpeechToText() {
        if (speechRecognizer != null) {
            final Future<Void> task = speechRecognizer.stopContinuousRecognitionAsync();
            setOnTaskCompletedListener(task, result -> {
                continuousRecognitionStarted = false;
                Log.d(LOG_TAG, "Stopped Speech to Text");
            });
        } else {
            continuousRecognitionStarted = false;
            Log.d(LOG_TAG, "Speech to Text was not started");
        }

        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
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
