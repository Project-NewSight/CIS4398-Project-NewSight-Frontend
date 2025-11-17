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
public class ReadTextActivity extends AppCompatActivity implements WebSocketManager.WsListener, TTSHelper.TTSListener {

    private static final String TAG = "ReadTextActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    
    // Backend WebSocket URL - UPDATE THIS TO MATCH YOUR BACKEND
    private static final String SERVER_WS_URL = "ws://10.0.2.2:8000/ws";  // For Android emulator
    // For real device, use: "ws://YOUR_BACKEND_IP:8000/ws"
    
    // Feature identifier for text detection
    private static final String FEATURE_TEXT_DETECTION = "text_detection";

    // UI Components
    private PreviewView previewView;
    private TextView tvConnectionStatus;
    private TextView tvDetectedText;
    private Button btnStartStop;
    private Button btnReadAloud;
    private FrameLayout btnHome;
    private FrameLayout btnMic;

    // Core components
    private ExecutorService cameraExecutor;
    private WebSocketManager wsManager;
    private TTSHelper ttsHelper;

    // State management
    private boolean isDetecting = false;
    private String lastDetectedText = "";
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 1000; // Update text every second

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
        btnHome = findViewById(R.id.btnHome);
        btnMic = findViewById(R.id.btnMic);
    }

    private void initializeComponents() {
        cameraExecutor = Executors.newSingleThreadExecutor();
        ttsHelper = new TTSHelper(this, this);
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
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        // Microphone button - voice command
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceCommandActivity.class);
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
            btnStartStop.setText("Stop Detection");
            tvDetectedText.setText("Detecting text...");
            Toast.makeText(this, "Text detection started", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Text detection started");
        } else {
            Toast.makeText(this, "Not connected to backend", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopDetection() {
        isDetecting = false;
        btnStartStop.setText("Start Detection");
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
                
                // Set up frame analyzer with feature provider
                imageAnalyzer.setAnalyzer(cameraExecutor, 
                    new FrameAnalyzer(wsManager, () -> isDetecting ? FEATURE_TEXT_DETECTION : null));

                // Select back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer);

                Log.i(TAG, "Camera initialized successfully");
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
        try {
            JSONObject json = new JSONObject(jsonResults);
            
            // Try to get the combined text string first
            String detectedText = "";
            
            if (json.has("text_string")) {
                detectedText = json.getString("text_string");
            } else if (json.has("detections")) {
                // Fallback: combine text from individual detections
                JSONArray detections = json.getJSONArray("detections");
                StringBuilder textBuilder = new StringBuilder();
                
                for (int i = 0; i < detections.length(); i++) {
                    JSONObject detection = detections.getJSONObject(i);
                    String text = detection.getString("text");
                    double confidence = detection.getDouble("confidence");
                    
                    // Only include text with confidence above threshold
                    if (confidence >= 0.5) {
                        if (textBuilder.length() > 0) {
                            textBuilder.append(" ");
                        }
                        textBuilder.append(text);
                    }
                }
                
                detectedText = textBuilder.toString();
            }
            
            // Update UI if text is detected and enough time has passed
            if (!detectedText.trim().isEmpty()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
                    lastDetectedText = detectedText;
                    lastUpdateTime = currentTime;
                    
                    // Update text display
                    tvDetectedText.setText(detectedText);
                    
                    Log.d(TAG, "Detected text: " + detectedText);
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing text detection results: " + e.getMessage());
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
        
        Log.d(TAG, "Activity destroyed, resources cleaned up");
    }
}
