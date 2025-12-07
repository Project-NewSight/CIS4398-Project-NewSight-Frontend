package com.example.newsight.additem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.R;
import com.example.newsight.camera.CameraCaptureActivity;

public class AddItemFrontActivity extends AppCompatActivity {

    FrameLayout btnHome, btnTakeFront, btnContinue;
    ImageView imgFrontPreview;

    Uri frontUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        btnHome = findViewById(R.id.btnHome);
        btnTakeFront = findViewById(R.id.btnTakeFront);
        btnContinue = findViewById(R.id.btnContinue);
        imgFrontPreview = findViewById(R.id.imgFrontPreview);

        btnHome.setOnClickListener(v -> finish());

        btnTakeFront.setOnClickListener(v -> {
            Intent i = new Intent(this, CameraCaptureActivity.class);
            startActivityForResult(i, AddItemRequestCodes.TAKE_FRONT);
        });

        btnContinue.setOnClickListener(v -> {
            Intent i = new Intent(this, AddItemTagFrontActivity.class);
            if (frontUri != null)
                i.putExtra("front_photo_path", frontUri.toString());
            startActivity(i);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == AddItemRequestCodes.TAKE_FRONT) {
            frontUri = data.getData();
            imgFrontPreview.setImageURI(frontUri);
        }
    }
}
