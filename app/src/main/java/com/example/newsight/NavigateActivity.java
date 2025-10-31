package com.example.newsight;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import android.widget.Toast;

public class NavigateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        // HOME BUTTON → Go back to main home screen
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        //  MIC BUTTON → Placeholder for voice activation
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Toast.makeText(this, "🎤 Voice command activated", Toast.LENGTH_SHORT).show();
        });

        //  FIND MY LOCATION → Launch location identification
        FrameLayout btnFindLocation = findViewById(R.id.btnFindLocation);
        btnFindLocation.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, FindLocationActivity.class);
            startActivity(intent);
        });

        //  OBSTACLE DETECTION → Launch obstacle detection interface
        FrameLayout btnObstacleDetection = findViewById(R.id.btnObstacleDetection);
        btnObstacleDetection.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, ObstacleActivity.class);
            startActivity(intent);
        });

        //  GET DIRECTIONS → Launch direction submenu
        FrameLayout btnGetDirections = findViewById(R.id.btnGetDirections);
        btnGetDirections.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, GetDirectionsActivity.class);
            startActivity(intent);
        });
    }
}
