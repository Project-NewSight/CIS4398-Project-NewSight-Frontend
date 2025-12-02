package com.example.newsight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
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
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements WebSocketManager.WsListener {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final String TAG = "MainActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin, btnOpenCamera, btnTestHaptic;
    private FrameLayout cameraContainer;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private WebSocketManager wsManager;
    private VoiceCommandHelper voiceCommandHelper;
    private TtsHelper ttsHelper;
    private static final int PERMISSION_REQUEST_CODE = 200;

    // State variables for text display
    private String lastDisplayedText = null;

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

        // Initialize link TextViews
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);

        // Initialize labels for highlighting
        TextView tvEmailLabel = findViewById(R.id.tvEmailLabel);
        TextView tvPasswordLabel = findViewById(R.id.tvPasswordLabel);

        // Focus listeners for label highlighting
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            tvEmailLabel.setTextColor(ContextCompat.getColor(this,
                    hasFocus ? R.color.primary : R.color.muted_foreground));
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            tvPasswordLabel.setTextColor(ContextCompat.getColor(this,
                    hasFocus ? R.color.primary : R.color.muted_foreground));
        });

        btnOpenCamera.setVisibility(android.view.View.GONE);
        cameraContainer.setVisibility(android.view.View.GONE);

        cameraExecutor = Executors.newSingleThreadExecutor();
        hapticHandler = new Handler();

        initializeHapticSystem();

        // Initialize voice command helper
        voiceCommandHelper = new VoiceCommandHelper(this);
        ttsHelper = new TtsHelper(this);

        // Set up voice command callbacks
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

        // Auto-start wake word detection if logged in and permission granted
        if (isLoggedIn && checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        } else {
            // Ensure it's stopped if not logged in
            voiceCommandHelper.stopListening();
        }

        // Password visibility toggle
        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etPassword.getRight() - etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    int selection = etPassword.getSelectionEnd();
                    if (etPassword.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) {
                        etPassword.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
                    } else {
                        etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                    }
                    etPassword.setSelection(selection);
                    // Re-apply tint
                    for (android.graphics.drawable.Drawable drawable : etPassword.getCompoundDrawables()) {
                        if (drawable != null) {
                            drawable.setTint(ContextCompat.getColor(MainActivity.this, R.color.muted_foreground));
                        }
                    }
                    return true;
                }
            }
            return false;
        });

        // Set click listeners
        btnLogin.setOnClickListener(v -> handleLogin());
        btnOpenCamera.setOnClickListener(v -> checkCameraPermission());
        btnTestHaptic.setOnClickListener(v -> testAllHapticPatterns());

        // Navigation links
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        tvCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // ✅ Dynamic feature handling

        String featureFromIntent = getIntent().getStringExtra("feature");
        if (featureFromIntent != null && !featureFromIntent.isEmpty()) {
            currentFeature = featureFromIntent;
            isLoggedIn = true;

            // Hide login UI since user came from a feature activity
            etEmail.setVisibility(android.view.View.VISIBLE);
            etPassword.setVisibility(android.view.View.VISIBLE);
            btnLogin.setVisibility(android.view.View.VISIBLE);
            btnOpenCamera.setVisibility(View.GONE);

            String wsUrl = "wss://cis4398-project-newsight-backend.onrender.com/ws/verify";
            wsManager = new WebSocketManager(wsUrl, this);
            wsManager.setFeature(currentFeature);
            wsManager.connect();

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

        // Save email to SharedPreferences
        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .edit()
                .putString("user_email", email)
                .apply();

        // Hide login UI
        etEmail.setVisibility(android.view.View.VISIBLE);
        etPassword.setVisibility(android.view.View.VISIBLE);
        btnLogin.setVisibility(android.view.View.VISIBLE);
        btnOpenCamera.setVisibility(android.view.View.GONE);

        // Start LoadingActivity (which will then navigate to HomeActivity)
        Intent intent = new Intent(MainActivity.this, LoadingActivity.class);
        startActivity(intent);

        // Clear fields so they are empty when user returns
        etEmail.setText("");
        etPassword.setText("");

        // Initialize WebSocket connection
        String wsUrl = "ws://192.168.1.254:8000/ws/verify";
        wsManager = new WebSocketManager(wsUrl, this);

        currentFeature = "familiar_face";
        wsManager.setFeature(currentFeature);

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
        if (currentFeature == null || "none".equals(currentFeature)) {
            currentFeature = "familiar_face";          // ✅ ensure a real feature
            if (wsManager != null) wsManager.setFeature(currentFeature); // ✅ tell backend
        }

        if (previewView == null) {
            previewView = new PreviewView(this);
            cameraContainer.addView(previewView,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));
        }
        etEmail.setVisibility(android.view.View.GONE);
        etPassword.setVisibility(android.view.View.GONE);
        btnLogin.setVisibility(android.view.View.GONE);
        
        // Show and setup bottom navigation for face detection mode
        android.view.View bottomNav = findViewById(R.id.floatingBottomNav);
        if (bottomNav != null) {
            bottomNav.setVisibility(android.view.View.VISIBLE);
        }
        setupBottomNavigation();
        
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
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLoggedIn) {
                    Toast.makeText(this, "Voice commands ready", Toast.LENGTH_SHORT).show();
                    voiceCommandHelper.startWakeWordDetection();
                }
            } else {
                // Only show error if we are logged in, otherwise it's fine
                if (isLoggedIn) {
                    Toast.makeText(this, "Microphone permission is required for voice commands",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void setupBottomNavigation() {
        android.widget.LinearLayout navHome = findViewById(R.id.navHome);
        android.widget.LinearLayout navVoice = findViewById(R.id.navVoice);
        android.widget.LinearLayout navSettings = findViewById(R.id.navSettings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                // Go back to Home screen
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
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
                // Open Settings
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
    }

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
        
        // Safety check: If login UI is visible, we are in login mode, so disable voice
        if (etEmail != null && etEmail.getVisibility() == View.VISIBLE) {
            isLoggedIn = false; // Ensure state matches UI
            if (voiceCommandHelper != null) {
                voiceCommandHelper.stopListening();
            }
            return;
        }

        if (voiceCommandHelper != null) {
            if (isLoggedIn && checkMicrophonePermission()) {
                voiceCommandHelper.startWakeWordDetection();
            } else {
                voiceCommandHelper.stopListening();
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
        if (voiceCommandHelper != null) {
            voiceCommandHelper.cleanup();
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

    // WebSocket callbacks
    @Override
    public void onResultsReceived(String results) {
        runOnUiThread(() -> {
            try {
                JSONObject jsonObject = new JSONObject(results);
                String detectedText = null;

                if (jsonObject.has("stable_text") && !jsonObject.isNull("stable_text")) {
                    detectedText = jsonObject.getString("stable_text").trim();
                }

                // If new, valid text is found and it's different from the last one shown
                if (detectedText != null && !detectedText.isEmpty() && !detectedText.equals(lastDisplayedText)) {
                    lastDisplayedText = detectedText; // Update the state
                    Toast.makeText(this, lastDisplayedText, Toast.LENGTH_SHORT).show();
                } 
                // If no valid text is found, AND we have never shown any text before
                else if ((detectedText == null || detectedText.isEmpty()) && lastDisplayedText == null) {
                    Toast.makeText(this, "Reading...", Toast.LENGTH_SHORT).show();
                }
                // Otherwise, do nothing to avoid overwriting a valid result with "Reading..."

            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse backend JSON: " + results, e);
                // Only show "Reading..." on error if we haven't found any text yet.
                if (lastDisplayedText == null) {
                    Toast.makeText(this, "Reading...", Toast.LENGTH_SHORT).show();
                }
            }
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
                // Already here
                ttsHelper.speak("You are already in Facial Recognition mode");
                return;

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
            if (feature.equalsIgnoreCase("HOME")) {
                finalIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            
            new Handler(getMainLooper()).postDelayed(() -> {
                startActivity(finalIntent);
                if (!feature.equalsIgnoreCase("SETTINGS")) {
                    finish();
                }
            }, 1000);
        }
    }
}
