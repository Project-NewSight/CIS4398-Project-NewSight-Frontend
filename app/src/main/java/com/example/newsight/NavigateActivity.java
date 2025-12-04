package com.example.newsight;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newsight.helpers.LocationHelper;
import com.example.newsight.helpers.LocationWebSocketHelper;
import com.example.newsight.helpers.NavigationHelper;
import com.example.newsight.models.DirectionsResponse;
import com.example.newsight.models.NavigationUpdate;
import com.example.newsight.models.VoiceResponse;
import com.example.newsight.models.TransitInfo;
import com.example.newsight.models.TransitStop;
import com.example.newsight.models.TransitOption;
import com.example.newsight.models.TransitLeg;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * NavigateActivity - AR Navigation with voice commands
 *
 * Features:
 * - Full-screen camera preview
 * - Voice command integration
 * - Real-time GPS tracking
 * - AR overlay with turn-by-turn navigation
 * - Text-to-speech announcements
 */
public class NavigateActivity extends AppCompatActivity {

    private static final String TAG = "NavigateActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String BACKEND_URL = "http://192.168.1.254:8000";
    private static final String LOCATION_WS_URL = "ws://192.168.1.254:8000/location/ws";
    private static final String NAVIGATION_WS_URL = "ws://192.168.1.254:8000/navigation/ws";

    // UI Components
    private PreviewView previewView;
    private RelativeLayout arOverlay;
    private LinearLayout loadingContainer;
    private TextView tvTransitInfo;  // NEW: Persistent transit banner
    private TextView tvStreetName;
    private TextView tvDistance;
    private ImageView ivArrow;
    private TextView tvInstruction;
    private TextView tvLoadingMessage;
    private TextView tvConnectionStatus;
    private LinearLayout navHome;
    private LinearLayout navVoice;
    private LinearLayout navSettings;

    // Helpers
    private TtsHelper ttsHelper;
    private VoiceCommandHelper voiceCommandHelper;
    private LocationHelper locationHelper;
    private LocationWebSocketHelper locationWebSocketHelper;
    private NavigationHelper navigationHelper;
    private Gson gson;
    private Handler mainHandler;

    // Haptic Feedback Components
    private VibrationMotor vibrationMotor;
    private PatternGenerator patternGenerator;
    private boolean hapticInitialized = false;
    private String lastHapticInstruction = "";
    private long lastHapticTime = 0;
    private static final long HAPTIC_COOLDOWN_MS = 3000;

    // State
    private String sessionId;
    private boolean isNavigating = false;
    private boolean locationWsConnected = false;
    private DirectionsResponse currentDirections;

    // Transit-specific state
    private boolean isTransitNavigation = false;
    private TransitInfo transitInfo;
    private TransitStop nearestStop;
    private boolean transitOptionsShown = false;
    private static final int TRANSIT_OPTIONS_DISTANCE_THRESHOLD = 100; // Show options when within 100m of stop

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        // Initialize
        sessionId = UUID.randomUUID().toString();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());

        // Bind UI
        bindViews();

        // Initialize helpers
        initializeHelpers();

        // Set up listeners
        setupListeners();

        // Request permissions
        if (checkAllPermissions()) {
            startServices();

            // Check if we were launched with navigation data from another activity
            checkForAutoStartNavigation();
        } else {
            requestAllPermissions();
        }
    }

    /**
     * Check if this activity was launched with navigation data from voice command in another activity
     */
    private void checkForAutoStartNavigation() {
        Intent intent = getIntent();
        boolean autoStart = intent.getBooleanExtra("auto_start_navigation", false);

        if (!autoStart) {
            return; // Not launched from navigation voice command
        }

        Log.d(TAG, "🚀 Auto-starting navigation from another activity");

        String fullNavResponse = intent.getStringExtra("full_navigation_response");
        String directionsJson = intent.getStringExtra("directions_json");
        String destination = intent.getStringExtra("destination");
        String passedSessionId = intent.getStringExtra("session_id");

        if (fullNavResponse != null && !fullNavResponse.isEmpty()) {
            // NEW: Parse the full navigation response with all transit data
            Log.d(TAG, "✅ Got full navigation response from HomeActivity");

            mainHandler.postDelayed(() -> {
                try {
                    JSONObject navData = new JSONObject(fullNavResponse);

                    Log.d(TAG, "🔍 Full nav data keys: " + navData.keys().toString());

                    // Parse directions
                    JSONObject directionsObj = navData.optJSONObject("directions");
                    if (directionsObj == null) {
                        Log.e(TAG, "❌ No directions in navigation response");
                        Toast.makeText(this, "No directions available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DirectionsResponse directions = gson.fromJson(directionsObj.toString(), DirectionsResponse.class);

                    if (directions == null || directions.getSteps() == null || directions.getSteps().isEmpty()) {
                        Log.e(TAG, "❌ Invalid directions data");
                        Toast.makeText(this, "Error: Invalid directions", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if this is transit navigation
                    boolean isTransit = navData.optBoolean("is_transit_navigation", false);
                    String navType = navData.optString("navigation_type", "walking");

                    Log.d(TAG, "🔍 is_transit_navigation: " + isTransit);
                    Log.d(TAG, "🔍 navigation_type: " + navType);

                    if (isTransit || "transit".equals(navType)) {
                        isTransitNavigation = true;

                        // Parse transit_info
                        JSONObject transitInfoObj = navData.optJSONObject("transit_info");
                        if (transitInfoObj != null) {
                            transitInfo = gson.fromJson(transitInfoObj.toString(), TransitInfo.class);
                            Log.d(TAG, "✅ Transit info parsed");
                        } else {
                            Log.w(TAG, "⚠️ No transit_info in response");
                        }

                        // Parse nearest_stop
                        JSONObject nearestStopObj = navData.optJSONObject("nearest_stop");
                        if (nearestStopObj != null) {
                            nearestStop = gson.fromJson(nearestStopObj.toString(), TransitStop.class);
                            Log.d(TAG, "✅ Nearest stop parsed: " + nearestStop.getName());
                        } else {
                            Log.w(TAG, "⚠️ No nearest_stop in response");
                        }

                        Log.d(TAG, "🚌 Transit navigation data loaded from HomeActivity");
                    } else {
                        // Walking navigation - reset transit state
                        isTransitNavigation = false;
                        transitInfo = null;
                        nearestStop = null;
                        transitOptionsShown = false;
                        Log.d(TAG, "🚶 Walking navigation - transit state reset");
                    }

                    Log.d(TAG, "📍 Starting navigation with " + directions.getSteps().size() + " steps");
                    startNavigation(directions);

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parsing navigation response: " + e.getMessage(), e);
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading navigation", Toast.LENGTH_SHORT).show();
                }
            }, 1500); // Wait for camera and services to initialize

        } else if (directionsJson != null && !directionsJson.isEmpty()) {
            // Fallback: old way with just directions (no transit info)
            Log.d(TAG, "✅ Got directions JSON (legacy mode)");

            mainHandler.postDelayed(() -> {
                try {
                    DirectionsResponse directions = gson.fromJson(directionsJson, DirectionsResponse.class);

                    if (directions != null && directions.getSteps() != null && !directions.getSteps().isEmpty()) {
                        Log.d(TAG, "📍 Starting navigation with " + directions.getSteps().size() + " steps");

                        // No transit info in legacy mode
                        isTransitNavigation = false;
                        transitInfo = null;
                        nearestStop = null;
                        transitOptionsShown = false;

                        startNavigation(directions);
                    } else {
                        Log.e(TAG, "❌ Invalid directions data");
                        Toast.makeText(this, "Error: Invalid directions", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parsing directions: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading directions", Toast.LENGTH_SHORT).show();
                }
            }, 1500);

        } else if (destination != null && !destination.isEmpty()) {
            // Only destination provided - don't auto-trigger recording
            Log.d(TAG, "⚠️ Only have destination: " + destination);
            hideLoading();
            Toast.makeText(this,
                    "Use voice command or tap the mic button to get directions",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void bindViews() {
        previewView = findViewById(R.id.previewView);
        arOverlay = findViewById(R.id.arOverlay);
        loadingContainer = findViewById(R.id.loadingContainer);
        tvTransitInfo = findViewById(R.id.tvTransitInfo);
        tvStreetName = findViewById(R.id.tvStreetName);
        tvDistance = findViewById(R.id.tvDistance);
        ivArrow = findViewById(R.id.ivArrow);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        navHome = findViewById(R.id.navHome);
        navVoice = findViewById(R.id.navVoice);
        navSettings = findViewById(R.id.navSettings);
    }

    private void initializeHelpers() {
        ttsHelper = new TtsHelper(this);

        // Voice Command Helper
        voiceCommandHelper = new VoiceCommandHelper(this);
        voiceCommandHelper.setSessionId(sessionId); // Set session ID for navigation
        voiceCommandHelper.setCallback(new VoiceCommandHelper.VoiceCommandCallback() {
            @Override
            public void onWakeWordDetected() {
                Log.d(TAG, "✅ Wake word detected");
                updateLoadingMessage("Listening...");
            }

            @Override
            public void onCommandStarted() {
                Log.d(TAG, "🎙️ Recording command");
                showLoading("Recording your command...");
            }

            @Override
            public void onCommandProcessing() {
                Log.d(TAG, "⚙️ Processing command");
                showLoading("Processing...");
            }

            @Override
            public void onResponseReceived(String jsonResponse) {
                Log.d(TAG, "📦 Response: " + jsonResponse);
                handleVoiceResponse(jsonResponse);
            }

            @Override
            public void onNavigateToFeature(String feature, JSONObject extractedParams) {
                Log.d(TAG, "🧭 Navigate to: " + feature);
                // Navigation is handled in handleVoiceResponse
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Voice error: " + error);
                hideLoading();
                Toast.makeText(NavigateActivity.this, error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "✅ Voice command complete");
            }
        });

        // Location Helper
        locationHelper = new LocationHelper(this);
        locationHelper.setLocationCallback(new LocationHelper.LocationUpdateCallback() {
            @Override
            public void onLocationUpdate(double latitude, double longitude, float accuracy) {
                Log.d(TAG, String.format("📍 Location: (%.6f, %.6f) ±%.1fm", latitude, longitude, accuracy));

                // Send to location WebSocket (for background tracking)
                sendLocationToBackend(latitude, longitude);

                // If navigating, also send to navigation WebSocket
                if (isNavigating && navigationHelper != null && navigationHelper.isConnected()) {
                    navigationHelper.sendLocation(latitude, longitude);
                }
            }

            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "❌ Location error: " + error);
                updateConnectionStatus("GPS Error", "#FF0000");
            }
        });
    }

    private void setupListeners() {
        // Home Navigation
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        // Voice Navigation
        navVoice.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            } else {
                requestAllPermissions();
            }
        });

        // Settings Navigation
        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void startServices() {
        startCamera();
        startLocationTracking();
        startLocationWebSocket();

        // Auto-start wake word detection
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    // ==================== Camera ====================

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

                Log.i(TAG, "✅ Camera started");
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "❌ Camera failed: " + e.getMessage(), e);
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ==================== Location Tracking ====================

    private void startLocationTracking() {
        locationHelper.startLocationUpdates();
        updateConnectionStatus("GPS Active", "#00FF00");
    }

    private void startLocationWebSocket() {
        locationWebSocketHelper = new LocationWebSocketHelper(LOCATION_WS_URL, sessionId);
        locationWebSocketHelper.setConnectionCallback(new LocationWebSocketHelper.ConnectionCallback() {
            @Override
            public void onConnected() {
                locationWsConnected = true;
                Log.d(TAG, "✅ Location WebSocket connected");
                updateConnectionStatus("Location WS Connected", "#00FF00");
            }

            @Override
            public void onDisconnected() {
                locationWsConnected = false;
                Log.w(TAG, "⚠️ Location WebSocket disconnected");
                updateConnectionStatus("Location WS Disconnected", "#FFAA00");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Location WebSocket error: " + error);
            }
        });

        locationWebSocketHelper.connect();
        Log.d(TAG, "🔌 Starting location WebSocket...");
    }

    private void sendLocationToBackend(double latitude, double longitude) {
        // Send to location WebSocket for backend tracking
        if (locationWebSocketHelper != null && locationWebSocketHelper.isConnected()) {
            locationWebSocketHelper.sendLocation(latitude, longitude);
        } else {
            Log.w(TAG, "⚠️ Location WS not connected, cannot send location");
        }
    }

    // ==================== Voice Response Handler ====================

    private void handleVoiceResponse(String jsonResponse) {
        try {
            Log.d(TAG, "📦 RAW Response: " + jsonResponse);

            VoiceResponse response = gson.fromJson(jsonResponse, VoiceResponse.class);

            if (response == null) {
                Log.e(TAG, "❌ Response is NULL");
                hideLoading();
                return;
            }

            if (response.getExtractedParams() == null) {
                Log.e(TAG, "❌ ExtractedParams is NULL");
                hideLoading();
                return;
            }

            String feature = response.getExtractedParams().getFeature();
            Log.d(TAG, "🎯 Feature: " + feature);

            if ("NAVIGATION".equals(feature)) {
                Log.d(TAG, "✅ NAVIGATION feature detected!");

                // Check if this is transit navigation
                boolean isTransit = response.getExtractedParams().isTransitNavigation();
                Log.d(TAG, "🚌 Navigation type: " + (isTransit ? "TRANSIT" : "WALKING"));


                // Backend should return directions object
                DirectionsResponse directions = response.getExtractedParams().getDirections();

                if (directions == null) {
                    Log.e(TAG, "❌ Directions is NULL - checking if we got destination at least");
                    String destination = response.getExtractedParams().getDestination();
                    Log.e(TAG, "Destination from response: " + destination);

                    hideLoading();
                    String errorMsg = "Backend didn't return directions. Check session ID.";
                    ttsHelper.speak(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    return;
                }

                if (directions.getSteps() == null || directions.getSteps().isEmpty()) {
                    Log.e(TAG, "❌ No steps in directions - status: " + directions.getStatus());
                    hideLoading();
                    String errorMsg = "No route found: " + directions.getMessage();
                    ttsHelper.speak(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    return;
                }

                Log.d(TAG, "✅ Got " + directions.getSteps().size() + " steps to " + directions.getDestination());
                currentDirections = directions;

                // If transit navigation, store transit info
                if (isTransit) {
                    isTransitNavigation = true;
                    transitInfo = response.getExtractedParams().getTransitInfo();
                    nearestStop = response.getExtractedParams().getNearestStop();

                    if (transitInfo != null && nearestStop != null) {
                        Log.d(TAG, "✅ Transit info stored: " + nearestStop.getName());
                        Log.d(TAG, "📊 Best transit route available: " + (transitInfo.hasBestOption() ? "Yes" : "No"));
                    }
                } else {
                    // Reset transit state for walking navigation
                    isTransitNavigation = false;
                    transitInfo = null;
                    nearestStop = null;
                    transitOptionsShown = false;
                    Log.d(TAG, "🚶 Walking navigation - transit state reset");
                }

                startNavigation(directions);

            } else {
                Log.d(TAG, "ℹ️ Non-navigation feature: " + feature);
                // Non-navigation feature
                hideLoading();
                navigateToOtherFeature(feature, response.getExtractedParams());
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing voice response: " + e.getMessage(), e);
            e.printStackTrace();
            hideLoading();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
     * Trigger haptic feedback based on navigation update
     */
    private void triggerHapticForNavigation(NavigationUpdate update) {
        if (!hapticInitialized || vibrationMotor == null || patternGenerator == null) {
            return;
        }

        String instruction = update.getInstruction();
        if (instruction == null || instruction.isEmpty()) {
            return;
        }

        // Cooldown check - don't spam same haptic pattern
        long currentTime = System.currentTimeMillis();
        if (instruction.equals(lastHapticInstruction) &&
                (currentTime - lastHapticTime) < HAPTIC_COOLDOWN_MS) {
            return;
        }

        try {
            VibrationPattern pattern = null;
            int intensity = 70; // Default intensity

            // Distance-based proximity feedback
            double distanceMeters = update.getDistanceToNext();

            if (distanceMeters < 10) {
                // Very close to turn - high intensity proximity alert
                pattern = patternGenerator.generateProximityPattern((float) distanceMeters);
                intensity = 90;
                Log.d(TAG, "🔊 Proximity alert - very close (" + distanceMeters + "m)");

            } else if (distanceMeters < 30) {
                // Approaching turn - medium intensity
                pattern = patternGenerator.generateProximityPattern((float) distanceMeters);
                intensity = 75;
                Log.d(TAG, "🔊 Proximity alert - approaching (" + distanceMeters + "m)");

            } else if (distanceMeters < 100) {
                // Determine directional pattern based on instruction
                String lower = instruction.toLowerCase();

                if (lower.contains("left")) {
                    pattern = patternGenerator.generateDirectionalPattern(
                            PatternGenerator.Direction.LEFT, intensity);
                    Log.d(TAG, "⬅️ Haptic: Turn LEFT");

                } else if (lower.contains("right")) {
                    pattern = patternGenerator.generateDirectionalPattern(
                            PatternGenerator.Direction.RIGHT, intensity);
                    Log.d(TAG, "➡️ Haptic: Turn RIGHT");

                } else {
                    pattern = patternGenerator.generateDirectionalPattern(
                            PatternGenerator.Direction.FORWARD, intensity);
                    Log.d(TAG, "⬆️ Haptic: Continue FORWARD");
                }
            }

            // Trigger the vibration if we have a pattern
            if (pattern != null && pattern.validate()) {
                vibrationMotor.triggerVibration(pattern, (int) pattern.getDuration(), intensity);

                lastHapticInstruction = instruction;
                lastHapticTime = currentTime;

                Log.d(TAG, "✅ Haptic triggered - distance: " + distanceMeters + "m, intensity: " + intensity + "%");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error triggering haptic feedback: " + e.getMessage());
        }
    }

    /**
     * Trigger celebration haptic pattern when arriving at destination
     */
    private void triggerArrivalCelebration() {
        if (!hapticInitialized || vibrationMotor == null || patternGenerator == null) {
            return;
        }

        try {
            VibrationPattern celebrationPattern = patternGenerator.generateArrivalCelebrationPattern();

            if (celebrationPattern != null && celebrationPattern.validate()) {
                vibrationMotor.triggerVibration(celebrationPattern,
                        (int) celebrationPattern.getDuration(), 80);

                Log.d(TAG, "🎉 Arrival celebration haptic triggered!");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error triggering celebration haptic: " + e.getMessage());
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

    // ==================== Navigation ====================

    private void startNavigation(DirectionsResponse directions) {
        Log.d(TAG, "🗺️ Starting navigation to: " + directions.getDestination());

        isNavigating = true;
        currentDirections = directions;

        // Initialize haptic feedback system
        initializeHapticFeedback();

        // Show AR overlay
        arOverlay.setVisibility(View.VISIBLE);
        hideLoading();

        // Show transit banner if this is transit navigation
        if (isTransitNavigation && transitInfo != null && nearestStop != null) {
            showPersistentTransitBanner();
        } else {
            // Hide transit banner for walking-only navigation
            tvTransitInfo.setVisibility(View.GONE);
        }

        // Announce destination with navigation mode and duration
        String navigationMode = isTransitNavigation ? "transit" : "walking";
        String announcement = "Starting navigation with " + navigationMode + " to " + directions.getDestination();

        // Add duration if available
        if (directions.getTotalDuration() != null && !directions.getTotalDuration().isEmpty()) {
            announcement += ". Estimated time: " + directions.getTotalDuration();
        }

        ttsHelper.speak(announcement);

        // Connect to navigation WebSocket
        navigationHelper = new NavigationHelper(NAVIGATION_WS_URL, sessionId);
        navigationHelper.setNavigationCallback(new NavigationHelper.NavigationCallback() {
            @Override
            public void onNavigationUpdate(NavigationUpdate update) {
                updateAROverlay(update);
            }

            @Override
            public void onNavigationComplete() {
                Log.d(TAG, "✅ Navigation complete!");
                ttsHelper.speak("You have arrived at your destination");
                triggerArrivalCelebration();
                stopNavigation();
            }

            @Override
            public void onConnectionStatus(boolean isConnected) {
                updateConnectionStatus(isConnected ? "Navigation Active" : "Reconnecting...",
                        isConnected ? "#00FF00" : "#FFAA00");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Navigation error: " + error);
                Toast.makeText(NavigateActivity.this, "Navigation error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        navigationHelper.connect();

        // Display first step
        if (directions.getSteps() != null && !directions.getSteps().isEmpty()) {
            displayFirstStep(directions);
        }
    }

    private void displayFirstStep(DirectionsResponse directions) {
        var firstStep = directions.getSteps().get(0);

        tvStreetName.setText("Starting Navigation");

        // Safely display instruction
        String instruction = firstStep.getInstruction();
        tvInstruction.setText(instruction != null ? instruction : "Continue...");

        // Display distance
        tvDistance.setText(formatDistance(firstStep.getDistanceMeters()));

        // Update arrow
        updateArrowForInstruction(instruction);
    }

    private void updateAROverlay(NavigationUpdate update) {
        mainHandler.post(() -> {
            // Safely set instruction with null check
            String instruction = update.getInstruction();
            tvInstruction.setText(instruction != null ? instruction : "Continue...");

            // Format distance from distance_to_next (in meters)
            String distance = formatDistance((int) update.getDistanceToNext());
            tvDistance.setText(distance);

            // Update arrow
            updateArrowForInstruction(instruction);

            // Voice announcement
            if (update.isShouldAnnounce() && update.getAnnouncement() != null) {
                ttsHelper.speak(update.getAnnouncement());
            }

            // Trigger haptic feedback for navigation cues
            triggerHapticForNavigation(update);

            // FOR TRANSIT NAVIGATION: Check if close to bus stop
            if (isTransitNavigation && !transitOptionsShown && nearestStop != null) {
                checkProximityToBusStop(update);
            }

            Log.d(TAG, "🗺️ " + update.toString());
        });
    }

    /**
     * Check if user is close to bus stop and show transit options
     */
    private void checkProximityToBusStop(NavigationUpdate update) {
        if (nearestStop == null) {
            return;
        }

        // Get user's current location from locationHelper
        if (locationHelper == null || locationHelper.getLastKnownLocation() == null) {
            return;
        }

        android.location.Location userLocation = locationHelper.getLastKnownLocation();
        double userLat = userLocation.getLatitude();
        double userLng = userLocation.getLongitude();

        // Calculate actual distance from user to bus stop
        double stopLat = nearestStop.getLat();
        double stopLng = nearestStop.getLng();

        float[] results = new float[1];
        android.location.Location.distanceBetween(userLat, userLng, stopLat, stopLng, results);
        float distanceToStop = results[0];  // Distance in meters

        Log.d(TAG, String.format("📏 Distance to bus stop: %.1f meters", distanceToStop));

        // If we're close to the bus stop (within threshold), show transit options
        if (distanceToStop <= TRANSIT_OPTIONS_DISTANCE_THRESHOLD) {
            Log.d(TAG, "🚌 User is within " + TRANSIT_OPTIONS_DISTANCE_THRESHOLD + "m of bus stop!");
            transitOptionsShown = true;
            showTransitOptions();
        }
    }

    /**
     * Display the best transit route to the user
     */
    private void showTransitOptions() {
        if (transitInfo == null || !transitInfo.hasBestOption()) {
            Log.w(TAG, "⚠️ No transit route available");
            ttsHelper.speak("You've arrived at the bus stop, but no transit route is currently available.");
            return;
        }

        Log.d(TAG, "🚌 Showing best transit route");

        TransitOption bestRoute = transitInfo.getBestOption();

        // Build announcement for the best route
        StringBuilder announcement = new StringBuilder("You've arrived at ");
        if (nearestStop != null) {
            announcement.append(nearestStop.getName()).append(". ");
        }

        // Extract route info from best option
        String routeInfo = bestRoute.getSummary();
        announcement.append("Take ").append(routeInfo);

        ttsHelper.speak(announcement.toString());

        // Display transit route in UI
        displayTransitOptionsInUI();
    }

    /**
     * Display the best transit route in the AR overlay
     */
    private void displayTransitOptionsInUI() {
        if (transitInfo == null || !transitInfo.hasBestOption()) {
            return;
        }

        TransitOption bestRoute = transitInfo.getBestOption();

        // Update the UI to show the best transit route
        tvStreetName.setText("🚌 Best Route");

        // Show route summary
        tvInstruction.setText(bestRoute.getSummary());

        if (bestRoute.getDurationMin() != null) {
            tvDistance.setText(bestRoute.getDurationMin() + " min trip");
        }

        // Show bus icon instead of navigation arrow
        ivArrow.setImageResource(android.R.drawable.ic_menu_directions);

        // Show alerts if any
        if (transitInfo.hasAlerts()) {
            announceTransitAlerts();
        }

        Toast.makeText(this,
                "Best transit route found. Follow the directions.",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Announce any transit alerts (delays, cancellations)
     */
    private void announceTransitAlerts() {
        if (transitInfo == null || !transitInfo.hasAlerts()) {
            return;
        }

        for (TransitInfo.TransitAlert alert : transitInfo.getAlerts()) {
            Log.w(TAG, "⚠️ Transit alert: " + alert.getMessage());
            ttsHelper.speak("Alert: " + alert.getMessage());
        }
    }

    private void updateArrowForInstruction(String instruction) {
        // Default to straight arrow if instruction is null or empty
        if (instruction == null || instruction.isEmpty()) {
            ivArrow.setImageResource(R.drawable.ic_arrow_straight);
            return;
        }

        String lower = instruction.toLowerCase();

        int arrowResource;
        if (lower.contains("left")) {
            arrowResource = lower.contains("slight") ? R.drawable.ic_arrow_slight_left : R.drawable.ic_arrow_left;
        } else if (lower.contains("right")) {
            arrowResource = lower.contains("slight") ? R.drawable.ic_arrow_slight_right : R.drawable.ic_arrow_right;
        } else {
            arrowResource = R.drawable.ic_arrow_straight;
        }

        ivArrow.setImageResource(arrowResource);
    }

    private String formatDistance(int meters) {
        double feet = meters * 3.28084;

        if (feet < 528) {
            return String.format("%.0f ft", feet);
        } else {
            double miles = feet / 5280;
            return String.format("%.1f mi", miles);
        }
    }

    /**
     * Show persistent transit banner with bus route and arrival time
     */
    private void showPersistentTransitBanner() {
        if (transitInfo == null || !transitInfo.hasBestOption() || nearestStop == null) {
            return;
        }

        TransitOption bestRoute = transitInfo.getBestOption();

        // Extract bus route name
        String routeName = extractRouteName(bestRoute);

        // Extract and format arrival time
        String arrivalInfo = formatBusArrivalTime(bestRoute);

        // Get final destination (from transit info or extract from somewhere)
        String destination = extractDestination();

        // Build banner text (2 lines)
        String bannerText = "🚌 " + routeName + ": " + arrivalInfo + "\nto " + destination;

        // Display banner
        tvTransitInfo.setText(bannerText);
        tvTransitInfo.setVisibility(View.VISIBLE);

        Log.d(TAG, "✅ Transit banner displayed: " + bannerText);
    }

    /**
     * Extract bus route name from transit option
     */
    private String extractRouteName(TransitOption bestRoute) {
        if (bestRoute == null || bestRoute.getLegs() == null) {
            return "Bus";
        }

        for (TransitLeg leg : bestRoute.getLegs()) {
            if (leg.isTransit() && leg.getRouteShortName() != null && !leg.getRouteShortName().isEmpty()) {
                return "Bus " + leg.getRouteShortName();
            }
        }
        return "Bus";
    }

    /**
     * Format bus arrival time as "Next in X min"
     */
    private String formatBusArrivalTime(TransitOption bestRoute) {
        if (bestRoute == null || bestRoute.getLegs() == null) {
            return "Check schedule";
        }

        // Find transit leg with departure time
        for (TransitLeg leg : bestRoute.getLegs()) {
            if (leg.isTransit() && leg.getDepartureTime() != null) {
                long departureTime = leg.getDepartureTime();
                long now = System.currentTimeMillis() / 1000;
                int minutesUntil = (int) ((departureTime - now) / 60);

                if (minutesUntil <= 1) {
                    return "Arriving now!";
                } else if (minutesUntil < 60) {
                    return "Next in " + minutesUntil + " min";
                } else {
                    // Show actual time if > 1 hour
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.US);
                    return "at " + sdf.format(new java.util.Date(departureTime * 1000));
                }
            }
        }

        return "Check schedule";
    }

    /**
     * Extract final destination from transit info or current directions
     */
    private String extractDestination() {
        if (transitInfo != null && transitInfo.getDestination() != null) {
            Object destText = transitInfo.getDestination().get("text");
            if (destText != null) {
                return destText.toString();
            }
        }

        if (currentDirections != null && currentDirections.getDestination() != null) {
            // This is the bus stop, not final destination, but fallback
            return currentDirections.getDestination();
        }

        return "destination";
    }

    private void stopNavigation() {
        isNavigating = false;

        stopHapticFeedback();
        cleanupHapticFeedback();

        // Reset transit state
        isTransitNavigation = false;
        transitInfo = null;
        nearestStop = null;
        transitOptionsShown = false;


        if (navigationHelper != null) {
            navigationHelper.disconnect();
            navigationHelper = null;
        }

        arOverlay.setVisibility(View.GONE);
        updateConnectionStatus("GPS Active", "#00FF00");

        Toast.makeText(this, "Navigation ended", Toast.LENGTH_SHORT).show();
    }

    // ==================== Navigate to Other Features ====================

    private void navigateToOtherFeature(String feature, VoiceResponse.ExtractedParams params) {
        if (feature == null || feature.isEmpty()) {
            return;
        }

        Intent intent = null;
        String ttsMessage = null;

        switch (feature.toUpperCase()) {
            case "OBJECT_DETECTION":
                intent = new Intent(this, ObstacleActivity.class);
                ttsMessage = "Activating Object Detection";
                break;

            case "FACIAL_RECOGNITION":
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("feature", "detect_people");
                ttsMessage = "Activating Facial Recognition";
                break;

            case "TEXT_DETECTION":
                intent = new Intent(this, ReadTextActivity.class);
                intent.putExtra("feature", "text_detection");
                ttsMessage = "Activating Text Detection";
                Toast.makeText(this, "Opening Read Text", Toast.LENGTH_SHORT).show();
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

        if (intent != null && ttsMessage != null) {
            ttsHelper.speak(ttsMessage);
            final Intent finalIntent = intent;
            mainHandler.postDelayed(() -> {
                startActivity(finalIntent);
                finish();
            }, 900);
        }
    }

    // ==================== UI Updates ====================

    private void showLoading(String message) {
        mainHandler.post(() -> {
            loadingContainer.setVisibility(View.VISIBLE);
            tvLoadingMessage.setText(message);
        });
    }

    private void updateLoadingMessage(String message) {
        mainHandler.post(() -> tvLoadingMessage.setText(message));
    }

    private void hideLoading() {
        mainHandler.post(() -> loadingContainer.setVisibility(View.GONE));
    }

    private void updateConnectionStatus(String status, String color) {
        mainHandler.post(() -> {
            tvConnectionStatus.setText("● " + status);
            tvConnectionStatus.setTextColor(android.graphics.Color.parseColor(color));
            tvConnectionStatus.setVisibility(View.VISIBLE);
        });
    }

    // ==================== Permissions ====================

    private boolean checkAllPermissions() {
        return checkCameraPermission() && checkLocationPermission() && checkMicrophonePermission();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.VIBRATE
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkAllPermissions()) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                startServices();
            } else {
                Toast.makeText(this, "All permissions are required for navigation",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ==================== Lifecycle ====================

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

        // Clean up

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
        if (navigationHelper != null) {
            navigationHelper.cleanup();
        }
        if (ttsHelper != null) {
            // ttsHelper.cleanup() if it has one
        }
    }
}
