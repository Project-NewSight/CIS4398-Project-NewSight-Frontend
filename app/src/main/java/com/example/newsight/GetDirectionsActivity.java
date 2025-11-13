package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class GetDirectionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_directions);

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

        FrameLayout btnSelectNavigation = findViewById(R.id.btnSelectNavigation);
        btnSelectNavigation.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectDestinationActivity.class);
            startActivity(intent);
        });

        FrameLayout btnStartNavigation = findViewById(R.id.btnStartNavigation);
        btnStartNavigation.setOnClickListener(v -> {
            Intent intent = new Intent(this, StartNavigationActivity.class);
            startActivity(intent);
        });

        FrameLayout btnStopNavigation = findViewById(R.id.btnStopNavigation);
        btnStopNavigation.setOnClickListener(v -> {
            Intent intent = new Intent(this, StopNavigationActivity.class);
            startActivity(intent);
        });

        //start camera when clicking on get directions
        FrameLayout btnDetectPeople = findViewById(R.id.btnStartNavigation);
        btnDetectPeople.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("feature", "start_navigation");
            startActivity(intent);
        });
    }
}
