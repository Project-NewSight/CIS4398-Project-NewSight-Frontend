    package com.example.newsight;

    import android.os.Bundle;
    import android.widget.FrameLayout;
    import android.content.Intent;
    import androidx.appcompat.app.AppCompatActivity;

    public class DetectionActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_detect);

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

            FrameLayout btnReadText = findViewById(R.id.btnReadText);
            btnReadText.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReadTextActivity.class);
                intent.putExtra("feature", "read_text");
                startActivity(intent);
            });

            FrameLayout btnIdentifyObject = findViewById(R.id.btnIdentifyObject);
            btnIdentifyObject.setOnClickListener(v -> {
                Intent intent = new Intent(this, IdentifyObjectActivity.class);
                startActivity(intent);
            });

            FrameLayout btnObstacleScan = findViewById(R.id.btnObstacleScan);
            btnObstacleScan.setOnClickListener(v -> {
                Intent intent = new Intent(this, ObstacleActivity.class);
                startActivity(intent);
            });

            FrameLayout btnDetectPeople = findViewById(R.id.btnDetectPeople);
            btnDetectPeople.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("feature", "detect_people");
                startActivity(intent);
            });
        }
    }
