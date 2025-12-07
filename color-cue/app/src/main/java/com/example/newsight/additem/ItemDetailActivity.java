package com.example.newsight.additem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.newsight.R;
import com.example.newsight.ViewClosetActivity;

public class ItemDetailActivity extends AppCompatActivity {

    FrameLayout btnBack, btnHome;
    ImageView imgItem;
    TextView txtDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        btnBack = findViewById(R.id.btnBack);
        btnHome = findViewById(R.id.btnHome);
        imgItem = findViewById(R.id.imgItemDetail);
        txtDetails = findViewById(R.id.txtItemDetails);

        // ----- Get data from Intent -----
        String imageUrl   = getIntent().getStringExtra("image_url");
        String color      = getIntent().getStringExtra("color");
        String category   = getIntent().getStringExtra("category");
        String material   = getIntent().getStringExtra("material");
        String washing    = getIntent().getStringExtra("washing_instructions");
        String printed    = getIntent().getStringExtra("printed_text");
        String pattern    = getIntent().getStringExtra("pattern");
        String style      = getIntent().getStringExtra("style");
        String genre      = getIntent().getStringExtra("genre");
        String notes      = getIntent().getStringExtra("notes");

        // ----- Load image -----
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(imgItem);
        }

        // ----- Build details string (exact order requested) -----
        StringBuilder sb = new StringBuilder();
        sb.append("Item Details\n\n");

        sb.append("Color: ").append(safe(color)).append("\n");
        sb.append("Category: ").append(safe(category)).append("\n");
        sb.append("Material: ").append(safe(material)).append("\n");
        sb.append("Washing Instructions: ").append(safe(washing)).append("\n");
        sb.append("Printed Text: ").append(safe(printed)).append("\n");
        sb.append("Pattern: ").append(safe(pattern)).append("\n");
        sb.append("Style: ").append(safe(style)).append("\n");
        sb.append("Genre: ").append(safe(genre)).append("\n");
        sb.append("Notes: ").append(safe(notes)).append("\n");

        txtDetails.setText(sb.toString());

        // ----- Buttons -----
        btnBack.setOnClickListener(v -> finish());

        btnHome.setOnClickListener(v -> {
            Intent go = new Intent(ItemDetailActivity.this, ViewClosetActivity.class);
            startActivity(go);
            finish();
        });
    }

    private String safe(String text) {
        return (text == null || text.trim().isEmpty()) ? "---" : text;
    }
}
