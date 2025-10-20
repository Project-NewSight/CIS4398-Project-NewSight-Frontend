package com.example.newsight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    EditText etEmail, etPassword;
    Button btnLogin, btnOpenCamera, btnTestVibration; // Added test button

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;

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
            testVibrationPattern();
            testVibration();
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
        if (!HapticPermissionHelper.canVibrate(this)) {
            Toast.makeText(this, "Cannot vibrate - check permissions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simple test vibration (500ms)
        android.os.Vibrator vibrator = (android.os.Vibrator)
                getSystemService(VIBRATOR_SERVICE);

        if (vibrator != null) {
            Log.d(TAG, "Triggering test vibration");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(
                        500, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }

            Toast.makeText(this, "Vibration triggered!", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Vibrator is null");
            Toast.makeText(this, "Vibrator not available", Toast.LENGTH_SHORT).show();
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
}