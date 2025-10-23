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

        // 🟨 Home Button → Go back to HomeActivity
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        // 🎤 Mic Button → Open VoiceCommandActivity
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
            startActivity(intent);
        });

        // 🔵 Speak to Text → Open SpeakToTextActivity
        FrameLayout btnSpeakToText = findViewById(R.id.btnSpeakToText);
        btnSpeakToText.setOnClickListener(v -> {
            Intent intent = new Intent(this, SpeakToTextActivity.class);
            startActivity(intent);
        });

        // 🟢 Translate Sign Language → Open SignTranslationActivity
        FrameLayout btnSignTranslation = findViewById(R.id.btnSignTranslation);
        btnSignTranslation.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignTranslationActivity.class);
            startActivity(intent);
        });

        // 🟨 Start/Stop Communicating → Open CommToggleActivity (or similar)
        FrameLayout btnStartStopComm = findViewById(R.id.btnStartStopComm);
        btnStartStopComm.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommToggleActivity.class);
            startActivity(intent);
        });
    }
}
