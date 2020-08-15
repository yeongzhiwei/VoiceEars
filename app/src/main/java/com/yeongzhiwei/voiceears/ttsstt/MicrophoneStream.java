package com.yeongzhiwei.voiceears.ttsstt;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;

/**
 * MicrophoneStream exposes the Android Microphone as an PullAudioInputStreamCallback
 * to be consumed by the Speech SDK.
 * It configures the microphone with 16 kHz sample rate, 16 bit samples, mono (single-channel).
 * https://github.com/Azure-Samples/cognitive-services-speech-sdk/blob/master/samples/java/android/sdkdemo/app/src/main/java/com/microsoft/cognitiveservices/speech/samples/sdkdemo/MicrophoneStream.java
 */
class MicrophoneStream extends PullAudioInputStreamCallback {

    private static final String LOG_TAG = MicrophoneStream.class.getSimpleName();

    private final static int SAMPLE_RATE = 16000;
    private AudioRecord audioRecord;

    MicrophoneStream() {
        this.initMic();
    }

    @Override
    public int read(byte[] bytes) {
        long ret = this.audioRecord.read(bytes, 0, bytes.length);
        return (int)ret;
    }

    @Override
    public void close() {
        this.audioRecord.release();
        this.audioRecord = null;
        Log.d(LOG_TAG, "Released microphone");
    }

    private void initMic() {
        // Note: currently, the Speech SDK support 16 kHz sample rate, 16 bit samples, mono (single-channel) only.
        AudioFormat af = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build();
        this.audioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(af)
                .build();

        this.audioRecord.startRecording();
        Log.d(LOG_TAG, "Started listening on microphone");
    }

}