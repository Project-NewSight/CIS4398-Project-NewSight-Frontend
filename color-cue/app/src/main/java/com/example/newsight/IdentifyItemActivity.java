package com.example.newsight;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.camera.CameraCaptureActivity;

public class IdentifyItemActivity extends AppCompatActivity {

    FrameLayout btnHome, btnIdentify, btnMic;
    ImageView imgPreview;
    Uri itemUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_item);

        btnHome = findViewById(R.id.btnHome);
        btnIdentify = findViewById(R.id.btnIdentify);
        btnMic = findViewById(R.id.btnMic);
        imgPreview = findViewById(R.id.imgPreview);

        btnHome.setOnClickListener(v -> finish());

        btnIdentify.setOnClickListener(v -> {
            Intent i = new Intent(this, CameraCaptureActivity.class);
            startActivityForResult(i, 200);   // new request code
        });

        btnMic.setOnClickListener(v -> {
            Intent i = new Intent(this, VoiceCommandActivity.class);
            startActivity(i);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);

        if (req == 200 && res == RESULT_OK) {
            itemUri = data.getData();
            imgPreview.setImageURI(itemUri);

            Intent i = new Intent(this, IdentifyItemResultActivity.class);
            i.putExtra("photo_uri", itemUri.toString());
            startActivity(i);
        }
    }
}
