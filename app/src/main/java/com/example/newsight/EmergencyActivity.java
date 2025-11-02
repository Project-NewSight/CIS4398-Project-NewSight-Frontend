package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class EmergencyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        //Home
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        // Mic
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
            startActivity(intent);
        });

        // Ping Location
        FrameLayout btnPingLocation = findViewById(R.id.btnPingLocation);
        btnPingLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, FindLocationActivity.class);
            startActivity(intent);
        });

        // Send Text Alert
        FrameLayout btnSendAlert = findViewById(R.id.btnSendAlert);
        btnSendAlert.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendTextAlertActivity.class);
            startActivity(intent);
        });

        // Open Camera (new)
        FrameLayout btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnOpenCamera.setOnClickListener(v -> {
            // Launch MainActivity and open camera directly
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("feature", "emergency"); // signal MainActivity to open camera immediately
            startActivity(intent);
        });
    }
}
