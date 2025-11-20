package com.example.newsight;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONObject;

public class NavigateActivity extends AppCompatActivity {

    private static final String TAG = "NavigateActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private TtsHelper ttsHelper;
    private VoiceCommandHelper voiceCommandHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        // Initialize voice command helper
        voiceCommandHelper = new VoiceCommandHelper(this);
        ttsHelper = new TtsHelper(this);

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
            }

            @Override
            public void onNavigateToFeature(String feature, JSONObject extractedParams) {
                Log.d(TAG, "Navigating to feature: " + feature);
                navigateToFeature(feature, extractedParams);
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

        // MIC BUTTON → Voice command activation
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            } else {
                requestMicrophonePermission();
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

        // Auto-start wake word detection when activity starts
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    /**
     * Navigate to the appropriate activity based on the feature name
     */
    private void navigateToFeature(String feature, JSONObject extractedParams) {
        if (feature == null || feature.isEmpty()) {
            Log.w(TAG, "Feature is null or empty, skipping navigation");
            return;
        }

        Intent intent = null;
        String ttsMessage = null;

        // Map feature names to activities
        switch (feature.toUpperCase()) {
            case "NAVIGATION":
                ttsMessage = "Activating navigation";
                intent = null;
                Toast.makeText(this, "Opening Navigation", Toast.LENGTH_SHORT).show();
                break;

            case "OBJECT_DETECTION":
                intent = new Intent(NavigateActivity.this, ObstacleActivity.class);
                ttsMessage = "Activating Object Detection";
                Toast.makeText(this, "Opening Observe", Toast.LENGTH_SHORT).show();
                break;

            case "FACIAL_RECOGNITION":
                intent = new Intent(NavigateActivity.this, MainActivity.class);
                intent.putExtra("feature", "detect_people");
                ttsMessage = "Activating Facial Recognition";
                Toast.makeText(this, "Opening Observe", Toast.LENGTH_SHORT).show();
                break;

            case "TEXT_DETECTION":
                intent = new Intent(NavigateActivity.this, DetectionActivity.class);
                ttsMessage = "Activating Text Detection";
                Toast.makeText(this, "Opening Observe", Toast.LENGTH_SHORT).show();
                break;

            case "COLOR_CUE":
                intent = new Intent(NavigateActivity.this, ColorCueActivity.class);
                ttsMessage = "Activating Color Cue";
                Toast.makeText(this, "Opening Observe", Toast.LENGTH_SHORT).show();
                break;

            case "ASL_DETECTOR":
                intent = new Intent(NavigateActivity.this, CommunicateActivity.class);
                ttsMessage = "Activating ASL Detector";
                Toast.makeText(this, "Opening Communicate", Toast.LENGTH_SHORT).show();
                break;

            case "EMERGENCY_CONTACT":
                intent = new Intent(NavigateActivity.this, EmergencyActivity.class);
                ttsMessage = "Activating Emergency Contact";
                Toast.makeText(this, "Opening Emergency", Toast.LENGTH_SHORT).show();
                break;

            default:
                Log.w(TAG, "Unknown feature: " + feature);
                ttsMessage = "I am sorry, I am not able to detect your feature";
                Toast.makeText(this, "Unknown feature: " + feature, Toast.LENGTH_SHORT).show();
                return;
        }

        // Add extracted parameters as extras if needed
        if (intent != null && extractedParams != null) {
            intent.putExtra("extracted_params", extractedParams.toString());

            // You can also extract specific parameters
            String query = extractedParams.optString("query", null);
            String destination = extractedParams.optString("destination", null);

            if (query != null) {
                intent.putExtra("query", query);
            }
            if (destination != null) {
                intent.putExtra("destination", destination);
            }

            Log.d(TAG, "Starting activity with extras: " + extractedParams.toString());
        }

        // Start the activity
        if (ttsMessage != null) {
            ttsHelper.speak(ttsMessage);

            final Intent finalIntent = intent;

            new android.os.Handler().postDelayed(() -> startActivity(finalIntent), 900);
        } else{
            startActivity(intent);
        }
    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Voice commands ready", Toast.LENGTH_SHORT).show();
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
        // Stop listening when user leaves this screen
        if (voiceCommandHelper != null) {
            voiceCommandHelper.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure portrait orientation when returning
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Restart wake word detection when returning to this screen
        if (voiceCommandHelper != null && checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (voiceCommandHelper != null) {
            voiceCommandHelper.cleanup();
        }
    }
}