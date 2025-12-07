package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.additem.AddItemFrontActivity;

public class ColorCueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colorcue);

        FrameLayout btnBack = findViewById(R.id.btnBack);
        FrameLayout btnHome = findViewById(R.id.btnHome);
        FrameLayout btnMatchOutfit = findViewById(R.id.btnMatchOutfit);
        FrameLayout btnAddItem = findViewById(R.id.btnAddItem);
        FrameLayout btnIdentifyItem = findViewById(R.id.btnIdentifyItem);
        FrameLayout btnViewCloset = findViewById(R.id.btnViewCloset);
        FrameLayout btnMic = findViewById(R.id.btnMic);

        btnBack.setOnClickListener(v -> finish());
        btnHome.setOnClickListener(v -> finish());

        btnMatchOutfit.setOnClickListener(v ->
                startActivity(new Intent(this, MatchOutfitActivity.class)));

        btnAddItem.setOnClickListener(v ->
                startActivity(new Intent(this, AddItemFrontActivity.class)));

        btnIdentifyItem.setOnClickListener(v ->
                startActivity(new Intent(this, IdentifyItemActivity.class)));

        btnViewCloset.setOnClickListener(v ->
                startActivity(new Intent(this, ViewClosetActivity.class))); // if needed

        btnMic.setOnClickListener(v ->
                startActivity(new Intent(this, VoiceCommandActivity.class)));
    }
}
