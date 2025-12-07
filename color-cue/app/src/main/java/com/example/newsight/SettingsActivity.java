package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONException;
import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private VoiceCommandHelper voiceCommandHelper;
    private TtsHelper ttsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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

        // Auto-start wake word detection when activity starts
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish(); // Go back to previous screen (Home)
        });

        FrameLayout btnUserProfile = findViewById(R.id.btnUserProfile);
        btnUserProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            startActivity(intent);
        });

        FrameLayout btnTrustedContacts = findViewById(R.id.btnTrustedContacts);
        btnTrustedContacts.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrustedContactsActivity.class);
            startActivity(intent);
        });

        FrameLayout btnPrivacyData = findViewById(R.id.btnPrivacyData);
        btnPrivacyData.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrivacyAndDataActivity.class);
            startActivity(intent);
        });

        FrameLayout btnHelpSupport = findViewById(R.id.btnHelpSupport);
        btnHelpSupport.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpAndSupportActivity.class);
            startActivity(intent);
        });

        FrameLayout btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation
        android.widget.LinearLayout navHome = findViewById(R.id.navHome);
        android.widget.LinearLayout navVoice = findViewById(R.id.navVoice);
        android.widget.LinearLayout navSettings = findViewById(R.id.navSettings);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        navVoice.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                voiceCommandHelper.startDirectRecording();
            } else {
                requestMicrophonePermission();
            }
        });

        navSettings.setOnClickListener(v -> {
            // Already on settings page, do nothing
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
        if (voiceCommandHelper != null) {
            voiceCommandHelper.cleanup();
        }
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
                // Already here
                ttsHelper.speak("You are already in Settings");
                return;

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
            
            // Use Handler to delay start activity so TTS can start speaking
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startActivity(finalIntent);
                if (!feature.equalsIgnoreCase("SETTINGS")) {
                    // Don't finish if we are just navigating away, unless we want to clear stack
                    // But Settings is usually on top of Home.
                    // If we go to another feature, we probably want to finish Settings.
                    // But if we go to Home, we might just want to clear top.
                    // Let's finish Settings to keep stack clean.
                    finish();
                }
            }, 1000);
        }
    }
}
