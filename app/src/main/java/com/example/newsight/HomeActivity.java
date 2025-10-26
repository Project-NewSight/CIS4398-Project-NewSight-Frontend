package com.example.newsight;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Navigate
        FrameLayout btnNavigate = findViewById(R.id.btnNavigate);
        btnNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NavigateActivity.class);
            startActivity(intent);
        });

        // Observe
        FrameLayout btnObserve = findViewById(R.id.btnObserve);
        btnObserve.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ObserveActivity.class);
            startActivity(intent);
        });

        // Communicate
        FrameLayout btnCommunicate = findViewById(R.id.btnCommunicate);
        btnCommunicate.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CommunicateActivity.class);
            startActivity(intent);
        });

        // Emergency
        FrameLayout btnEmergency = findViewById(R.id.btnEmergency);
        btnEmergency.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, EmergencyActivity.class);
            startActivity(intent);
        });

        // Settings
        FrameLayout btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Read and Ask
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
        });
    }
}
