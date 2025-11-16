package com.example.newsight;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.content.Intent;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private VoiceCommandHelper voiceCommandHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize voice command helper
        voiceCommandHelper = new VoiceCommandHelper(this);

        // Set up voice command callbacks
        voiceCommandHelper.setCallback(new VoiceCommandHelper.VoiceCommandCallback() {
            @Override
            public void onWakeWordDetected() {
                Log.d(TAG, "Wake word detected");
            }

            @Override
            public void onCommandStarted() {
                Log.d(TAG, "Command recording started");
            }

            @Override
            public void onCommandProcessing() {
                Log.d(TAG, "Processing command");
            }

            @Override
            public void onResponseReceived(String jsonResponse) {
                Log.d(TAG, "Response received: " + jsonResponse);
                // TODO: Pass jsonResponse to TTS helper class when ready
                // ttsHelper.handleResponse(jsonResponse);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "Voice command completed");
            }
        });

        // Navigate
        FrameLayout btnNavigate = findViewById(R.id.btnNavigate);
        btnNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NavigateActivity.class);
            startActivity(intent);
        });

        // Observe
        FrameLayout btnObserve = findViewById(R.id.btnObserve);
        btnObserve.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ObserveActivity.class);
            startActivity(intent);
        });

        // Communicate
        FrameLayout btnCommunicate = findViewById(R.id.btnCommunicate);
        btnCommunicate.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CommunicateActivity.class);
            startActivity(intent);
        });

        // Emergency
        FrameLayout btnEmergency = findViewById(R.id.btnEmergency);
        btnEmergency.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, EmergencyActivity.class);
            startActivity(intent);
        });

        // Settings
        FrameLayout btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Read and Ask - Mic button
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            }
        });

        // Request microphone permission on startup
        requestMicrophonePermissionOnStartup();
    }

    private void requestMicrophonePermissionOnStartup() {
        if (!checkMicrophonePermission()) {
            // Request permission immediately when app starts
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, start wake word detection
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Voice commands ready", Toast.LENGTH_SHORT).show();
                // Start wake word detection after permission granted
                voiceCommandHelper.startWakeWordDetection();
            } else {
                Toast.makeText(this, "Microphone permission is required for voice commands",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (voiceCommandHelper != null) {
            voiceCommandHelper.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (voiceCommandHelper != null && checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceCommandHelper != null) {
            voiceCommandHelper.cleanup();
        }
    }
}