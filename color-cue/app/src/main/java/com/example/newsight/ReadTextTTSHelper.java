package com.example.newsight;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

/**
 * Helper class for Text-to-Speech functionality
 * Manages TTS initialization and provides methods to speak text aloud
 */
public class ReadTextTTSHelper {
    private static final String TAG = "ReadTextTTSHelper";
    private TextToSpeech tts;
    private boolean isReady = false;
    private Context context;
    private TTSListener listener;

    public interface TTSListener {
        void onTTSReady();
        void onTTSError(String error);
        void onSpeechStart();
        void onSpeechComplete();
    }

    public ReadTextTTSHelper(Context context, TTSListener listener) {
        this.context = context;
        this.listener = listener;
        initializeTTS();
    }

    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                    isReady = false;
                    if (listener != null) {
                        listener.onTTSError("Language not supported");
                    }
                } else {
                    isReady = true;
                    Log.d(TAG, "TTS initialized successfully");

                    // Set speech parameters
                    tts.setSpeechRate(1.0f);
                    tts.setPitch(1.0f);

                    // Set up progress listener
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            if (listener != null) {
                                listener.onSpeechStart();
                            }
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            if (listener != null) {
                                listener.onSpeechComplete();
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "TTS error for utterance: " + utteranceId);
                            if (listener != null) {
                                listener.onTTSError("Speech synthesis error");
                            }
                        }
                    });

                    if (listener != null) {
                        listener.onTTSReady();
                    }
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
                isReady = false;
                if (listener != null) {
                    listener.onTTSError("TTS initialization failed");
                }
            }
        });
    }

    /**
     * Speak text aloud
     * @param text The text to speak
     */
    public void speak(String text) {
        if (!isReady) {
            Log.w(TAG, "TTS not ready yet");
            if (listener != null) {
                listener.onTTSError("TTS not ready");
            }
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Empty text provided");
            return;
        }

        // Stop any ongoing speech first
        stop();

        // Create parameters for this utterance
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "textDetection_" + System.currentTimeMillis());

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        Log.d(TAG, "Speaking: " + text);
    }

    /**
     * Stop current speech
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * Check if TTS is currently speaking
     */
    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    /**
     * Set speech rate
     * @param rate Speech rate (0.5 to 2.0, 1.0 is normal)
     */
    public void setSpeechRate(float rate) {
        if (tts != null) {
            tts.setSpeechRate(rate);
        }
    }

    /**
     * Set speech pitch
     * @param pitch Speech pitch (0.5 to 2.0, 1.0 is normal)
     */
    public void setPitch(float pitch) {
        if (tts != null) {
            tts.setPitch(pitch);
        }
    }

    /**
     * Check if TTS is ready to use
     */
    public boolean isReady() {
        return isReady;
    }

    /**
     * Clean up TTS resources
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isReady = false;
            Log.d(TAG, "TTS shutdown");
        }
    }
}
