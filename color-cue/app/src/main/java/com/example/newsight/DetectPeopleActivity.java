package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class DetectPeopleActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_people);

        Button openCam = findViewById(R.id.btnOpenCamera);
        openCam.setOnClickListener(v -> {
            Intent i = new Intent(this, CameraActivity.class);
            i.putExtra("feature", "familiar_face"); // <-- the key your CameraActivity reads
            startActivity(i);
        });
    }
}
