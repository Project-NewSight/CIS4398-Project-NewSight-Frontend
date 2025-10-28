//package com.example.newsight;
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.FrameLayout;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.camera.core.CameraSelector;
//import androidx.camera.core.ImageAnalysis;
//import androidx.camera.core.Preview;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.camera.view.PreviewView;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.google.common.util.concurrent.ListenableFuture;
//
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MainActivity extends AppCompatActivity implements WebSocketManager.WsListener {
//
//    private static final int REQUEST_CAMERA_PERMISSION = 1001;
//    private static final String TAG = "MainActivity";
//
//    private PreviewView previewView;
//    private ExecutorService cameraExecutor;
//    private WebSocketManager wsManager;
//
//    private EditText etEmail, etPassword;
//    private Button btnLogin, btnOpenCamera;
//    private FrameLayout cameraContainer;
//
//    private boolean isLoggedIn = false;
//    private String currentFeature = "navigation"; // default
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        etEmail = findViewById(R.id.etEmail);
//        etPassword = findViewById(R.id.etPassword);
//        btnLogin = findViewById(R.id.btnLogin);
//        btnOpenCamera = findViewById(R.id.btnOpenCamera);
//        cameraContainer = findViewById(R.id.cameraContainer);
//
//        btnOpenCamera.setVisibility(android.view.View.GONE);
//        cameraContainer.setVisibility(android.view.View.GONE);
//
//        cameraExecutor = Executors.newSingleThreadExecutor();
//
//        btnLogin.setOnClickListener(v -> handleLogin());
//        btnOpenCamera.setOnClickListener(v -> checkCameraPermission());
//    }
//
//    private void handleLogin() {
//        String email = etEmail.getText().toString().trim();
//        String password = etPassword.getText().toString().trim();
//
//        if (email.isEmpty() || password.isEmpty()) {
//            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Simulated login success
//        isLoggedIn = true;
//        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
//
//        // 🔌 Initialize WebSocketManager
//        String wsUrl = "wss://your-backend-url/ws"; // TODO: replace with your backend URL
//        wsManager = new WebSocketManager(wsUrl, this);
//        wsManager.connect();
//
//        // Hide login UI
//        etEmail.setVisibility(android.view.View.GONE);
//        etPassword.setVisibility(android.view.View.GONE);
//        btnLogin.setVisibility(android.view.View.GONE);
//        btnOpenCamera.setVisibility(android.view.View.VISIBLE);
//    }
//
//    private void checkCameraPermission() {
//        if (!isLoggedIn) {
//            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
//        } else {
//            openCamera();
//        }
//    }
//
//    private void openCamera() {
//        cameraContainer.setVisibility(android.view.View.VISIBLE);
//
//        if (previewView == null) {
//            previewView = new PreviewView(this);
//            cameraContainer.addView(previewView,
//                    new FrameLayout.LayoutParams(
//                            FrameLayout.LayoutParams.MATCH_PARENT,
//                            FrameLayout.LayoutParams.MATCH_PARENT));
//        }
//
//        startCamera();
//    }
//
//    private void startCamera() {
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
//                ProcessCameraProvider.getInstance(this);
//
//        cameraProviderFuture.addListener(() -> {
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//
//                Preview preview = new Preview.Builder().build();
//                preview.setSurfaceProvider(previewView.getSurfaceProvider());
//
//                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
//
//                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
//                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                        .build();
//
//                FrameAnalyzer.FeatureProvider provider = () -> currentFeature;
//
//                // ✅ Pass active WebSocketManager
//                imageAnalysis.setAnalyzer(cameraExecutor, new FrameAnalyzer(wsManager, provider));
//
//                cameraProvider.unbindAll();
//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
//
//            } catch (ExecutionException | InterruptedException e) {
//                Log.e(TAG, "Camera initialization failed", e);
//            }
//        }, ContextCompat.getMainExecutor(this));
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CAMERA_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                openCamera();
//            } else {
//                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
//            }
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        cameraExecutor.shutdown();
//        if (wsManager != null) wsManager.disconnect();
//    }
//
//    // 🔹 WebSocketManager.WsListener implementation
//    @Override
//    public void onResultsReceived(String results) {
//        runOnUiThread(() -> {
//            Log.d(TAG, "Backend results: " + results);
//            Toast.makeText(this, "Backend: " + results, Toast.LENGTH_SHORT).show();
//        });
//    }
//
//    @Override
//    public void onConnectionStatus(boolean isConnected) {
//        runOnUiThread(() -> {
//            String status = isConnected ? "Connected to backend" : "Disconnected";
//            Log.i(TAG, status);
//            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
//        });
//    }
//}
