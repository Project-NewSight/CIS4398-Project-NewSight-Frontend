package com.example.newsight.api.models;

import com.google.gson.annotations.SerializedName;

public class IdentifyResponse {

    @SerializedName("closest_item")
    public ClosestItem closest_item;

    @SerializedName("similarity")
    public float similarity;

    @SerializedName("detected_item")
    public DetectedItem detected_item;

    // ---- NESTED DATA MODELS ----

    public static class ClosestItem {
        @SerializedName("id")
        public int id;

        @SerializedName("color")
        public String color;

        @SerializedName("category")
        public String category;

        @SerializedName("pattern")
        public String pattern;

        @SerializedName("genre")
        public String genre;

        @SerializedName("notes")
        public String notes;

        @SerializedName("image_url")
        public String image_url;
    }

    public static class DetectedItem {
        @SerializedName("color")
        public String color;

        @SerializedName("category")
        public String category;

        @SerializedName("pattern")
        public String pattern;

        @SerializedName("printed_text")
        public String printed_text;
    }
}
