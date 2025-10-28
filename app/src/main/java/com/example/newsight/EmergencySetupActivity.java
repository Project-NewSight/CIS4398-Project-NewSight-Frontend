package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class EmergencySetupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_setup);

        //  Home
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        //  Mic
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
            startActivity(intent);
        });

        //  Set Emergency Contacts
        FrameLayout btnSetContacts = findViewById(R.id.btnSetContacts);
        btnSetContacts.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrustedContactsActivity.class);
            startActivity(intent);
        });

        //  Configure GPS Ping
        FrameLayout btnConfigureGPS = findViewById(R.id.btnConfigureGPS);
        btnConfigureGPS.setOnClickListener(v -> {
            Intent intent = new Intent(this, GPSSetupActivity.class);
            startActivity(intent);
        });

        //  Test Alert
        FrameLayout btnTestAlert = findViewById(R.id.btnTestAlert);
        btnTestAlert.setOnClickListener(v -> {
            Intent intent = new Intent(this, TestAlertActivity.class);
            startActivity(intent);
        });

        //  Full Emergency Simulation
        FrameLayout btnFullTest = findViewById(R.id.btnFullTest);
        btnFullTest.setOnClickListener(v -> {
            Intent intent = new Intent(this, FullEmergencySimActivity.class);
            startActivity(intent);
        });
    }
}
