package com.example.newsight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for real-time text detection using camera and backend OCR
 * Displays camera preview, detects text from frames, and reads text aloud using TTS
 */
public class ReadTextActivity extends AppCompatActivity implements WebSocketManager.WsListener, ReadTextTTSHelper.TTSListener {

    private static final String TAG = "ReadTextActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;

    // Backend WebSocket URL - UPDATE THIS TO MATCH YOUR BACKEND
    private static final String SERVER_WS_URL = "ws://192.168.1.254:8000/ws";  // For Android emulator
    // For real device, use: "ws://YOUR_BACKEND_IP:8000/ws"

    // Feature identifier for text detection
    private static final String FEATURE_TEXT_DETECTION = "text_detection";

    // UI Components
    private PreviewView previewView;
    private TextView tvConnectionStatus;
    private TextView tvDetectedText;
    private Button btnStartStop;
    private Button btnReadAloud;
    private android.widget.LinearLayout navHome;
    private android.widget.LinearLayout navVoice;
    private android.widget.LinearLayout navSettings;

    // Core components
    private ExecutorService cameraExecutor;
    private WebSocketManager wsManager;
    private ReadTextTTSHelper ttsHelper;
    private VoiceCommandHelper voiceCommandHelper;

    // State management
    private boolean isDetecting = false;
    private String lastDetectedText = "";
    private String lastSpokenText = "";  // Track what we last spoke
    private long lastUpdateTime = 0;
    private long lastSpeechTime = 0;
    private static final long UPDATE_INTERVAL_MS = 300; // Update text every 300ms (even faster)
    private static final long SPEECH_COOLDOWN_MS = 1500; // Wait 1.5 seconds before speaking same text again (reduced)
    private static final long NO_TEXT_TIMEOUT_MS = 2000; // Clear text if nothing detected for 2 seconds (reduced)
    private long lastTextDetectedTime = 0;
    private int consecutiveEmptyResults = 0; // Track empty results
    private static final int CLEAR_AFTER_EMPTY_COUNT = 3; // Clear after 3 empty results

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_text);

        initializeViews();
        initializeComponents();
        setupClickListeners();

        // Check camera permission
        if (checkCameraPermission()) {
            initializeCameraAndBackend();
        } else {
            requestCameraPermission();
        }
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvDetectedText = findViewById(R.id.tvDetectedText);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnReadAloud = findViewById(R.id.btnReadAloud);
        navHome = findViewById(R.id.navHome);
        navVoice = findViewById(R.id.navVoice);
        navSettings = findViewById(R.id.navSettings);
    }

    private void initializeComponents() {
        cameraExecutor = Executors.newSingleThreadExecutor();
        ttsHelper = new ReadTextTTSHelper(this, this);
        
        // Initialize voice command helper
        voiceCommandHelper = new VoiceCommandHelper(this);
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
        
        // Auto-start wake word detection if permission granted
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }
    }

    private void setupClickListeners() {
        // Start/Stop detection button
        btnStartStop.setOnClickListener(v -> toggleDetection());

        // Read aloud button
        btnReadAloud.setOnClickListener(v -> {
            if (lastDetectedText != null && !lastDetectedText.trim().isEmpty()) {
                ttsHelper.speak(lastDetectedText);
            } else {
                Toast.makeText(this, "No text detected yet", Toast.LENGTH_SHORT).show();
            }
        });

        // Home button
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // Voice button
        navVoice.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            } else {
                requestMicrophonePermission();
            }
        });
        
        // Settings button
        navSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void toggleDetection() {
        if (isDetecting) {
            stopDetection();
        } else {
            startDetection();
        }
    }

    private void startDetection() {
        if (wsManager != null && wsManager.isConnected()) {
            isDetecting = true;

            // Reset ALL state for fresh detection
            lastDetectedText = "";
            lastSpokenText = "";
            lastUpdateTime = 0;
            lastSpeechTime = 0;
            lastTextDetectedTime = System.currentTimeMillis();
            consecutiveEmptyResults = 0;

            btnStartStop.setText("Stop Detection");
            tvDetectedText.setText("Detecting text...");
            Toast.makeText(this, "Text detection started - will speak text automatically", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "🚀 Text detection started - all state reset, auto-speech enabled");
        } else {
            Toast.makeText(this, "Not connected to backend", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopDetection() {
        isDetecting = false;
        btnStartStop.setText("Start Detection");
        tvDetectedText.setText("Ready to detect text");

        // Stop any ongoing speech
        if (ttsHelper != null) {
            ttsHelper.stop();
        }

        Toast.makeText(this, "Text detection stopped", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Text detection stopped");
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
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

    private void initializeCameraAndBackend() {
        // Initialize WebSocket connection
        wsManager = new WebSocketManager(SERVER_WS_URL, this);
        wsManager.connect();

        // Start camera
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image analysis use case
                ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // Set up frame analyzer with feature provider - USING ReadTextFrameAnalyzer
                imageAnalyzer.setAnalyzer(cameraExecutor,
                        new ReadTextFrameAnalyzer(wsManager, () -> isDetecting ? FEATURE_TEXT_DETECTION : null));

                // Select back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer);

                Log.i(TAG, "Camera initialized successfully with ReadTextFrameAnalyzer");
            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this,
                        "Camera setup failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // WebSocketManager.WsListener implementation
    @Override
    public void onResultsReceived(String results) {
        runOnUiThread(() -> processTextDetectionResults(results));
    }

    @Override
    public void onConnectionStatus(boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                tvConnectionStatus.setText("✓ Connected");
                tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                tvConnectionStatus.setText("✗ Disconnected");
                tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                stopDetection();
            }
        });
    }

    /**
     * Process text detection results from backend
     * Expected JSON format:
     * {
     *   "detections": [
     *     {"text": "detected text", "confidence": 0.95, "bbox": [[x1,y1], [x2,y2], ...]}
     *   ],
     *   "text_string": "combined detected text"
     * }
     */
    private void processTextDetectionResults(String jsonResults) {
        if (!isDetecting) {
            return; // Don't process if detection is stopped
        }

        try {
            JSONObject json = new JSONObject(jsonResults);
            long currentTime = System.currentTimeMillis();

            // Try to get the combined text string first
            String detectedText = "";

            // Try text_string first (what we send from backend)
            if (json.has("text_string") && !json.isNull("text_string")) {
                detectedText = json.getString("text_string");
                Log.d(TAG, "Got text_string: " + detectedText);
            }
            // Fallback to stable_text if available
            else if (json.has("stable_text") && !json.isNull("stable_text")) {
                detectedText = json.getString("stable_text");
                Log.d(TAG, "Got stable_text: " + detectedText);
            }
            // Fallback to full_text if available
            else if (json.has("full_text") && !json.isNull("full_text")) {
                detectedText = json.getString("full_text");
                Log.d(TAG, "Got full_text: " + detectedText);
            }
            // Last resort: combine text from individual detections
            else if (json.has("detections")) {
                JSONArray detections = json.getJSONArray("detections");
                StringBuilder textBuilder = new StringBuilder();

                for (int i = 0; i < detections.length(); i++) {
                    JSONObject detection = detections.getJSONObject(i);
                    if (detection.has("text")) {
                        String text = detection.getString("text");
                        double confidence = detection.optDouble("confidence", 0.0);

                        // Only include text with confidence above threshold
                        if (confidence >= 0.5) {
                            if (textBuilder.length() > 0) {
                                textBuilder.append(" ");
                            }
                            textBuilder.append(text);
                        }
                    }
                }

                detectedText = textBuilder.toString();
                Log.d(TAG, "Got detections array: " + detectedText);
            }

            // Normalize text (trim, handle null/"null" string)
            if (detectedText == null || detectedText.equals("null")) {
                detectedText = "";
            }
            String normalizedText = detectedText.trim();
            String normalizedLower = normalizedText.toLowerCase();

            Log.d(TAG, "Normalized text: '" + normalizedText + "' (empty=" + normalizedText.isEmpty() + ")");

            // Update UI if text is detected
            if (!normalizedText.isEmpty()) {
                lastTextDetectedTime = currentTime;
                consecutiveEmptyResults = 0; // Reset empty counter when text is found

                if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
                    lastDetectedText = normalizedText;
                    lastUpdateTime = currentTime;

                    // Update text display
                    tvDetectedText.setText(normalizedText);

                    Log.d(TAG, "✅ Detected text: " + normalizedText);

                    // AUTO-SPEAK: Speak if this is new/different text
                    String lastSpokenLower = lastSpokenText.toLowerCase();
                    boolean isNewText = lastSpokenLower.isEmpty() || !normalizedLower.equals(lastSpokenLower);
                    boolean enoughTimePassed = (currentTime - lastSpeechTime) >= SPEECH_COOLDOWN_MS;

                    Log.d(TAG, "Speech check: isNewText=" + isNewText +
                            " ('" + normalizedLower + "' vs '" + lastSpokenLower + "'), " +
                            "enoughTimePassed=" + enoughTimePassed +
                            " (" + (currentTime - lastSpeechTime) + "ms / " + SPEECH_COOLDOWN_MS + "ms)");

                    if (isNewText || enoughTimePassed) {
                        if (ttsHelper != null && ttsHelper.isReady()) {
                            Log.d(TAG, "🔊 Auto-speaking: " + normalizedText);
                            ttsHelper.speak(normalizedText);
                            lastSpokenText = normalizedText;
                            lastSpeechTime = currentTime;
                        } else {
                            Log.w(TAG, "⚠️ TTS not ready, cannot speak");
                        }
                    } else {
                        Log.d(TAG, "⏭️ Skipping speech (same text, too soon)");
                    }
                }
            } else {
                // No text detected - increment empty counter
                consecutiveEmptyResults++;

                // Clear state more aggressively after consecutive empty results
                if (consecutiveEmptyResults >= CLEAR_AFTER_EMPTY_COUNT && !lastDetectedText.isEmpty()) {
                    Log.d(TAG, "🧹 Clearing state after " + consecutiveEmptyResults + " empty results");
                    tvDetectedText.setText("Detecting text...");
                    lastDetectedText = "";
                    lastSpokenText = "";
                    lastSpeechTime = 0;
                    lastUpdateTime = 0;
                    consecutiveEmptyResults = 0;
                } else if (!lastDetectedText.isEmpty()) {
                    // Also clear based on time
                    long timeSinceLastText = currentTime - lastTextDetectedTime;
                    if (timeSinceLastText >= NO_TEXT_TIMEOUT_MS) {
                        Log.d(TAG, "🧹 Clearing display after " + timeSinceLastText + "ms of no text");
                        tvDetectedText.setText("Detecting text...");
                        lastDetectedText = "";
                        lastSpokenText = "";
                        lastSpeechTime = 0;
                        lastUpdateTime = 0;
                        consecutiveEmptyResults = 0;
                    }
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "❌ Error parsing text detection results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // TTSHelper.TTSListener implementation
    @Override
    public void onTTSReady() {
        Log.d(TAG, "TTS is ready");
        runOnUiThread(() ->
                Toast.makeText(this, "Text-to-speech ready", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onTTSError(String error) {
        Log.e(TAG, "TTS error: " + error);
        runOnUiThread(() ->
                Toast.makeText(this, "TTS error: " + error, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onSpeechStart() {
        Log.d(TAG, "Speech started");
    }

    @Override
    public void onSpeechComplete() {
        Log.d(TAG, "Speech completed");
    }

    // Permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCameraAndBackend();
            } else {
                Toast.makeText(this,
                        "Camera permission required for text detection",
                        Toast.LENGTH_LONG).show();
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

    // Lifecycle management
    @Override
    protected void onPause() {
        super.onPause();
        stopDetection();
        if (ttsHelper != null) {
            ttsHelper.stop();
        }
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

        // Clean up camera
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        // Clean up WebSocket
        if (wsManager != null) {
            wsManager.disconnect();
        }

        // Clean up TTS
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        
        // Clean up voice command helper
        if (voiceCommandHelper != null) {
            voiceCommandHelper.cleanup();
        }

        Log.d(TAG, "Activity destroyed, resources cleaned up");
    }
    
    private void navigateToOtherFeature(String feature, JSONObject params) {
        if (feature == null || feature.isEmpty()) {
            return;
        }

        Intent intent = null;
        String ttsMessage = null;

        switch (feature.toUpperCase()) {
            case "NAVIGATION":
                intent = new Intent(this, NavigateActivity.class);
                intent.putExtra("auto_start_navigation", true);
                if (params != null) {
                    try {
                        if (params.has("destination")) {
                            intent.putExtra("destination", params.getString("destination"));
                        }
                        if (params.has("directions")) {
                            intent.putExtra("directions_json", params.getJSONObject("directions").toString());
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing navigation params", e);
                    }
                }
                ttsMessage = "Starting Navigation";
                break;

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
                // Already here
                if (ttsHelper != null) {
                    ttsHelper.speak("You are already in Text Detection mode");
                }
                return;

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
                if (ttsHelper != null) {
                    ttsHelper.speak("I am sorry, I am not able to detect the feature");
                }
                return;

            default:
                Log.w(TAG, "Unknown feature: " + feature);
                if (ttsHelper != null) {
                    ttsHelper.speak("I am sorry, I am not able to detect the feature");
                }
                return;
        }

        if (intent != null && ttsMessage != null) {
            if (ttsHelper != null) {
                ttsHelper.speak(ttsMessage);
            }
            final Intent finalIntent = intent;
            if (feature.equalsIgnoreCase("HOME")) {
                finalIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startActivity(finalIntent);
                finish();
            }, 1000);
        }
    }
}
