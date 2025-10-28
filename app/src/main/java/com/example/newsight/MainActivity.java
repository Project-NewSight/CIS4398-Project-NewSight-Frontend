package com.example.newsight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Debug log to confirm activity loaded
        Toast.makeText(this, "MainActivity started", Toast.LENGTH_SHORT).show();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Welcome " + email, Toast.LENGTH_SHORT).show();

                // ✅ Go straight to the Home screen
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // close login so user can't go "back" to it
            }
        });
    }
}


    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final String TAG = "MainActivity";

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private WebSocketManager wsManager;

/*
    EditText etEmail, etPassword;
    Button btnLogin, btnOpenCamera;


    private EditText etEmail, etPassword;
    private Button btnLogin, btnOpenCamera;
    private FrameLayout cameraContainer;

    private boolean isLoggedIn = false;

    private String currentFeature = "navigation"; // default feature

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Toast.makeText(this, "MainActivity started", Toast.LENGTH_SHORT).show();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // login layout

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnOpenCamera = findViewById(R.id.btnOpenCamera);
        cameraContainer = findViewById(R.id.cameraContainer);

        btnOpenCamera.setVisibility(View.GONE);
        cameraContainer.setVisibility(View.GONE);

        wsManager = null; // placeholder for WebSocketManager
        cameraExecutor = Executors.newSingleThreadExecutor();


        btnLogin.setOnClickListener(v -> handleLogin());

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                isLoggedIn = true;
                Toast.makeText(MainActivity.this, "Logged in as " + email, Toast.LENGTH_SHORT).show();

                // Show camera button on successful login
                btnLogin.setVisibility(View.GONE);
                etEmail.setVisibility(View.GONE);
                etPassword.setVisibility(View.GONE);
                btnOpenCamera.setVisibility(View.VISIBLE);

                // Optional: disable login to prevent multiple clicks
                btnLogin.setEnabled(false);
                etEmail.setEnabled(false);
                etPassword.setEnabled(false);

                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });


        btnOpenCamera.setOnClickListener(v -> checkCameraPermission());
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mock login success
        isLoggedIn = true;
        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();

        // Hide login UI
        etEmail.setVisibility(View.GONE);
        etPassword.setVisibility(View.GONE);
        btnLogin.setVisibility(View.GONE);

        // Show camera button
        btnOpenCamera.setVisibility(View.VISIBLE);
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
        cameraContainer.setVisibility(View.VISIBLE);

        if (previewView == null) {
            previewView = new PreviewView(this);
            cameraContainer.addView(previewView,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));
        }

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

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
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
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (wsManager != null) wsManager.disconnect();
    }
}
*/