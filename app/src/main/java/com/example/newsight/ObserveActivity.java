package com.example.newsight;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

public class ObserveActivity extends AppCompatActivity {
    private static final String TAG = "ObserveActivity";
    private VoiceCommandHelper voiceCommandHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observe);

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

        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            }
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

        // Start wake word detection if permission is granted
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
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