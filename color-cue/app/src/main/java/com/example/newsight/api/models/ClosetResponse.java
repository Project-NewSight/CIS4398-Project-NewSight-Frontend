package com.example.newsight.api.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ClosetResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("count")
    public int count;

    @SerializedName("items")
    public List<ClothingItem> items;

    @SerializedName("TTS_Output")
    public TTSOutput tts_output;

    public static class TTSOutput {
        @SerializedName("message")
        public String message;
    }
}

