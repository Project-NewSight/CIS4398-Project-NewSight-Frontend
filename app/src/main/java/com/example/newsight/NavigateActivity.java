package com.example.newsight;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.content.Intent;

public class NavigateActivity extends AppCompatActivity {
    private static final String TAG = "NavigateActivity";
    private VoiceCommandHelper voiceCommandHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

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

        // HOME BUTTON → Go back to main home screen
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        // MIC BUTTON → Start voice recording
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            }
        });

        // FIND MY LOCATION → Launch location identification
        FrameLayout btnFindLocation = findViewById(R.id.btnFindLocation);
        btnFindLocation.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, FindLocationActivity.class);
            startActivity(intent);
        });

        // OBSTACLE DETECTION → Launch obstacle detection interface
        FrameLayout btnObstacleDetection = findViewById(R.id.btnObstacleDetection);
        btnObstacleDetection.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, ObstacleActivity.class);
            startActivity(intent);
        });

        // GET DIRECTIONS → Launch direction submenu
        FrameLayout btnGetDirections = findViewById(R.id.btnGetDirections);
        btnGetDirections.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, GetDirectionsActivity.class);
            startActivity(intent);
        });

        // Start wake word detection if permission is granted
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure portrait orientation when returning
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Restart wake word detection
        if (voiceCommandHelper != null && checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
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
    protected void onDestroy() {
        super.onDestroy();
        if (voiceCommandHelper != null) {
            voiceCommandHelper.cleanup();
        }
    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }
}