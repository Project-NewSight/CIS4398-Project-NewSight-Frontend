package com.example.newsight.additem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.R;
import com.example.newsight.camera.CameraCaptureActivity;

public class AddItemTagBackActivity extends AppCompatActivity {

    FrameLayout btnHome, btnScanTagBack, btnContinue;
    ImageView imgTagBackPreview;

    String frontPhotoPath, tagFrontPath;
    Uri tagBackUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item_tag_back);

        frontPhotoPath = getIntent().getStringExtra("front_photo_path");
        tagFrontPath = getIntent().getStringExtra("tag_front_path");

        btnHome = findViewById(R.id.btnHome);
        btnScanTagBack = findViewById(R.id.btnScanTagBack);
        btnContinue = findViewById(R.id.btnContinue);
        imgTagBackPreview = findViewById(R.id.imgTagBackPreview);

        btnHome.setOnClickListener(v -> finish());

        btnScanTagBack.setOnClickListener(v -> {
            Intent i = new Intent(this, CameraCaptureActivity.class);
            startActivityForResult(i, AddItemRequestCodes.TAKE_TAG_BACK);
        });

        btnContinue.setOnClickListener(v -> {
            Intent i = new Intent(this, AddItemInfoActivity.class);
            i.putExtra("front_photo_path", frontPhotoPath);
            i.putExtra("tag_front_path", tagFrontPath);
            if (tagBackUri != null)
                i.putExtra("tag_back_path", tagBackUri.toString());
            startActivity(i);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == AddItemRequestCodes.TAKE_TAG_BACK) {
            tagBackUri = data.getData();
            imgTagBackPreview.setImageURI(tagBackUri);
        }
    }
}
