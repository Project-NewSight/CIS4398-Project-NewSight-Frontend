package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.example.newsight.R;


public class ObserveActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observe);

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

        FrameLayout btnDetection = findViewById(R.id.btnDetection);
        btnDetection.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetectionActivity.class);
            startActivity(intent);
        });

        FrameLayout btnFashion = findViewById(R.id.btnFashion);
        btnFashion.setOnClickListener(v -> {
            Intent intent = new Intent(this, ColorCueActivity.class);
            startActivity(intent);
        });
    }
}
