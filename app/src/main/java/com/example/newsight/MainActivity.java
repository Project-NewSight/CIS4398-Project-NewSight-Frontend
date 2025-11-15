package com.example.newsight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements WebSocketManager.WsListener {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final String TAG = "MainActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin, btnOpenCamera, btnTestHaptic;
    private FrameLayout cameraContainer;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private WebSocketManager wsManager;

    //Haptic Feedback Components
    private VibrationMotor vibrationMotor;
    private PatternGenerator patternGenerator;
    private Handler hapticHandler;

    private boolean isLoggedIn = false;
    private String currentFeature = "none"; // dynamic feature name (e.g., "emergency", "detect_people", etc.)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Toast.makeText(this, "MainActivity started", Toast.LENGTH_SHORT).show();

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnTestHaptic = findViewById(R.id.btnTestHaptic);
        cameraContainer = findViewById(R.id.cameraContainer);

        btnOpenCamera.setVisibility(android.view.View.GONE);
        cameraContainer.setVisibility(android.view.View.GONE);

        cameraExecutor = Executors.newSingleThreadExecutor();
        hapticHandler = new Handler();

        initializeHapticSystem();

        btnLogin.setOnClickListener(v -> handleLogin());
        btnOpenCamera.setOnClickListener(v -> checkCameraPermission());
        btnTestHaptic.setOnClickListener(v -> testAllHapticPatterns());

        // ✅ Dynamic feature handling
        String featureFromIntent = getIntent().getStringExtra("feature");
        if (featureFromIntent != null && !featureFromIntent.isEmpty()) {
            currentFeature = featureFromIntent;
            isLoggedIn = true;

            // Hide login UI since user came from a feature activity
            etEmail.setVisibility(android.view.View.GONE);
            etPassword.setVisibility(android.view.View.GONE);
            btnLogin.setVisibility(android.view.View.GONE);
            btnOpenCamera.setVisibility(android.view.View.VISIBLE);

            Log.i(TAG, "Launching feature: " + currentFeature);
            checkCameraPermission();
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        isLoggedIn = true;
        Toast.makeText(this, "Logged in as " + email, Toast.LENGTH_SHORT).show();

        // Hide login UI
        etEmail.setVisibility(android.view.View.GONE);
        etPassword.setVisibility(android.view.View.GONE);
        btnLogin.setVisibility(android.view.View.GONE);
        btnOpenCamera.setVisibility(android.view.View.VISIBLE);

        // Start HomeActivity
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);

        // Initialize WebSocket connection
        String wsUrl = "wss://your-backend-url/ws"; // TODO: replace with your actual backend URL
        wsManager = new WebSocketManager(wsUrl, this);
        wsManager.connect();
    }

    private void checkCameraPermission() {
        if (!isLoggedIn) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        cameraContainer.setVisibility(android.view.View.VISIBLE);

        if (previewView == null) {
            previewView = new PreviewView(this);
            cameraContainer.addView(previewView,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));
        }

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                FrameAnalyzer.FeatureProvider provider = () -> currentFeature;
                imageAnalysis.setAnalyzer(cameraExecutor, new FrameAnalyzer(wsManager, provider));

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                Log.i(TAG, "Camera started for feature: " + currentFeature);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Initialize the haptic feedback system
     */
    private void initializeHapticSystem() {
        Log.d(TAG, "Initializing haptic system...");

        if (!HapticPermissionHelper.canVibrate(this)) {
            Log.w(TAG, "Vibration not available on this device");
            Toast.makeText(this, "Vibration not available", Toast.LENGTH_SHORT).show();
            btnTestHaptic.setEnabled(false);
            return;
        }

        try {
            vibrationMotor = new VibrationMotor(this);
            vibrationMotor.initialize();
            patternGenerator = new PatternGenerator();

            Log.d(TAG, "Haptic system initialized successfully");
            Toast.makeText(this, "Haptic system ready!", Toast.LENGTH_SHORT).show();

        } catch (VibrationMotor.VibrationException e) {
            Log.e(TAG, "Failed to initialize haptic system: " + e.getMessage());
            Toast.makeText(this, "Haptic initialization failed", Toast.LENGTH_SHORT).show();
            btnTestHaptic.setEnabled(false);
        }
    }

    /**
     * Test all 8 haptic patterns in sequence
     */
    private void testAllHapticPatterns() {
        if (vibrationMotor == null || !vibrationMotor.isInitialized()) {
            Toast.makeText(this, "Haptic system not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "=== Testing All Haptic Patterns ===");
        Toast.makeText(this, "Testing 8 haptic patterns - feel the difference!",
                Toast.LENGTH_LONG).show();

        // Disable button during test
        btnTestHaptic.setEnabled(false);

        // Start pattern sequence
        testPatternSequence(0);
    }

    /**
     * Execute haptic pattern test sequence
     */
    private void testPatternSequence(int step) {
        if (vibrationMotor == null || patternGenerator == null) {
            return;
        }

        VibrationPattern pattern;
        int delay;

        switch (step) {
            case 0:
                Log.d(TAG, "Testing: RIGHT Turn");
                Toast.makeText(this, "1/8: Right Turn", Toast.LENGTH_SHORT).show();
                pattern = patternGenerator.generateDirectionalPattern(
                        PatternGenerator.Direction.RIGHT, 80);
                vibrationMotor.triggerVibration(pattern, 500, 80);
                delay = 1500;
                break;

            case 1:
                Log.d(TAG, "Testing: LEFT Turn");
                Toast.makeText(this, "2/8: Left Turn", Toast.LENGTH_SHORT).show();
                pattern = patternGenerator.generateDirectionalPattern(
                        PatternGenerator.Direction.LEFT, 80);
                vibrationMotor.triggerVibration(pattern, 500, 80);
                delay = 1500;
                break;

            case 2:
                Log.d(TAG, "Testing: FORWARD");
                Toast.makeText(this, "3/8: Forward", Toast.LENGTH_SHORT).show();
                pattern = patternGenerator.generateDirectionalPattern(
                        PatternGenerator.Direction.FORWARD, 80);
                vibrationMotor.triggerVibration(pattern, 500, 80);
                delay = 1500;
                break;

            case 3:
                Log.d(TAG, "Testing: OBSTACLE WARNING");
                Toast.makeText(this, "4/8: Obstacle Warning", Toast.LENGTH_SHORT).show();
                pattern = patternGenerator.generateObstacleWarningPattern();
                vibrationMotor.triggerVibration(pattern, 600, 100);
                delay = 2000;
                break;

            case 4:
                Log.d(TAG, "Testing: CROSSWALK STOP");
                Toast.makeText(this, "5/8: Crosswalk Stop", Toast.LENGTH_SHORT).show();
                pattern = patternGenerator.generateCrosswalkStopPattern();
                vibrationMotor.triggerVibration(pattern, 850, 80);
                delay = 2000;
                break;

            case 5:
                Log.d(TAG, "Testing: PROXIMITY NEAR");
                Toast.makeText(this, "6/8: Proximity Near", Toast.LENGTH_SHORT).show();
                pattern = patternGenerator.generateProximityPattern(25.0f);
                vibrationMotor.triggerVibration(pattern, 300, 70);
                delay = 1500;
                break;

            case 6:
                Log.d(TAG, "Testing: PROXIMITY VERY NEAR");
                Toast.makeText(this, "7/8: Proximity Very Near", Toast.LENGTH_SHORT).show();
                pattern = patternGenerator.generateProximityPattern(5.0f);
                vibrationMotor.triggerVibration(pattern, 300, 90);
                delay = 1500;
                break;

            case 7:
                Log.d(TAG, "Testing: ARRIVAL CELEBRATION");
                Toast.makeText(this, "8/8: Arrival Celebration", Toast.LENGTH_SHORT).show();
                pattern = patternGenerator.generateArrivalCelebrationPattern();
                vibrationMotor.triggerVibration(pattern, 1600, 70);
                delay = 2500;
                break;

            default:
                Log.d(TAG, "Haptic pattern test complete!");
                Toast.makeText(this, "✓ All patterns tested!", Toast.LENGTH_LONG).show();
                btnTestHaptic.setEnabled(true);
                return;
        }

        // Schedule next pattern
        hapticHandler.postDelayed(() -> testPatternSequence(step + 1), delay);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (wsManager != null) wsManager.disconnect();
        if (vibrationMotor != null) vibrationMotor.close();
        if (hapticHandler != null) hapticHandler.removeCallbacksAndMessages(null);
    }

    // WebSocket callbacks
    @Override
    public void onResultsReceived(String results) {
        runOnUiThread(() -> {
            Log.d(TAG, "Backend results: " + results);
            Toast.makeText(this, "Backend: " + results, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onConnectionStatus(boolean isConnected) {
        runOnUiThread(() -> {
            String status = isConnected ? "Connected to backend" : "Disconnected";
            Log.i(TAG, status);
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        });
    }
}
