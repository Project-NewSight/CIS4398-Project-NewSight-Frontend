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

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish(); // Go back to previous screen (Home)
        });

        FrameLayout btnUserProfile = findViewById(R.id.btnUserProfile);
        btnUserProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            startActivity(intent);
        });

        FrameLayout btnTrustedContacts = findViewById(R.id.btnTrustedContacts);
        btnTrustedContacts.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrustedContactsActivity.class);
            startActivity(intent);
        });

        FrameLayout btnPrivacyData = findViewById(R.id.btnPrivacyData);
        btnPrivacyData.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrivacyAndDataActivity.class);
            startActivity(intent);
        });

        FrameLayout btnHelpSupport = findViewById(R.id.btnHelpSupport);
        btnHelpSupport.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpAndSupportActivity.class);
            startActivity(intent);
        });

        FrameLayout btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            startActivity(intent);
        });
    }
}
