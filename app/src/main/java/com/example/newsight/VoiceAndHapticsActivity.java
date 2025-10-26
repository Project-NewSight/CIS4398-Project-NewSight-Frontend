package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class VoiceAndHapticsActivity extends AppCompatActivity {

    private boolean voiceEnabled = true;
    private boolean hapticsEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_and_haptics);

        // Home
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        // 🎤 Mic
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
            startActivity(intent);
        });

        // Voice Toggle
        FrameLayout btnVoiceToggle = findViewById(R.id.btnVoiceToggle);
        TextView txtVoiceToggle = btnVoiceToggle.findViewById(android.R.id.text1);
        btnVoiceToggle.setOnClickListener(v -> {
            voiceEnabled = !voiceEnabled;
            ((TextView) ((FrameLayout) v).getChildAt(0))
                    .setText("Voice Feedback: " + (voiceEnabled ? "ON" : "OFF"));
        });

        // Haptic Toggle
        FrameLayout btnHapticToggle = findViewById(R.id.btnHapticToggle);
        btnHapticToggle.setOnClickListener(v -> {
            hapticsEnabled = !hapticsEnabled;
            ((TextView) ((FrameLayout) v).getChildAt(0))
                    .setText("Haptic Feedback: " + (hapticsEnabled ? "ON" : "OFF"));
        });
    }
}
