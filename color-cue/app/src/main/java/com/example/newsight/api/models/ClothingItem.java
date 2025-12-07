package com.example.newsight.api.models;

import com.google.gson.annotations.SerializedName;

public class ClothingItem {

    @SerializedName("item_id")
    public int item_id;

    @SerializedName("closet_id")
    public int closet_id;

    @SerializedName("category")
    public String category;

    @SerializedName("category_raw_yolo")
    public String category_raw_yolo;

    @SerializedName("category_raw_classifier")
    public String category_raw_classifier;

    @SerializedName("category_unified")
    public String category_unified;

    @SerializedName("category_model_used")
    public String category_model_used;

    @SerializedName("category_confidence")
    public float category_confidence;

    @SerializedName("color")
    public String color;

    @SerializedName("pattern")
    public String pattern;

    @SerializedName("printed_text")
    public String printed_text;

    @SerializedName("material")
    public String material;

    @SerializedName("style")
    public String style;

    @SerializedName("genre")
    public String genre;

    @SerializedName("washing_instructions")
    public String washing_instructions;

    @SerializedName("notes")
    public String notes;

    @SerializedName("image_url")
    public String image_url;

    @SerializedName("tag_front_url")
    public String tag_front_url;

    @SerializedName("tag_back_url")
    public String tag_back_url;

    @SerializedName("embedding_str")
    public String embedding_str;

    @SerializedName("primary_color")
    public String primary_color;

    @SerializedName("secondary_colors")
    public String secondary_colors;

    @SerializedName("is_multicolor")
    public boolean is_multicolor;

    @SerializedName("pattern_probability")
    public float pattern_probability;

    @SerializedName("dominant_colors_json")
    public String dominant_colors_json;
}
