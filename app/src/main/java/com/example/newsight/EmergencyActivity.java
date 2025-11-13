package com.example.newsight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EmergencyActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private File photoFile;
    private Uri photoUri;
    private Bitmap photoBitmap;

    private Button btnTakePhoto;
    private Button btnSendAlert;
    private ImageView imgPreview;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openCamera();
                else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String> requestLocationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted)
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                    photoBitmap = bitmap;
                    imgPreview.setImageBitmap(bitmap);
                    Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to take photo", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //Home
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });

        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnSendAlert = findViewById(R.id.btn_send_alert);
        imgPreview = findViewById(R.id.img_preview);

        // Request location permission upfront if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA);
            }
        });

        btnSendAlert.setOnClickListener(v -> sendEmergencyAlert(photoBitmap));
    }

    private void openCamera() {
        File imagesDir = new File(getExternalFilesDir(null), "Pictures");
        if (!imagesDir.exists()) imagesDir.mkdirs();

        photoFile = new File(imagesDir, "photo_" + System.currentTimeMillis() + ".jpg");
        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        for (android.content.pm.ResolveInfo resolvedInfo :
                getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            String packageName = resolvedInfo.activityInfo.packageName;
            grantUriPermission(packageName, photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        cameraLauncher.launch(photoUri);
    }


    private void sendEmergencyAlert(Bitmap bitmap) {
        Toast.makeText(this, "Sending emergency alert...", Toast.LENGTH_SHORT).show();

        if (bitmap == null) {
            Toast.makeText(this, "No photo attached", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Check and get location dynamically
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        sendAlertRequest(bitmap, latitude, longitude);
                    } else {
                        Toast.makeText(this, "Unable to retrieve location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendAlertRequest(Bitmap bitmap, double latitude, double longitude) {
        OkHttpClient client = new OkHttpClient();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageBytes = stream.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("latitude", String.valueOf(latitude))
                .addFormDataPart("longitude", String.valueOf(longitude))
                .addFormDataPart("photo", "photo.jpg",
                        RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                .build();

        Request request = new Request.Builder()
                .url("https://cis4398-project-newsight-backend.onrender.com/emergency_alert/7")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(EmergencyActivity.this,
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(EmergencyActivity.this,
                                "Alert sent successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EmergencyActivity.this,
                                "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Open Camera (new)
        FrameLayout btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnOpenCamera.setOnClickListener(v -> {
            // Launch MainActivity and open camera directly
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("feature", "emergency"); // signal MainActivity to open camera immediately
            startActivity(intent);
        });
    }
}


