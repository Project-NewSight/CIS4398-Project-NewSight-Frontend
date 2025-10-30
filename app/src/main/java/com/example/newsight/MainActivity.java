package com.example.newsight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
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

    private static final String TAG = "MainActivity";
    EditText etEmail, etPassword;
    Button btnLogin, btnOpenCamera, btnTestVibration; // Added test button

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private VibrationMotor vibrationMotor;

    private boolean isLoggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate called");

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
        btnTestVibration = findViewById(R.id.btnTestVibration);

        Log.d(TAG, "Views initialized");

        // Test vibration permission on startup
        testVibrationPermission();

        // Hide buttons initially
        btnOpenCamera.setVisibility(View.GONE);
        btnTestVibration.setVisibility(View.GONE);

        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                isLoggedIn = true;
                Log.d(TAG, "Login successful, isLoggedIn = " + isLoggedIn);
                Toast.makeText(MainActivity.this, "Logged in as " + email, Toast.LENGTH_SHORT).show();

                // Show camera and vibration test buttons
                btnLogin.setVisibility(View.GONE);
                etEmail.setVisibility(View.GONE);
                etPassword.setVisibility(View.GONE);
                btnOpenCamera.setVisibility(View.VISIBLE);
                btnTestVibration.setVisibility(View.VISIBLE);

                Log.d(TAG, "Camera and Vibration buttons visible");
            }
        });

        btnOpenCamera.setOnClickListener(v -> {
            Log.d(TAG, "Camera button clicked!");

            if (!isLoggedIn) {
                Log.d(TAG, "Not logged in");
                Toast.makeText(MainActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Checking camera permission...");
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not granted, requesting...");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                Log.d(TAG, "Permission already granted, opening camera...");
                openCamera();
            }
        });

        // Test vibration button
        btnTestVibration.setOnClickListener(v -> {
            Log.d(TAG, "Test Vibration button clicked");
            testPatternGenerator();
        });

        Log.d(TAG, "onCreate complete");
    }

    private void testVibrationPermission() {
        boolean canVibrate = HapticPermissionHelper.canVibrate(this);

        if (canVibrate) {
            Log.d(TAG, "✓ Vibration is ready to use");
            Toast.makeText(this, "Vibration permission: OK", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "✗ Vibration not available");
            Toast.makeText(this, "Vibration not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void testVibration() {
        Log.d(TAG, "=== Testing VibrationMotor Class ===");

        if (!HapticPermissionHelper.canVibrate(this)) {
            Toast.makeText(this, "Cannot vibrate - check permissions", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Initialize VibrationMotor
            vibrationMotor = new VibrationMotor(this);
            vibrationMotor.initialize();

            // Test 1: Simple vibration
            Log.d(TAG, "Test 1: Simple 500ms vibration");
            vibrationMotor.vibrateSimple(500, 70);

            // Wait a bit before next test
            new android.os.Handler().postDelayed(() -> {
                // Test 2: Pattern vibration
                Log.d(TAG, "Test 2: Pattern vibration");
                long[] timings = {0, 200, 100, 200};
                int[] intensities = {0, 150, 0, 150};
                VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

                vibrationMotor.triggerVibration(pattern, 500, 80);
            }, 1000);

            Toast.makeText(this, "Testing VibrationMotor - Check Logcat", Toast.LENGTH_LONG).show();

        } catch (VibrationMotor.VibrationException e) {
            Log.e(TAG, "VibrationMotor error: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void testVibrationPattern() {
        Log.d(TAG, "=== Testing VibrationPattern Class ===");

        // Test 1: Create a simple pattern
        long[] timings = {0, 200, 100, 200};
        int[] intensities = {0, 150, 0, 150};
        VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

        Log.d(TAG, "Pattern created: " + pattern.toString());
        Log.d(TAG, "Duration: " + pattern.getDuration() + "ms");
        Log.d(TAG, "Is valid: " + pattern.validate());

        // Test 2: Create pattern with simplified constructor
        long[] simpleTimings = {0, 500};
        VibrationPattern simplePattern = new VibrationPattern(simpleTimings, -1);

        Log.d(TAG, "Simple pattern: " + simplePattern.toString());
        Log.d(TAG, "Simple pattern valid: " + simplePattern.validate());

        // Test 3: Create invalid pattern
        long[] badTimings = {0, 200, 100};
        int[] badIntensities = {0, 150}; // Mismatch
        VibrationPattern badPattern = new VibrationPattern(badTimings, badIntensities, -1);

        Log.d(TAG, "Bad pattern valid: " + badPattern.validate()); // Should be false

        Toast.makeText(this, "Check Logcat for VibrationPattern tests", Toast.LENGTH_LONG).show();
    }

    private void testPatternGenerator() {
        Log.d(TAG, "=== Testing PatternGenerator ===");

        if (!HapticPermissionHelper.canVibrate(this)) {
            Toast.makeText(this, "Cannot vibrate", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            vibrationMotor = new VibrationMotor(this);
            vibrationMotor.initialize();
            PatternGenerator generator = new PatternGenerator();

            Log.d(TAG, "Pattern library has " + generator.getPatternCount() + " patterns");

            testPatternSequence(generator, 0);

            Toast.makeText(this, "Testing all patterns - feel the vibrations!",
                    Toast.LENGTH_LONG).show();

        } catch (VibrationMotor.VibrationException e) {
            Log.e(TAG, "Error: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void testPatternSequence(PatternGenerator generator, int step) {
        android.os.Handler handler = new android.os.Handler();

        switch (step) {
            case 0:
                Log.d(TAG, "Testing: RIGHT turn");
                VibrationPattern right = generator.generateDirectionalPattern(
                        PatternGenerator.Direction.RIGHT, 80);
                vibrationMotor.triggerVibration(right, 500, 80);
                handler.postDelayed(() -> testPatternSequence(generator, 1), 1500);
                break;

            case 1:
                Log.d(TAG, "Testing: LEFT turn");
                VibrationPattern left = generator.generateDirectionalPattern(
                        PatternGenerator.Direction.LEFT, 80);
                vibrationMotor.triggerVibration(left, 500, 80);
                handler.postDelayed(() -> testPatternSequence(generator, 2), 1500);
                break;

            case 2:
                Log.d(TAG, "Testing: FORWARD");
                VibrationPattern forward = generator.generateDirectionalPattern(
                        PatternGenerator.Direction.FORWARD, 80);
                vibrationMotor.triggerVibration(forward, 500, 80);
                handler.postDelayed(() -> testPatternSequence(generator, 3), 1500);
                break;

            case 3:
                Log.d(TAG, "Testing: OBSTACLE WARNING");
                VibrationPattern warning = generator.generateObstacleWarningPattern();
                vibrationMotor.triggerVibration(warning, 600, 100);
                handler.postDelayed(() -> testPatternSequence(generator, 4), 2000);
                break;

            case 4:
                Log.d(TAG, "Testing: CROSSWALK STOP");
                VibrationPattern stop = generator.generateCrosswalkStopPattern();
                vibrationMotor.triggerVibration(stop, 850, 80);
                handler.postDelayed(() -> testPatternSequence(generator, 5), 2000);
                break;

            case 5:
                Log.d(TAG, "Testing: ARRIVAL CELEBRATION");
                VibrationPattern celebration = generator.generateArrivalCelebrationPattern();
                vibrationMotor.triggerVibration(celebration, 1600, 70);
                Log.d(TAG, "Pattern test sequence complete!");
                break;
        }
    }

    private void openCamera() {
        Log.d(TAG, "openCamera() called");
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Log.d(TAG, "Launching camera intent...");
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult called, requestCode=" + requestCode);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted!");
                openCamera();
            } else {
                Log.d(TAG, "Camera permission denied");
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vibrationMotor != null) {
            vibrationMotor.close();
        }
    }
}
