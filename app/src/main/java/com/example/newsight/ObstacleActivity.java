package com.example.newsight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import com.example.newsight.models.VoiceResponse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ObstacleActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String TAG = "ObstacleActivity";

    private PreviewView previewView;
    private OverlayView overlayView;
    private VoiceCommandHelper voiceCommandHelper;
    private TtsHelper ttsHelper;
    private String sessionId;
    private com.example.newsight.helpers.LocationHelper locationHelper;
    private com.example.newsight.helpers.LocationWebSocketHelper locationWebSocketHelper;

    //Haptic Feedback Components
    private VibrationMotor vibrationMotor;
    private PatternGenerator patternGenerator;
    private boolean hapticInitialized = false;
    private long lastHapticTime = 0;
    private static final long HAPTIC_COOLDOWN_MS = 1500;
    private String lastDetectedObstacle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // force landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        setContentView(R.layout.activity_obstacle);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlay);

        // Generate session ID
        sessionId = java.util.UUID.randomUUID().toString();

        // Initialize voice command helper with session ID
        voiceCommandHelper = new VoiceCommandHelper(this);
        voiceCommandHelper.setSessionId(sessionId);
        ttsHelper = new TtsHelper(this);

        // Initialize haptic feedback system
        initializeHapticFeedback();

        setupVoiceCommands();
        setupBottomNavigation();

        // Start background location tracking for navigation requests
        startBackgroundLocation();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCameraStream();
        }

        // Auto-start wake word detection if permission granted
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    // ==================== Haptic Feedback System ====================

    /**
     * Initialize haptic feedback system
     */
    private void initializeHapticFeedback() {
        try {
            vibrationMotor = new VibrationMotor(this);
            vibrationMotor.initialize();

            patternGenerator = new PatternGenerator();

            hapticInitialized = true;
            Log.d(TAG, "✅ Haptic feedback system initialized");

        } catch (VibrationMotor.VibrationException e) {
            Log.e(TAG, "❌ Failed to initialize haptic feedback: " + e.getMessage());
            hapticInitialized = false;
            Toast.makeText(this, "Haptic feedback unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback from DetectorProcessor when obstacles are detected
     */
    public void onObstaclesDetected(int obstacleCount, float largestObstacleSize, String closestObstacleType) {
        if (!hapticInitialized || vibrationMotor == null || patternGenerator == null) {
            return;
        }

        // Cooldown check - don't spam haptic warnings
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastHapticTime) < HAPTIC_COOLDOWN_MS) {
            return;
        }

        // Only trigger haptic if obstacles are detected
        if (obstacleCount == 0) {
            return;
        }

        try {
            VibrationPattern pattern;
            int intensity;

            // Determine urgency based on obstacle size (larger = closer = more urgent)
            if (largestObstacleSize > 0.6f) {
                // Very close obstacle (>60% of screen) - maximum alert
                pattern = patternGenerator.generateObstacleWarningPattern();
                intensity = 100;
                Log.d(TAG, "🚨 CRITICAL: Very close obstacle detected - " + closestObstacleType);

            } else if (largestObstacleSize > 0.4f) {
                // Close obstacle (40-60% of screen) - high alert
                pattern = patternGenerator.generateProximityPattern(5.0f); // ~5m estimated
                intensity = 85;
                Log.d(TAG, "⚠️ WARNING: Close obstacle detected - " + closestObstacleType);

            } else if (largestObstacleSize > 0.2f) {
                // Medium distance (20-40% of screen) - medium alert
                pattern = patternGenerator.generateProximityPattern(10.0f); // ~10m estimated
                intensity = 70;
                Log.d(TAG, "⚡ CAUTION: Obstacle detected - " + closestObstacleType);

            } else {
                // Far obstacle (<20% of screen) - low alert
                pattern = patternGenerator.generateProximityPattern(20.0f); // ~20m estimated
                intensity = 55;
                Log.d(TAG, "ℹ️ NOTICE: Distant obstacle - " + closestObstacleType);
            }

            // Trigger the vibration
            if (pattern != null && pattern.validate()) {
                vibrationMotor.triggerVibration(pattern, (int) pattern.getDuration(), intensity);

                lastDetectedObstacle = closestObstacleType;
                lastHapticTime = currentTime;

                Log.d(TAG, String.format("✅ Haptic triggered - count: %d, size: %.2f, type: %s, intensity: %d%%",
                        obstacleCount, largestObstacleSize, closestObstacleType, intensity));
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error triggering obstacle haptic: " + e.getMessage());
        }
    }

    /**
     * Stop all haptic feedback
     */
    private void stopHapticFeedback() {
        if (vibrationMotor != null) {
            vibrationMotor.stopVibration();
        }
    }

    /**
     * Cleanup haptic system
     */
    private void cleanupHapticFeedback() {
        if (vibrationMotor != null) {
            vibrationMotor.close();
            vibrationMotor = null;
        }

        patternGenerator = null;
        hapticInitialized = false;

        Log.d(TAG, "✅ Haptic feedback system cleaned up");
    }

    private void startBackgroundLocation() {
        if (!checkLocationPermission()) {
            return;
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

    private void startCameraStream() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        try {
            // Create detector with haptic callback
            DetectorProcessor detector = new DetectorProcessor(
                    this,  // Context
                    overlayView,  // OverlayView
                    new DetectorProcessor.ObstacleDetectionCallback() {  // Callback
                        @Override
                        public void onObstaclesDetected(int count, float largestSize, String closestType) {
                            // Trigger haptic feedback on main thread
                            runOnUiThread(() ->
                                    ObstacleActivity.this.onObstaclesDetected(count, largestSize, closestType)
                            );
                        }
                    }
            );

            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), detector);
        } catch (Exception e) {
            Log.e(TAG, "Could not initialize detector.", e);
            Toast.makeText(getApplicationContext(), "Detector Failed to Start", Toast.LENGTH_SHORT).show();
            return;
        }

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraStream();
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Voice commands ready", Toast.LENGTH_SHORT).show();
                voiceCommandHelper.startWakeWordDetection();
            } else {
                Toast.makeText(this, "Microphone permission is required for voice commands",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupVoiceCommands() {
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
                navigateToOtherFeature(feature, extractedParams);
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
    }

    private void setupBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navVoice = findViewById(R.id.navVoice);
        LinearLayout navSettings = findViewById(R.id.navSettings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(ObstacleActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (navVoice != null) {
            navVoice.setOnClickListener(v -> {
                if (checkMicrophonePermission()) {
                    voiceCommandHelper.startDirectRecording();
                } else {
                    requestMicrophonePermission();
                }
            });
        }

        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(ObstacleActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
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
    protected void onPause() {
        super.onPause();
        if (voiceCommandHelper != null) {
            voiceCommandHelper.stopListening();
        }

        stopHapticFeedback();
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

        cleanupHapticFeedback();

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

    private void navigateToOtherFeature(String feature, JSONObject extractedParams) {
        if (feature == null || feature.isEmpty()) {
            return;
        }

        Intent intent = null;
        String ttsMessage = null;

        switch (feature.toUpperCase()) {
            case "NAVIGATION":
                intent = new Intent(this, NavigateActivity.class);

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
                // Already here
                ttsHelper.speak("You are already in Object Detection mode");
                return;

            case "FACIAL_RECOGNITION":
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("feature", "detect_people");
                ttsMessage = "Activating Facial Recognition";
                break;

            case "TEXT_DETECTION":
                intent = new Intent(this, ReadTextActivity.class);
                intent.putExtra("feature", "text_detection");
                ttsMessage = "Activating Text Detection";
                break;

            case "COLOR_CUE":
                intent = new Intent(this, ColorCueActivity.class);
                ttsMessage = "Activating Color Cue";
                break;

            case "ASL_DETECTOR":
                intent = new Intent(this, CommunicateActivity.class);
                ttsMessage = "Activating ASL Detector";
                break;

            case "EMERGENCY_CONTACT":
                intent = new Intent(this, EmergencyActivity.class);
                ttsMessage = "Activating Emergency Contact";
                break;

            case "HOME":
                intent = new Intent(this, HomeActivity.class);
                ttsMessage = "Going to Home";
                break;

            case "SETTINGS":
                intent = new Intent(this, SettingsActivity.class);
                ttsMessage = "Opening Settings";
                break;

            case "NONE":
                ttsHelper.speak("I am sorry, I am not able to detect the feature");
                return;

            default:
                Log.w(TAG, "Unknown feature: " + feature);
                ttsHelper.speak("I am sorry, I am not able to detect the feature");
                return;
        }

        if (intent != null && ttsMessage != null) {
            ttsHelper.speak(ttsMessage);
            final Intent finalIntent = intent;

            // Add flags to clear top if going home
            if (feature.equalsIgnoreCase("HOME")) {
                finalIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }

            new Handler(getMainLooper()).postDelayed(() -> {
                startActivity(finalIntent);
                if (!feature.equalsIgnoreCase("SETTINGS")) {
                    finish();
                }
            }, 900); // Changed to 900ms to match HomeActivity
        }
    }
}