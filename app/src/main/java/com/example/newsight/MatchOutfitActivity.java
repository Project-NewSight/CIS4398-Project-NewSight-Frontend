package com.example.newsight;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.camera.CameraCaptureActivity;

public class MatchOutfitActivity extends AppCompatActivity {

    FrameLayout btnBack, btnHome, btnAddFirstImage, btnAddSecondImage, btnMic;
    ImageView imgPreview1, imgPreview2;

    Uri firstPhotoUri = null;
    Uri secondPhotoUri = null;

    private static final int REQ_FIRST_IMAGE = 101;
    private static final int REQ_SECOND_IMAGE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_outfit);

        // 🔵 Top Buttons
        btnBack = findViewById(R.id.btnBack);
        btnHome = findViewById(R.id.btnHome);

        // 🔶 Image Buttons
        btnAddFirstImage = findViewById(R.id.btnAddFirstImage);
        btnAddSecondImage = findViewById(R.id.btnAddSecondImage);

        // 🔶 Image Previews
        imgPreview1 = findViewById(R.id.imgPreview1);
        imgPreview2 = findViewById(R.id.imgPreview2);

        // 🎤 Mic
        btnMic = findViewById(R.id.btnMic);

        // 🔙 Back Button
        btnBack.setOnClickListener(v -> finish());

        // 🏠 Home Button
        btnHome.setOnClickListener(v -> finish());

        // 📸 Add Image 1
        btnAddFirstImage.setOnClickListener(v -> {
            Intent i = new Intent(this, CameraCaptureActivity.class);
            startActivityForResult(i, REQ_FIRST_IMAGE);
        });

        // 📸 Add Image 2
        btnAddSecondImage.setOnClickListener(v -> {
            Intent i = new Intent(this, CameraCaptureActivity.class);
            startActivityForResult(i, REQ_SECOND_IMAGE);
        });

        // 🎤 Voice Commands
        btnMic.setOnClickListener(v -> {
            Intent i = new Intent(this, VoiceCommandActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);

        if (res != RESULT_OK || data == null) return;

        Uri photoUri = data.getData();

        if (req == REQ_FIRST_IMAGE) {
            firstPhotoUri = photoUri;
            imgPreview1.setImageURI(firstPhotoUri);
        }

        if (req == REQ_SECOND_IMAGE) {
            secondPhotoUri = photoUri;
            imgPreview2.setImageURI(secondPhotoUri);
        }

        // If both are selected → Go to matching screen
        if (firstPhotoUri != null && secondPhotoUri != null) {
            Intent i = new Intent(this, MatchOutfitResultActivity.class);
            i.putExtra("photo_uri_1", firstPhotoUri.toString());
            i.putExtra("photo_uri_2", secondPhotoUri.toString());
            startActivity(i);
        }
    }
}
