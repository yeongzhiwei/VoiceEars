package com.yeongzhiwei.voiceears.ttsstt;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;

/**
 * MicrophoneStream exposes the Android Microphone as an PullAudioInputStreamCallback
 * to be consumed by the Speech SDK.
 * It configures the microphone with 16 kHz sample rate, 16 bit samples, mono (single-channel).
 */
public class MicrophoneStream extends PullAudioInputStreamCallback {

    private final static int SAMPLE_RATE = 16000;
    private final AudioStreamFormat audioStreamFormat;
    private AudioRecord audioRecord;

    public MicrophoneStream() {
        this.audioStreamFormat = AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE, (short)16, (short)1);
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
    }
}