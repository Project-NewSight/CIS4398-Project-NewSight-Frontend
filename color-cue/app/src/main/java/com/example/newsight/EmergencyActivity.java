package com.example.newsight;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EmergencyActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyActivity";
    private static final String BACKEND_URL = "http://192.168.1.254:8000/emergency_alert/7";

    // Increased delays for camera stabilization
    private static final long CAMERA_STABILIZATION_DELAY_MS = 2000; // 2 seconds for camera to focus
    private static final long LOCATION_TIMEOUT_MS = 3000; // 3 seconds max wait for location

    private PreviewView previewView;
    private ImageCapture imageCapture;

    private FusedLocationProviderClient fusedLocationClient;
    private Handler mainHandler;
    private boolean isCapturing = false;

    private TtsHelper ttsHelper;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            startCamera();
                        } else {
                            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

    private final ActivityResultLauncher<String> requestLocationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (!granted) {
                            Log.w(TAG, "Location permission denied");
                        }
                        // Still continue capturing — location just won't be attached
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        previewView = findViewById(R.id.cameraPreview);

        mainHandler = new Handler(Looper.getMainLooper());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize TTS
        ttsHelper = new TtsHelper(this);

        Toast.makeText(this, "Initializing emergency...", Toast.LENGTH_SHORT).show();

        // Ask for camera immediately
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }

        // Request location permission in background
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void startCamera() {
        Toast.makeText(this, "Starting camera...", Toast.LENGTH_SHORT).show();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

                Log.d(TAG, "Camera bound successfully");
                Toast.makeText(this, "Camera ready, stabilizing...", Toast.LENGTH_SHORT).show();

                // 📸 Wait for camera to stabilize before capturing
                // This gives the camera time to focus and adjust exposure
                mainHandler.postDelayed(() -> {
                    if (!isCapturing && !isFinishing()) {
                        Toast.makeText(this, "Capturing photo...", Toast.LENGTH_SHORT).show();
                        takePhoto();
                    }
                }, CAMERA_STABILIZATION_DELAY_MS);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization error", e);
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture is null");
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isCapturing) {
            Log.w(TAG, "Already capturing, ignoring duplicate call");
            return;
        }

        isCapturing = true;

        File photoFile = new File(
                getExternalFilesDir(null),
                "emergency_" + System.currentTimeMillis() + ".jpg"
        );

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        Log.d(TAG, "Taking photo...");

        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed", exc);
                        Toast.makeText(EmergencyActivity.this,
                                "Capture failed: " + exc.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        isCapturing = false;
                        finish();
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Log.d(TAG, "Photo saved successfully: " + photoFile.getAbsolutePath());
                        Toast.makeText(EmergencyActivity.this,
                                "Photo captured, getting location...",
                                Toast.LENGTH_SHORT).show();

                        // Verify file exists and is readable
                        if (!photoFile.exists() || photoFile.length() == 0) {
                            Log.e(TAG, "Photo file is empty or doesn't exist");
                            Toast.makeText(EmergencyActivity.this,
                                    "Photo file error", Toast.LENGTH_SHORT).show();
                            isCapturing = false;
                            finish();
                            return;
                        }

                        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        if (bitmap == null) {
                            Log.e(TAG, "Failed to decode bitmap from file");
                            Toast.makeText(EmergencyActivity.this,
                                    "Failed to process photo", Toast.LENGTH_SHORT).show();
                            isCapturing = false;
                            finish();
                            return;
                        }

                        Log.d(TAG, "Bitmap decoded successfully: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        getLocationAndSend(bitmap, photoFile);
                    }
                }
        );
    }

    private void getLocationAndSend(Bitmap bitmap, File photoFile) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
            Toast.makeText(this, "Photo missing", Toast.LENGTH_SHORT).show();
            isCapturing = false;
            finish();
            return;
        }

        // Set a timeout for location fetching
        final boolean[] locationFetched = {false};

        mainHandler.postDelayed(() -> {
            if (!locationFetched[0]) {
                Log.w(TAG, "Location timeout, sending without location");
                sendAlert(bitmap, null, photoFile);
            }
        }, LOCATION_TIMEOUT_MS);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No location permission");
            locationFetched[0] = true;
            sendAlert(bitmap, null, photoFile);
            return;
        }

        Log.d(TAG, "Fetching location...");
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (!locationFetched[0]) {
                        locationFetched[0] = true;
                        if (location != null) {
                            Log.d(TAG, "Location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                        } else {
                            Log.w(TAG, "Location is null");
                        }
                        sendAlert(bitmap, location, photoFile);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!locationFetched[0]) {
                        locationFetched[0] = true;
                        Log.e(TAG, "Failed to get location", e);
                        sendAlert(bitmap, null, photoFile);
                    }
                });
    }

    private void sendAlert(Bitmap bitmap, Location location, File photoFile) {
        Toast.makeText(this, "Sending emergency alert...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Preparing to send alert...");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageBytes = stream.toByteArray();

        Log.d(TAG, "Image size: " + imageBytes.length + " bytes");

        MultipartBody.Builder form = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("photo", "emergency.jpg",
                        RequestBody.create(imageBytes, MediaType.parse("image/jpeg")));

        if (location != null) {
            form.addFormDataPart("latitude", String.valueOf(location.getLatitude()));
            form.addFormDataPart("longitude", String.valueOf(location.getLongitude()));
            Log.d(TAG, "Including location: " + location.getLatitude() + ", " + location.getLongitude());
        } else {
            Log.d(TAG, "No location data available");
        }

        Request request = new Request.Builder()
                .url(BACKEND_URL)
                .post(form.build())
                .build();

        Log.d(TAG, "Sending request to: " + BACKEND_URL);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network request failed", e);
                runOnUiThread(() -> {
                    String errorMessage = "Send failed: " + e.getMessage();
                    Toast.makeText(EmergencyActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                    // Speak error message and wait for TTS to complete
                    speakAndFinish("Emergency alert failed to send. " + e.getMessage(), photoFile);

                    isCapturing = false;
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Log.d(TAG, "Response received: " + response.code());

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(EmergencyActivity.this,
                                "Emergency alert sent!",
                                Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Alert sent successfully");

                        // Speak success message and wait for TTS to complete
                        speakAndFinish("Emergency alert sent successfully", photoFile);
                    } else {
                        String errorMessage = "Error sending alert: " + response.code();
                        Toast.makeText(EmergencyActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Server returned error: " + response.code());

                        // Speak error message and wait for TTS to complete
                        speakAndFinish("Error sending emergency alert. Server error " + response.code(), photoFile);
                    }

                    isCapturing = false;
                });

                response.close();
            }
        });
    }

    /**
     * Speaks the given message and finishes the activity after TTS completes
     */
    private void speakAndFinish(String message, File photoFile) {
        if (ttsHelper != null) {
            ttsHelper.speak(message);

            // Wait for TTS to complete (estimate based on message length)
            // Average speaking rate is ~150 words per minute, or ~2.5 words per second
            // Add buffer time for safety
            String[] words = message.split("\\s+");
            int wordCount = words.length;
            long estimatedDuration = (long) ((wordCount / 2.5) * 1000) + 1000; // +1 second buffer

            Log.d(TAG, "TTS speaking, estimated duration: " + estimatedDuration + "ms");

            mainHandler.postDelayed(() -> {
                cleanupAndFinish(photoFile);
            }, estimatedDuration);
        } else {
            // If TTS is not available, just finish immediately
            cleanupAndFinish(photoFile);
        }
    }

    /**
     * Cleans up photo file and finishes the activity
     */
    private void cleanupAndFinish(File photoFile) {
        // Clean up photo file
        if (photoFile != null && photoFile.exists()) {
            photoFile.delete();
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
    }
}