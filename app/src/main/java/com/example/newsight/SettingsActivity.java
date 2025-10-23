package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Home
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

        // User Profile
        FrameLayout btnUserProfile = findViewById(R.id.btnUserProfile);
        btnUserProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            startActivity(intent);
        });

        // Trusted Contacts
        FrameLayout btnTrustedContacts = findViewById(R.id.btnTrustedContacts);
        btnTrustedContacts.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrustedContactsActivity.class);
            startActivity(intent);
        });

        // Voice and Haptics
        FrameLayout btnVoiceHaptics = findViewById(R.id.btnVoiceHaptics);
        btnVoiceHaptics.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceAndHapticsActivity.class);
            startActivity(intent);
        });

        // Privacy and Data
        FrameLayout btnPrivacyData = findViewById(R.id.btnPrivacyData);
        btnPrivacyData.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrivacyAndDataActivity.class);
            startActivity(intent);
        });

        // Emergency Setup
        FrameLayout btnEmergencySetup = findViewById(R.id.btnEmergencySetup);
        btnEmergencySetup.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmergencySetupActivity.class);
            startActivity(intent);
        });

        // Help and Support
        FrameLayout btnHelpSupport = findViewById(R.id.btnHelpSupport);
        btnHelpSupport.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpAndSupportActivity.class);
            startActivity(intent);
        });

        // Log Out
        FrameLayout btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            startActivity(intent);
        });
    }
}
