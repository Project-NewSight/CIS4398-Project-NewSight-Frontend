package com.example.newsight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadTextActivity extends AppCompatActivity implements WebSocketManager.WsListener, TextToSpeech.OnInitListener {

    private static final String TAG = "ReadTextActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 102;
    private static final String SERVER_WS_URL = "ws://10.0.0.23:8000/ws"; // Use your computer's IP

    private PreviewView previewView;
    private TextView tvAiStatus;
    private ExecutorService cameraExecutor;
    private WebSocketManager wsManager;
    private TextToSpeech tts;

    private String lastDisplayedText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_text);

        previewView = findViewById(R.id.previewView);
        tvAiStatus = findViewById(R.id.tv_ai_status);
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Initialize TextToSpeech engine
        tts = new TextToSpeech(this, this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            initCameraAndBackend();
        }
    }

    private void initCameraAndBackend() {
        wsManager = new WebSocketManager(SERVER_WS_URL, this);
        wsManager.connect();
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

                ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                
                // The feature is always "read_text" in this activity
                imageAnalyzer.setAnalyzer(cameraExecutor, new FrameAnalyzer(wsManager, () -> "read_text"));

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);

                Log.i(TAG, "Camera bound successfully for text detection.");
            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
                Toast.makeText(this, "Camera setup failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onResultsReceived(String results) {
        runOnUiThread(() -> {
            // Always log the raw result
            Log.d(TAG, "Raw backend result: " + results);

            try {
                JSONObject jsonObject = new JSONObject(results);
                String detectedText = null;

                if (jsonObject.has("stable_text") && !jsonObject.isNull("stable_text")) {
                    detectedText = jsonObject.getString("stable_text").trim();
                }

                // If new, valid text is found and it's different from the last one shown
                if (detectedText != null && !detectedText.isEmpty() && !detectedText.equals(lastDisplayedText)) {
                    lastDisplayedText = detectedText; // Update the state
                    String speech = "Text detected: " + lastDisplayedText;
                    speak(speech);
                    Toast.makeText(this, lastDisplayedText, Toast.LENGTH_SHORT).show();
                } 
                // If no valid text is found, AND we have never shown any text before
                else if ((detectedText == null || detectedText.isEmpty()) && lastDisplayedText == null) {
                    speak("Reading...");
                    Toast.makeText(this, "Reading...", Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse backend JSON: " + results, e);
                if (lastDisplayedText == null) {
                    speak("Reading...");
                    Toast.makeText(this, "Reading...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onConnectionStatus(boolean isConnected) {
        runOnUiThread(() -> {
            if (tvAiStatus != null) {
                tvAiStatus.setText(isConnected ? "AI Status: Connected" : "AI Status: Disconnected");
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS language is not supported.");
            } else {
                Log.i(TAG, "TTS engine initialized successfully.");
            }
        } else {
            Log.e(TAG, "TTS initialization failed.");
        }
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        cameraExecutor.shutdown();
        if (wsManager != null) {
            wsManager.disconnect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCameraAndBackend();
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
