package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONException;
import org.json.JSONObject;

public class TrustedContactsActivity extends AppCompatActivity {
    private static final String TAG = "TrustedContactsActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int ADD_CONTACT_REQUEST = 1;
    private VoiceCommandHelper voiceCommandHelper;
    private TtsHelper ttsHelper;
    private LinearLayout layoutContactsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_contacts);

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

        // Auto-start wake word detection
        if (checkMicrophonePermission()) {
            voiceCommandHelper.startWakeWordDetection();
        }

        layoutContactsList = findViewById(R.id.layout_contacts_list);

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        FrameLayout btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            startActivity(intent);
        });

        FrameLayout btnAddContact = findViewById(R.id.btn_add_contact);
        btnAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddContactActivity.class);
            startActivityForResult(intent, ADD_CONTACT_REQUEST);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            String name = data.getStringExtra("name");
            String phone = data.getStringExtra("phone");
            String relationship = data.getStringExtra("relationship");
            addContactView(name, phone, relationship);
        }
    }

    private void addContactView(String name, String phone, String relationship) {
        View contactView = LayoutInflater.from(this).inflate(R.layout.item_contact_card, layoutContactsList, false);
        
        TextView tvName = contactView.findViewById(R.id.tvName);
        TextView tvPhone = contactView.findViewById(R.id.tvPhone);
        TextView tvRelationship = contactView.findViewById(R.id.tvRelationship);
        FrameLayout btnDelete = contactView.findViewById(R.id.btnDelete);

        tvName.setText(name);
        tvPhone.setText(phone);
        tvRelationship.setText(relationship);

        btnDelete.setOnClickListener(v -> layoutContactsList.removeView(contactView));

        layoutContactsList.addView(contactView);
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
            
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startActivity(finalIntent);
                finish();
            }, 1000);
        }
    }
}
