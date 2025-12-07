package com.example.newsight.api.models;

public class MatchResponse {

    public boolean success;

    // true / false if the two items match
    public boolean match;

    // top item details
    public ClothingItem top_item;

    // bottom item details
    public ClothingItem bottom_item;

    // text explanation (if backend sends one)
    public String explanation;

    // TTS output (voice)
    public TTSOutput TTS_Output;

    public static class TTSOutput {
        public String message;
    }
}
