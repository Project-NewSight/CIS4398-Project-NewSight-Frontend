package com.example.newsight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

    private boolean backendEnabled = true;
    private String activeFeature = "familiar_face";

    private final String SERVER_WS_URL = "ws://100.19.30.133/ws/verify";


    private Button btnNavigation, btnASL, btnObjectDetection, btnStopFeature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);



        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();



        setActiveFeature("familiar_face");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            initCameraAndBackend();
        }
        // Check if an initial feature was passed
        String featureFromIntent = getIntent().getStringExtra("feature");
        if (featureFromIntent != null) {
            setActiveFeature(featureFromIntent); // starts people detection automatically
        }

    }

    private void setActiveFeature(String feature) {
        // write to the FIELD, not a shadowed local
        this.activeFeature = feature;

        Toast.makeText(this,
                (feature != null) ? feature + " feature active" : "Feature streaming stopped",
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Active feature set to: " + this.activeFeature);

        // If WS is already connected, notify backend immediately
        if (wsManager != null && wsManager.isConnected() && feature != null) {
            // pick ONE name and use it consistently. If your WS manager method is setFeature(...), use that:
            wsManager.setFeature(feature);
            // If your method is actually named setActiveFeature(...), then call that instead:
            // wsManager.setActiveFeature(feature);
        }
    }



    private void initCameraAndBackend() {
        if (backendEnabled) {
            wsManager = new WebSocketManager(SERVER_WS_URL, this);


            if (activeFeature != null) {
                wsManager.setFeature(activeFeature);
            }

            wsManager.connect();
        }
        startCameraSafe();
    }


    private void startCameraSafe() {
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
                imageAnalyzer.setAnalyzer(cameraExecutor, new FrameAnalyzer(wsManager, () -> activeFeature));

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);

                Log.i(TAG, "Camera bound successfully");
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
        Log.d(TAG, "WS msg: " + results);
        try {
            org.json.JSONObject obj = new org.json.JSONObject(results);

            // If backend signaled an error, you can still surface it
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
                // Suppress noisy "Ack"/no-match toasts; just log
                Log.d(TAG, "No match (toast suppressed)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Bad JSON", e);
        }
    }



    @Override
    public void onConnectionStatus(boolean isConnected) {
        runOnUiThread(() -> Toast.makeText(this,
                isConnected ? "Connected to backend" : "Backend not available",
                Toast.LENGTH_SHORT).show());
        Log.d(TAG, "WebSocket connected=" + isConnected);

        if (isConnected && wsManager != null && activeFeature != null) {
            // keep method name consistent with what you implement in WebSocketManager
            wsManager.setFeature(activeFeature);
            // or: wsManager.setActiveFeature(activeFeature);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
