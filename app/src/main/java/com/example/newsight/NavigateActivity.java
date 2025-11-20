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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newsight.helpers.LocationHelper;
import com.example.newsight.helpers.NavigationHelper;
import com.example.newsight.models.DirectionsResponse;
import com.example.newsight.models.NavigationUpdate;
import com.example.newsight.models.VoiceResponse;
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
    private TextView tvStreetName;
    private TextView tvDistance;
    private ImageView ivArrow;
    private TextView tvInstruction;
    private TextView tvLoadingMessage;
    private TextView tvConnectionStatus;
    private FrameLayout btnHome;
    private FrameLayout btnMic;

    // Helpers
    private TtsHelper ttsHelper;
    private VoiceCommandHelper voiceCommandHelper;
    private LocationHelper locationHelper;
    private NavigationHelper navigationHelper;
    private Gson gson;
    private Handler mainHandler;

    // State
    private String sessionId;
    private boolean isNavigating = false;
    private boolean locationWsConnected = false;
    private DirectionsResponse currentDirections;

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
        } else {
            requestAllPermissions();
        }
    }

    private void bindViews() {
        previewView = findViewById(R.id.previewView);
        arOverlay = findViewById(R.id.arOverlay);
        loadingContainer = findViewById(R.id.loadingContainer);
        tvStreetName = findViewById(R.id.tvStreetName);
        tvDistance = findViewById(R.id.tvDistance);
        ivArrow = findViewById(R.id.ivArrow);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        btnHome = findViewById(R.id.btnHome);
        btnMic = findViewById(R.id.btnMic);
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
        // Home Button
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        // Mic Button
        btnMic.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            } else {
                requestAllPermissions();
            }
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
        // This would be a WebSocket connection to send location in background
        // For now, we're using direct location updates through locationHelper
        locationWsConnected = true;
        Log.d(TAG, "✅ Location tracking ready");
    }

    private void sendLocationToBackend(double latitude, double longitude) {
        // In a real implementation, this would send to /location/ws WebSocket
        // For now, location is already being tracked by locationHelper
        // and will be sent via navigationHelper when navigation starts
        Log.d(TAG, String.format("📤 Location ready: (%.6f, %.6f)", latitude, longitude));
    }

    // ==================== Voice Response Handler ====================

    private void handleVoiceResponse(String jsonResponse) {
        try {
            VoiceResponse response = gson.fromJson(jsonResponse, VoiceResponse.class);
            
            if (response == null || response.getExtractedParams() == null) {
                hideLoading();
                return;
            }

            String feature = response.getExtractedParams().getFeature();
            
            if ("NAVIGATION".equals(feature)) {
                DirectionsResponse directions = response.getExtractedParams().getDirections();
                
                if (directions != null && directions.getSteps() != null && !directions.getSteps().isEmpty()) {
                    Log.d(TAG, "✅ Got directions: " + directions.toString());
                    currentDirections = directions;
                    startNavigation(directions);
                } else {
                    hideLoading();
                    String errorMsg = "Could not get directions";
                    ttsHelper.speak(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            } else {
                // Non-navigation feature
                hideLoading();
                navigateToOtherFeature(feature, response.getExtractedParams());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing voice response: " + e.getMessage(), e);
            hideLoading();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== Navigation ====================

    private void startNavigation(DirectionsResponse directions) {
        Log.d(TAG, "🗺️ Starting navigation to: " + directions.getDestination());
        
        isNavigating = true;
        currentDirections = directions;

        // Show AR overlay
        arOverlay.setVisibility(View.VISIBLE);
        hideLoading();

        // Announce destination
        ttsHelper.speak("Starting navigation to " + directions.getDestination());

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
        tvInstruction.setText(firstStep.getInstruction());
        tvDistance.setText(formatDistance(firstStep.getDistanceMeters()));
        updateArrowForInstruction(firstStep.getInstruction());
    }

    private void updateAROverlay(NavigationUpdate update) {
        mainHandler.post(() -> {
            tvInstruction.setText(update.getInstruction());
            tvDistance.setText(update.getFormattedDistance());
            updateArrowForInstruction(update.getInstruction());

            // Voice announcement
            if (update.isShouldAnnounce() && update.getAnnouncement() != null) {
                ttsHelper.speak(update.getAnnouncement());
            }

            Log.d(TAG, "🗺️ " + update.toString());
        });
    }

    private void updateArrowForInstruction(String instruction) {
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

    private void stopNavigation() {
        isNavigating = false;
        
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
                intent = new Intent(this, DetectionActivity.class);
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

            default:
                Log.w(TAG, "Unknown feature: " + feature);
                ttsMessage = "Sorry, I didn't recognize that feature";
                Toast.makeText(this, "Unknown feature: " + feature, Toast.LENGTH_SHORT).show();
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
                        Manifest.permission.RECORD_AUDIO
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
        if (voiceCommandHelper != null) {
            voiceCommandHelper.cleanup();
        }
        if (locationHelper != null) {
            locationHelper.cleanup();
        }
        if (navigationHelper != null) {
            navigationHelper.cleanup();
        }
        if (ttsHelper != null) {
            // ttsHelper.cleanup() if it has one
        }
    }
}
