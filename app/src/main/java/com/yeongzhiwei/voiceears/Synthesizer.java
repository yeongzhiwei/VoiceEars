package com.yeongzhiwei.voiceears;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;


class Synthesizer {
    private static final String LOG_TAG = Synthesizer.class.getName();

    private TtsServiceClient ttsServiceClient;
    private Voice voice;
    private AudioTrack audioTrack;

    Synthesizer(String apiKey) {
        this(apiKey, Voice.getDefaultVoice(Voice.Gender.Male));
    }

    Synthesizer(String apiKey, Voice voice) {
        this.ttsServiceClient = new TtsServiceClient(apiKey);
        this.voice = voice;
    }

    Voice getVoice() {
        return this.voice;
    }

    void speakToAudio(String text, Runnable callOnPlay, Runnable callOnStop) {
        playSound(speak(text), callOnPlay, callOnStop);
    }

    private byte[] speak(String text) {
        String ssml = "<speak version='1.0' xml:lang='" + voice.getLang()
                + "'><voice xml:lang='" + voice.getLang()
                + "' xml:gender='" + voice.getGender()
                + "' name='" + voice.getVoiceName() + "'>"
                + text + "</voice></speak>";
        Log.d(LOG_TAG, "Sending ssml: " + ssml);

        return speakSSML(ssml);
    }

    void speakSSMLToAudio(String ssml, Runnable callOnPlay, Runnable callOnStop) {
        playSound(speakSSML(ssml), callOnPlay, callOnStop);
    }

    private byte[] speakSSML(String ssml) {
        byte[] result;
        result = ttsServiceClient.speakSSML(ssml);
        if (result == null || result.length == 0) {
            return null;
        }
        return result;
    }

    private void playSound(final byte[] sound, final Runnable callOnPlay, Runnable callOnStop) {
        if (sound == null || sound.length == 0){
            Log.d(LOG_TAG, "playSound: no sound!");
            return;
        }

        AsyncTask.execute(() -> {
            if (callOnPlay != null) {
                callOnPlay.run();
            }

            final int SAMPLE_RATE = 16000;

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);

            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.play();
                audioTrack.write(sound, 0, sound.length);
                audioTrack.stop();
                audioTrack.release();
            }

            if (callOnStop != null) {
                callOnStop.run();
            }
        });
    }

    //stop playing audio data
    // if use STREAM mode, will wait for the end of the last write buffer data will stop.
    // if you stop immediately, call the pause() method and then call the flush() method to discard the data that has not yet been played
    void stopSound() {
        try {
            if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.pause();
                audioTrack.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
