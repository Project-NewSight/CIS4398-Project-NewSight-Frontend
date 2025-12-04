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

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private WebSocketManager wsManager;
    private String activeFeature = null; // ASL, OCR, Object Detection, etc.
    private FeatureProvider featureProvider;
    private final String SERVER_WS_URL = config.WEBSOCKET_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 1️⃣ Create a working FeatureProvider
        featureProvider = () -> activeFeature; // <<< FIXED

        // 2️⃣ Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

    private void initializeFeatureFromIntent() {
        activeFeature = getIntent().getStringExtra("feature");
        if (activeFeature != null) {
            Toast.makeText(this, activeFeature + " enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void initCameraAndBackend() {
        wsManager = new WebSocketManager(SERVER_WS_URL, this);
        wsManager.connect();
        startCameraSafe();
    }

    private void startCameraSafe() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 3️⃣ Create ONE analyzer based on the feature
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                if ("asl_detection".equals(activeFeature)) {
                    imageAnalysis.setAnalyzer(
                            cameraExecutor,
                            new ASLFrameAnalyzer(wsManager, featureProvider) /// << USES ASL
                    );
                } else {
                    imageAnalysis.setAnalyzer(
                            cameraExecutor,
                            new FrameAnalyzer(wsManager, featureProvider) /// << NORMAL
                    );
                }

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis /// <<< FIXED (no more imageAnalyzer)
                );

            } catch (Exception e) {
                Toast.makeText(this, "Camera failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeFeatureFromIntent();
            initCameraAndBackend();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onResultsReceived(String results) {
        runOnUiThread(() -> Toast.makeText(
                this,
                "AI: " + results,
                Toast.LENGTH_SHORT
        ).show());
    }

    @Override
    public void onConnectionStatus(boolean isConnected) {
        runOnUiThread(() -> Toast.makeText(
                this,
                isConnected ? "Connected to backend" : "Backend unavailable",
                Toast.LENGTH_SHORT
        ).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (wsManager != null) wsManager.disconnect();
    }

}