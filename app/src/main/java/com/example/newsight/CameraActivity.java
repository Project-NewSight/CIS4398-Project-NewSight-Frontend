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
    private String activeFeature = null;

    private final String SERVER_WS_URL = "ws://10.0.0.23:8000/ws";
    //ws://10.109.98.242:8000/ws

    private Button btnNavigation, btnASL, btnObjectDetection, btnStopFeature, btnReadText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        btnNavigation = findViewById(R.id.btnNavigation);
        btnASL = findViewById(R.id.btnASL);
        btnObjectDetection = findViewById(R.id.btnObjectDetection);
        btnStopFeature = findViewById(R.id.btnStopFeature);
        btnReadText = findViewById(R.id.btnReadText);

        // ---------- BUTTON LISTENERS ----------
        btnReadText.setOnClickListener(v -> setActiveFeature("read_text"));
        btnNavigation.setOnClickListener(v -> setActiveFeature("navigation"));
        btnASL.setOnClickListener(v -> setActiveFeature("asl_detection"));
        btnObjectDetection.setOnClickListener(v -> setActiveFeature("object_detection"));
        btnStopFeature.setOnClickListener(v -> setActiveFeature(null));


        // ---------- APPLY FEATURE *BEFORE* CAMERA STARTS ----------
        String featureFromIntent = getIntent().getStringExtra("feature");
        if (featureFromIntent != null) {
            setActiveFeature(featureFromIntent);
            Log.d("FEATURE", "Activated feature: " + featureFromIntent);
        } else {
            // Default mode if nothing was selected
            setActiveFeature("read_text");
        }


        // ---------- NOW check camera permission and start camera ----------
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            initCameraAndBackend();
        }
    }


    private void setActiveFeature(String feature) {
        activeFeature = feature;
        String message = (feature != null) ? feature + " feature active" : "Feature streaming stopped";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void initCameraAndBackend() {
        Log.d(TAG, "initCameraAndBackend called. backendEnabled is: " + backendEnabled);

        if (backendEnabled) {
            wsManager = new WebSocketManager(SERVER_WS_URL, this);
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
        runOnUiThread(() -> Toast.makeText(this,
                "AI result: " + results.substring(0, Math.min(results.length(), 20)) + "...",
                Toast.LENGTH_SHORT).show());
    }


    @Override
    public void onConnectionStatus(boolean isConnected) {
        runOnUiThread(() -> Toast.makeText(this,
                isConnected ? "Connected to backend" : "Backend not available",
                Toast.LENGTH_SHORT).show());
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (wsManager != null) wsManager.disconnect();
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
}
