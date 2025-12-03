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
import android.widget.TextView;


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
        FrameLayout rewardCard = findViewById(R.id.cardRewards);
        rewardCard.setOnClickListener((v) -> {
            Intent intent = new Intent(HomeActivity.this, RewardsActivity.class);
            intent.putExtra("current_points", 1250);
            startActivity(intent);
        });


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

        // Initialize views for animation
        FrameLayout cardRewards = findViewById(R.id.cardRewards);
        FrameLayout btnMic = findViewById(R.id.btnMic);
        FrameLayout btnEmergency = findViewById(R.id.btnEmergency);
        FrameLayout btnNavigate = findViewById(R.id.btnNavigate);
        FrameLayout btnReadText = findViewById(R.id.btnReadText);
        FrameLayout btnObserve = findViewById(R.id.btnObserve); // Identify
        FrameLayout btnFaces = findViewById(R.id.btnFaces);
        FrameLayout btnCommunicate = findViewById(R.id.btnCommunicate); // ASL
        FrameLayout btnColors = findViewById(R.id.btnColors);

        // Populate Rewards Data (Default 0)
        android.widget.TextView textPoints = findViewById(R.id.textPoints);
        android.widget.TextView textLevel = findViewById(R.id.textLevel);
        android.widget.TextView textStreak = findViewById(R.id.textStreak);
        android.view.View progressLevel = findViewById(R.id.progressLevel);
        android.widget.TextView tvDate = findViewById(R.id.tvDate);

        // Set current date
        if (tvDate != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, MMM dd", java.util.Locale.getDefault());
            String currentDate = dateFormat.format(new java.util.Date());
            tvDate.setText(currentDate);
        }

        if (textPoints != null) textPoints.setText("1,250");
        if (textLevel != null) textLevel.setText("1");
        if (textStreak != null) textStreak.setText("5 day streak!");

        // Initialize progress bar to 62.5% (1,250/2,000)
        if (progressLevel != null) {
            android.view.View progressContainer = (android.view.View) progressLevel.getParent();
            progressContainer.post(() -> {
                int parentWidth = progressContainer.getWidth();
                int progressWidth = (int) (parentWidth * 0.625); // 1,250/2,000 = 62.5%
                android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) progressLevel.getLayoutParams();
                params.width = progressWidth;
                progressLevel.setLayoutParams(params);
            });
        }

        // Apply staggered animations
        animateView(cardRewards, 100);
        animateView(btnMic, 150);
        animateView(btnEmergency, 200);
        animateView(btnNavigate, 250);
        animateView(btnReadText, 300);
        animateView(btnObserve, 350);
        animateView(btnFaces, 400);
        animateView(btnCommunicate, 450);
        animateView(btnColors, 500);

        // Apply touch animations to all tiles
        addTouchAnimation(btnMic);
        addTouchAnimation(btnEmergency);
        addTouchAnimation(btnNavigate);
        addTouchAnimation(btnReadText);
        addTouchAnimation(btnObserve);
        addTouchAnimation(btnFaces);
        addTouchAnimation(btnCommunicate);
        addTouchAnimation(btnColors);

        // Set Click Listeners
        btnNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NavigateActivity.class);
            startActivity(intent);
        });

        btnObserve.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ObstacleActivity.class);
            startActivity(intent);
        });

        btnCommunicate.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CommunicateActivity.class);
            startActivity(intent);
        });

        btnEmergency.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, EmergencyActivity.class);
            startActivity(intent);
        });

        btnFaces.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra("feature", "detect_people");
            startActivity(intent);
        });

        btnReadText.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReadTextActivity.class);
            intent.putExtra("feature", "text_detection");
            startActivity(intent);
        });
        if (btnColors != null) {
            btnColors.setOnClickListener(v -> Toast.makeText(this, "Colors clicked", Toast.LENGTH_SHORT).show());
        }

        // Read and Ask (Voice Search)
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

        // Logout button
        FrameLayout btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // TODO: Implement actual logout functionality (clear session, tokens, etc.)
            // For now, just navigate to login screen
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation
        android.widget.LinearLayout navHome = findViewById(R.id.navHome);
        android.widget.LinearLayout navVoice = findViewById(R.id.navVoice);
        android.widget.LinearLayout navSettings = findViewById(R.id.navSettings);
        android.widget.ScrollView scrollView = findViewById(R.id.scrollView);

        navHome.setOnClickListener(v -> {
            // Scroll to top
            scrollView.fullScroll(android.widget.ScrollView.FOCUS_UP);
        });

        navVoice.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            } else {
                requestMicrophonePermission();
            }
        });

        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
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
                intent = new Intent(HomeActivity.this, NavigateActivity.class);

                // Pass the FULL extracted_params JSON so NavigateActivity can parse everything
                intent.putExtra("auto_start_navigation", true);
                intent.putExtra("full_navigation_response", extractedParams.toString());
                intent.putExtra("session_id", sessionId);

                // Check navigation type for appropriate TTS message
                String navType = extractedParams.optString("navigation_type", "walking");
                boolean isTransit = extractedParams.optBoolean("is_transit_navigation", false);

                if (isTransit || "transit".equals(navType)) {
                    ttsMessage = "Starting transit navigation";
                    Log.d(TAG, "✅ Passing TRANSIT navigation to NavigateActivity");
                } else {
                    ttsMessage = "Starting walking navigation";
                    Log.d(TAG, "✅ Passing WALKING navigation to NavigateActivity");
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
                intent = new Intent(HomeActivity.this, ReadTextActivity.class);
                intent.putExtra("feature", "text_detection");
                ttsMessage = "Activating Text Detection";
                Toast.makeText(this, "Opening Read Text", Toast.LENGTH_SHORT).show();
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

            case "NONE":
                ttsHelper.speak("I am sorry, I am not able to detect the feature");
                Toast.makeText(this, "I am sorry, I am not able to detect the feature", Toast.LENGTH_SHORT).show();
                return;

            default:
                Log.w(TAG, "Unknown feature: ");
                ttsHelper.speak("I am sorry, I am not able to detect the feature");
                Toast.makeText(this, "I am sorry, I am not able to detect the feature", Toast.LENGTH_SHORT).show();
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

    private void animateView(android.view.View view, long delay) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(50f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(500)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void addTouchAnimation(android.view.View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // Scale up slightly on press (like React's whileTap)
                    v.animate()
                            .scaleX(0.98f)
                            .scaleY(0.98f)
                            .setDuration(100)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // Return to normal size
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    break;
            }
            return false; // Allow click events to propagate
        });
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