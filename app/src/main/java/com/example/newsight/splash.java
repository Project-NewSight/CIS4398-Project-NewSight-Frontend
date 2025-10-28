package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Navigate to CameraActivity on the MAIN thread after 3s, then close splash
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(splash.this, CameraActivity.class));
            finish(); // prevent returning to splash on back press
        }, 3000);
    }
}
