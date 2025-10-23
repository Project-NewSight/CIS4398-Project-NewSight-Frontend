package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class ObserveActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observe);

        // 🔝 Home Button
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        // 🎤 Mic Button
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
            startActivity(intent);
        });

        // 🟦 Read Text
        FrameLayout btnReadText = findViewById(R.id.btnReadText);
        btnReadText.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReadTextActivity.class);
            startActivity(intent);
        });

        // 🟣 Detect People
        FrameLayout btnDetectPeople = findViewById(R.id.btnDetectPeople);
        btnDetectPeople.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetectPeopleActivity.class);
            startActivity(intent);
        });

        // 🔴 Obstacle Scan
        FrameLayout btnObstacleScan = findViewById(R.id.btnObstacleScan);
        btnObstacleScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, ObstacleScanActivity.class);
            startActivity(intent);
        });

        // 🟢 Identify Object
        FrameLayout btnIdentifyObject = findViewById(R.id.btnIdentifyObject);
        btnIdentifyObject.setOnClickListener(v -> {
            Intent intent = new Intent(this, IdentifyObjectActivity.class);
            startActivity(intent);
        });

        // 🟨 Start / Stop Observing
        FrameLayout btnStartStopObserving = findViewById(R.id.btnStartStopObserving);
        btnStartStopObserving.setOnClickListener(v -> {
            Intent intent = new Intent(this, StartStopObservingActivity.class);
            startActivity(intent);
        });
    }
}
