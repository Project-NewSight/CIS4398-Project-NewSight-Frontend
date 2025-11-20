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

import org.json.JSONObject;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    private TtsHelper ttsHelper;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private VoiceCommandHelper voiceCommandHelper;
    private String sessionId;
    private com.example.newsight.helpers.LocationHelper locationHelper;
    private com.example.newsight.helpers.LocationWebSocketHelper locationWebSocketHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Generate session ID for this session
        sessionId = java.util.UUID.randomUUID().toString();

        // Initialize voice command helper
        voiceCommandHelper = new VoiceCommandHelper(this);
        voiceCommandHelper.setSessionId(sessionId); // Set session ID for navigation
        ttsHelper = new TtsHelper(this);

        // Set up voice command callbacks
        voiceCommandHelper.setCallback(new VoiceCommandHelper.VoiceCommandCallback() {
            @Override
            public void onWakeWordDetected() {
                Log.d(TAG, "Wake word detected");
                // Optional: Visual feedback when wake word detected
            }

            @Override
            public void onCommandStarted() {
                Log.d(TAG, "Command recording started");
                // Optional: Show recording indicator
            }

            @Override
            public void onCommandProcessing() {
                Log.d(TAG, "Processing command");
                // Optional: Show processing state
            }

            @Override
            public void onResponseReceived(String jsonResponse) {
                Log.d(TAG, "Response received: " + jsonResponse);

                // TODO: Pass jsonResponse to TTS helper class when ready
                // ttsHelper.handleResponse(jsonResponse);

                // For now, just log it
                // The JSON contains:
                // - confidence
                // - extracted_params (feature, query, destination, sub_features)
                // - TTS_Output (message)
            }

            @Override
            public void onNavigateToFeature(String feature, JSONObject extractedParams) {
                Log.d(TAG, "Navigating to feature: " + feature);
                navigateToFeature(feature, extractedParams);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
                // Optional: Handle error UI
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "Voice command completed");
                // Optional: Reset UI state
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

        // Read and Ask
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                // Start voice recording directly in this activity
                voiceCommandHelper.startDirectRecording();
            } else {
                requestMicrophonePermission();
            }
        });

        // Start location tracking for navigation requests
        startBackgroundLocation();

        // Auto-start wake word detection when activity starts
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }
    
    private void startBackgroundLocation() {
        if (!checkLocationPermission()) {
            return; // Will request permissions when needed
        }
        
        // Start GPS tracking
        locationHelper = new com.example.newsight.helpers.LocationHelper(this);
        locationHelper.setLocationCallback(new com.example.newsight.helpers.LocationHelper.LocationUpdateCallback() {
            @Override
            public void onLocationUpdate(double latitude, double longitude, float accuracy) {
                // Send to backend location WebSocket
                if (locationWebSocketHelper != null && locationWebSocketHelper.isConnected()) {
                    locationWebSocketHelper.sendLocation(latitude, longitude);
                }
            }

            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Location error: " + error);
            }
        });
        locationHelper.startLocationUpdates();
        
        // Connect location WebSocket
        locationWebSocketHelper = new com.example.newsight.helpers.LocationWebSocketHelper(
            "ws://192.168.1.254:8000/location/ws", sessionId);
        locationWebSocketHelper.connect();
    }
    
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
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
                // Check if we got full directions from backend
                JSONObject directionsObj = extractedParams.optJSONObject("directions");
                
                intent = new Intent(HomeActivity.this, NavigateActivity.class);
                if (directionsObj != null) {
                    // We have full directions! Pass them to NavigateActivity
                    intent.putExtra("auto_start_navigation", true);
                    intent.putExtra("directions_json", directionsObj.toString());
                    intent.putExtra("session_id", sessionId);
                    ttsMessage = "Starting navigation";
                    Log.d(TAG, "✅ Passing full directions to NavigateActivity");
                } else {
                    // No directions yet, just pass the destination
                    String destination = extractedParams.optString("destination", null);
                    intent.putExtra("auto_start_navigation", true);
                    intent.putExtra("destination", destination);
                    intent.putExtra("session_id", sessionId);
                    ttsMessage = "Activating navigation";
                    Log.d(TAG, "⚠️ Only passing destination to NavigateActivity");
                }
                Toast.makeText(this, "Opening Navigation", Toast.LENGTH_SHORT).show();
                break;

            case "OBJECT_DETECTION":
                intent = new Intent(HomeActivity.this, ObstacleActivity.class);
                ttsMessage = "Activating Object Detection";
                Toast.makeText(this, "Opening Observe", Toast.LENGTH_SHORT).show();
                break;

            case "FACIAL_RECOGNITION":
                intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra("feature", "detect_people");
                ttsMessage = "Activating Facial Recognition";
                Toast.makeText(this, "Opening Observe", Toast.LENGTH_SHORT).show();
                break;

            case "TEXT_DETECTION":
                intent = new Intent(HomeActivity.this, DetectionActivity.class);
                ttsMessage = "Activating Text Detection";
                Toast.makeText(this, "Opening Observe", Toast.LENGTH_SHORT).show();
                break;

            case "COLOR_CUE":
                intent = new Intent(HomeActivity.this, ColorCueActivity.class);
                ttsMessage = "Activating Color Cue";
                Toast.makeText(this, "Opening Observe", Toast.LENGTH_SHORT).show();
                break;

            case "ASL_DETECTOR":
                intent = new Intent(HomeActivity.this, CommunicateActivity.class);
                ttsMessage = "Activating ASL Detector";
                Toast.makeText(this, "Opening Communicate", Toast.LENGTH_SHORT).show();
                break;

            case "EMERGENCY_CONTACT":
                intent = new Intent(HomeActivity.this, EmergencyActivity.class);
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
        // Stop listening when user leaves this screen
        if (voiceCommandHelper != null) {
            voiceCommandHelper.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        if (locationHelper != null) {
            locationHelper.cleanup();
        }
        if (locationWebSocketHelper != null) {
            locationWebSocketHelper.cleanup();
        }
    }
}