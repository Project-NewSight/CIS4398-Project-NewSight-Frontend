package com.example.newsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.newsight.ui.theme.NewSightTheme
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException



class EmergencyActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            NewSightTheme {
                EmergencyScreen { bitmap ->
                    sendEmergencyAlert(bitmap)
                }
            }
        }
    }


    private fun sendEmergencyAlert(bitmap: Bitmap?) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location == null) {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val latitude = location.latitude
            val longitude = location.longitude
            val client = OkHttpClient()

            val imageBytes = if (bitmap != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.toByteArray()
            } else null

            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("latitude", latitude.toString())
                .addFormDataPart("longitude", longitude.toString())
            if (imageBytes != null) {
                requestBodyBuilder.addFormDataPart(
                    "photo",
                    "photo.jpg",
                    RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageBytes)
                )
            }
            val requestBody = requestBodyBuilder.build()
            val request = Request.Builder()
                .url("http://10.0.2.2:8000/emergency_alert/7")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@EmergencyActivity,
                            "Failed:${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful)
                            Toast.makeText(
                                this@EmergencyActivity,
                                "Alert sent Sucessfully",
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
    }

    @Composable
    fun EmergencyScreen(onSendClicked: (Bitmap?) -> Unit) {
        var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap -> photoBitmap = bitmap }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { cameraLauncher.launch() },
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

            Button(onClick = { onSendClicked(photoBitmap) }, Modifier.fillMaxWidth()) {
                Text("Send Emergency Alert to Trust Contact")
            }
        }
    }
}