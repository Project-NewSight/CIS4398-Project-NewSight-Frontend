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

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private VoiceCommandHelper voiceCommandHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize voice command helper
        voiceCommandHelper = new VoiceCommandHelper(this);

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
        FrameLayout btnMic = findViewById(R.id.btnMic);
        FrameLayout btnEmergency = findViewById(R.id.btnEmergency);
        FrameLayout btnNavigate = findViewById(R.id.btnNavigate);
        FrameLayout btnReadText = findViewById(R.id.btnReadText);
        FrameLayout btnObserve = findViewById(R.id.btnObserve); // Identify
        FrameLayout btnFaces = findViewById(R.id.btnFaces);
        FrameLayout btnCommunicate = findViewById(R.id.btnCommunicate); // ASL
        FrameLayout btnColors = findViewById(R.id.btnColors);

        // Apply staggered animations
        animateView(btnMic, 0);
        animateView(btnEmergency, 100);
        animateView(btnNavigate, 150);
        animateView(btnReadText, 200);
        animateView(btnObserve, 250);
        animateView(btnFaces, 300);
        animateView(btnCommunicate, 350);
        animateView(btnColors, 400);

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
            Intent intent = new Intent(HomeActivity.this, ObserveActivity.class);
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

        // Placeholder listeners for new buttons
        if (btnReadText != null) {
            btnReadText.setOnClickListener(v -> Toast.makeText(this, "Read Text clicked", Toast.LENGTH_SHORT).show());
        }
        if (btnFaces != null) {
            btnFaces.setOnClickListener(v -> Toast.makeText(this, "Faces clicked", Toast.LENGTH_SHORT).show());
        }
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
    }
}
