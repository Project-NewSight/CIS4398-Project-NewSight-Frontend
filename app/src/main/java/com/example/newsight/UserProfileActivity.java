package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class UserProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // 🟦 Home → HomeActivity
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        // 🎤 Mic → VoiceCommandActivity
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
            startActivity(intent);
        });

        // ✏️ Edit Profile → EditProfileActivity
        FrameLayout btnEditProfile = findViewById(R.id.btnEditProfile);
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            startActivity(intent);
        });
    }
}
