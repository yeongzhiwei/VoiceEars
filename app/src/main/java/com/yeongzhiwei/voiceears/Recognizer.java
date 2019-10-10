package com.yeongzhiwei.voiceears;

import android.util.Log;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class Recognizer {
    private static final String LOG_TAG = Recognizer.class.getSimpleName();
    private static ExecutorService s_executorService = Executors.newCachedThreadPool();

    interface Recognition {
        void recognize(String text, Boolean isFinal);
    }

    private SpeechConfig speechConfig;
    private Recognition recognition;

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

    Recognizer(String cognitiveServicesApiKey, String cognitiveServicesRegion, Recognition recognition) {
        speechConfig = SpeechConfig.fromSubscription(cognitiveServicesApiKey, cognitiveServicesRegion);

        this.recognition = recognition;
    }

    // Adapted from Sample code for the Microsoft Cognitive Services Speech SDK
    // https://github.com/Azure-Samples/cognitive-services-speech-sdk
    synchronized void startSpeechToText() {
        if (continuousListeningStarted) {
            return;
        }

            AudioConfig audioConfig = AudioConfig.fromStreamInput(createMicrophoneStream());
            speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);

            speechRecognizer.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
                Log.d(LOG_TAG, "recognizing: " + s);

                if (!s.isEmpty()) {
                    recognition.recognize(s, false);
                }

            });

            speechRecognizer.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                final String s = speechRecognitionResultEventArgs.getResult().getText();
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

    synchronized void stopSpeechToText() {
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
