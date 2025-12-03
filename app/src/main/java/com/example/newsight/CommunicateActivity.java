package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class CommunicateActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate);

        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
            startActivity(intent);
        });

        FrameLayout btnSpeakToText = findViewById(R.id.btnSpeakToText);
        btnSpeakToText.setOnClickListener(v -> {
            Intent intent = new Intent(this, SpeakToTextActivity.class);
            startActivity(intent);
        });

        FrameLayout btnSignTranslation = findViewById(R.id.btnSignTranslation);
        btnSignTranslation.setOnClickListener(v -> {
            Intent intent = new Intent(CommunicateActivity.this, CameraActivity.class);
            intent.putExtra("feature", "asl_detection");  // Tell backend to enable ASL model
            startActivity(intent);
        });

    }
}
