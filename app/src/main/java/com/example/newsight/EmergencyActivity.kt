package com.example.newsight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.newsight.ui.theme.NewSightTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class EmergencyActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private var photoBitmap: Bitmap? = null

    // --- Request camera permission ---
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    // --- Camera launcher using TakePicture() ---
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                photoBitmap = bitmap
                Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to take photo", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()

        setContent {
            NewSightTheme {
                EmergencyScreen(
                    photoBitmap = photoBitmap,
                    onTakePhoto = {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            openCamera()
                        } else {
                            requestCameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onSendClicked = { sendEmergencyAlert(photoBitmap) }
                )
            }
        }
    }

    // --- Open the camera safely with FileProvider ---
    private fun openCamera() {
        val imagesDir = File(getExternalFilesDir(null), "Pictures")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        photoFile = File(imagesDir, "photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)

        // Grant URI permissions for camera apps
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        cameraLauncher.launch(photoUri)
    }

    // --- Send the emergency alert (same logic, simplified here) ---
    private fun sendEmergencyAlert(bitmap: Bitmap?) {
        Toast.makeText(this, "Sending emergency alert...", Toast.LENGTH_SHORT).show()

        if (bitmap == null) {
            Toast.makeText(this, "No photo attached", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val imageBytes = stream.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("latitude", "39.9526") // placeholder
            .addFormDataPart("longitude", "-75.1652") // placeholder
            .addFormDataPart(
                "photo",
                "photo.jpg",
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageBytes)
            )
            .build()

        val request = Request.Builder()
            //.url("http://127.0.0.1:8000/emergency_alert/7")

            .url("https://cis4398-project-newsight-backend.onrender.com/emergency_alert/7")

            //.url("http://192.168.1.105:8000/emergency_alert/7")

            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@EmergencyActivity,
                        "Failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful)
                        Toast.makeText(
                            this@EmergencyActivity,
                            "Alert sent successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    else
                        Toast.makeText(
                            this@EmergencyActivity,
                            "Error: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }
        })
    }

    // --- Composable UI ---
    @Composable
    fun EmergencyScreen(
        photoBitmap: Bitmap?,
        onTakePhoto: () -> Unit,
        onSendClicked: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { onTakePhoto() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Take Photo")
            }

            Spacer(modifier = Modifier.height(18.dp))

            photoBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Photo of Environment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { onSendClicked() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Emergency Alert to Trust Contact")
            }
        }
    }
}

