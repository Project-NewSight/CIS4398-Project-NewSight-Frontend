package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class ColorCueActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colorcue);

        // 🟨 Home Button → Back to Home Screen
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        // 🎤 Mic Button → Voice Command Activity
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
            startActivity(intent);
        });

        // 🔵 Match Outfit Button → MatchOutfitActivity
        FrameLayout btnMatchOutfit = findViewById(R.id.btnMatchOutfit);
        btnMatchOutfit.setOnClickListener(v -> {
            Intent intent = new Intent(this, MatchOutfitActivity.class);
            startActivity(intent);
        });

        // 🟣 Catalog Item Button → CatalogItemActivity
        FrameLayout btnCatalogItem = findViewById(R.id.btnCatalogItem);
        btnCatalogItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, CatalogItemActivity.class);
            startActivity(intent);
        });

        // 🟡 Identify Item Button → IdentifyItemActivity
        FrameLayout btnIdentifyItem = findViewById(R.id.btnIdentifyItem);
        btnIdentifyItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, IdentifyClothesActivity.class);
            startActivity(intent);
        });
    }
}
