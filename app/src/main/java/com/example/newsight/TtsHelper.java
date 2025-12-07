package com.example.newsight;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class TtsHelper {

    private TextToSpeech tts;
    private boolean isReady = false;

    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                isReady = true;
            }
        });
    }

    public void speak(String text) {
        // Check if TextToSpeech is ready and text is not null or empty
        if (!isReady || text == null || text.trim().isEmpty()) return;

        text = text.trim();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID");
    }

    /**
     * Parse a JSON string and speak only the TTS_Output.message field.
     * Example JSON:
     * {
     * "confidence": 0.98,
     * "extracted_params": { ... },
     * "TTS_Output": { "message": "Processing your request" }
     * }
     */
    public void speakFromJson(String jsonString) {
        // Check if TextToSpeech is ready and jsonString is not null
        if (!isReady || jsonString == null) return;

        try {
            JSONObject root = new JSONObject(jsonString);

            // Only care about: TTS_Output.message
            JSONObject ttsOutput = root.optJSONObject("TTS_Output");
            if (ttsOutput == null) return;

            String message = ttsOutput.optString("message", "").trim();
            if (message.isEmpty()) return;

            speak(message);

        } catch (JSONException e) {
            // You can log this if you want; we silently ignore malformed JSON for now
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
