package com.example.newsight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements WebSocketManager.WsListener {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private WebSocketManager wsManager;

    // Feature
    private String activeFeature = "familiar_face";   // default behavior from repo
    private FeatureProvider featureProvider;

    // Backend control
    private boolean backendEnabled = true;

    // Server URL can now come from config file (local enhancement)
    private final String SERVER_WS_URL = config.WEBSOCKET_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Provide the feature to analyzers
        featureProvider = () -> activeFeature;

        // Camera permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
            return;
        }

        initializeFeatureFromIntent();
        initCameraAndBackend();
    }

    /**
     * Reads "feature" from Intent if passed and overrides the default.
     */
    private void initializeFeatureFromIntent() {
        String featureFromIntent = getIntent().getStringExtra("feature");
        if (featureFromIntent != null) {
            setActiveFeature(featureFromIntent);
        } else {
            setActiveFeature(activeFeature); // Default from repo
        }
    }

    /**
     * Sets feature consistently and informs the backend if connected.
     */
    private void setActiveFeature(String feature) {
        activeFeature = feature;

        Toast.makeText(
                this,
                feature != null ? feature + " feature active" : "Feature streaming stopped",
                Toast.LENGTH_SHORT
        ).show();

        Log.d(TAG, "Active feature set to: " + activeFeature);

        // Tell backend if connected
        if (wsManager != null && wsManager.isConnected() && activeFeature != null) {
            wsManager.setFeature(activeFeature);
        }
    }

    /**
     * Initialize WebSocket + Camera.
     */
    private void initCameraAndBackend() {

        if (backendEnabled) {
            wsManager = new WebSocketManager(SERVER_WS_URL, this);
            wsManager.connect();
        }

        startCameraSafe();
    }

    /**
     * Binds camera preview + analyzer safely.
     */
    private void startCameraSafe() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Build analyzer
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                if ("asl_detection".equals(activeFeature)) {
                    imageAnalysis.setAnalyzer(
                            cameraExecutor,
                            new ASLFrameAnalyzer(wsManager, featureProvider)
                    );
                } else {
                    imageAnalysis.setAnalyzer(
                            cameraExecutor,
                            new FrameAnalyzer(wsManager, featureProvider)
                    );
                }

                CameraSelector cameraSelector =
                        new CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

                Log.i(TAG, "Camera bound successfully");

            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
                Toast.makeText(this, "Camera setup failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Handles AI output exactly like GitHub version (match, name, ok, errors).
     */
    @Override
    public void onResultsReceived(String results) {
        Log.d(TAG, "WS msg: " + results);

        try {
            org.json.JSONObject obj = new org.json.JSONObject(results);

            if (!obj.optBoolean("ok", false)) {
                final String err = obj.optString("error", "unknown");
                runOnUiThread(() ->
                        Toast.makeText(this, "Backend error: " + err, Toast.LENGTH_SHORT).show());
                return;
            }

            boolean match = obj.optBoolean("match", false);
            if (match) {
                final String name = obj.optString("contactName", "Unknown");
                runOnUiThread(() ->
                        Toast.makeText(this, "Match: " + name, Toast.LENGTH_SHORT).show());
            } else {
                Log.d(TAG, "No match (toast suppressed)");
            }

        } catch (Exception e) {
            Log.e(TAG, "Bad JSON", e);
        }
    }

    /**
     * WebSocket connected/disconnected.
     */
    @Override
    public void onConnectionStatus(boolean isConnected) {

        runOnUiThread(() -> Toast.makeText(
                this,
                isConnected ? "Connected to backend" : "Backend unavailable",
                Toast.LENGTH_SHORT
        ).show());

        Log.d(TAG, "WebSocket connected=" + isConnected);

        if (isConnected && wsManager != null && activeFeature != null) {
            wsManager.setFeature(activeFeature);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CAMERA_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            initializeFeatureFromIntent();
            initCameraAndBackend();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (wsManager != null) wsManager.disconnect();
    }
}
