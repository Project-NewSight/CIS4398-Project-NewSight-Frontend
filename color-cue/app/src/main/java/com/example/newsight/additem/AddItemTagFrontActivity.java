package com.example.newsight.additem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;   // ✅ FIXED IMPORT

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.R;
import com.example.newsight.camera.CameraCaptureActivity;

public class AddItemTagFrontActivity extends AppCompatActivity {

    FrameLayout btnBack, btnHome, btnScanTagFront, btnContinue;
    ImageView imgTagFrontPreview;

    String frontPhotoPath;
    Uri tagFrontUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item_tag_front);

        frontPhotoPath = getIntent().getStringExtra("front_photo_path");

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnHome = findViewById(R.id.btnHome);
        btnScanTagFront = findViewById(R.id.btnScanTagFront);
        btnContinue = findViewById(R.id.btnContinue);
        imgTagFrontPreview = findViewById(R.id.imgTagFrontPreview);

        btnHome.setOnClickListener(v -> finish());

        btnScanTagFront.setOnClickListener(v -> {
            Toast.makeText(this, "Scan Tag Front clicked", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, CameraCaptureActivity.class);
            startActivityForResult(i, AddItemRequestCodes.TAKE_TAG_FRONT);
        });

        btnContinue.setOnClickListener(v -> {
            Intent i = new Intent(this, AddItemTagBackActivity.class);
            i.putExtra("front_photo_path", frontPhotoPath);
            if (tagFrontUri != null)
                i.putExtra("tag_front_path", tagFrontUri.toString());
            startActivity(i);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == AddItemRequestCodes.TAKE_TAG_FRONT) {
            tagFrontUri = data.getData();
            imgTagFrontPreview.setImageURI(tagFrontUri);
        }
    }
}
