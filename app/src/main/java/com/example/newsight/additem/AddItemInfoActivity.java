package com.example.newsight.additem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.R;

public class AddItemInfoActivity extends AppCompatActivity {

    FrameLayout btnHome, btnContinue;
    EditText inputGenre, inputNotes;

    String frontPhotoPath, tagFrontPath, tagBackPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item_info);

        frontPhotoPath = getIntent().getStringExtra("front_photo_path");
        tagFrontPath = getIntent().getStringExtra("tag_front_path");
        tagBackPath = getIntent().getStringExtra("tag_back_path");

        btnHome = findViewById(R.id.btnHome);
        btnContinue = findViewById(R.id.btnContinue);
        inputGenre = findViewById(R.id.inputGenre);
        inputNotes = findViewById(R.id.inputNotes);

        btnHome.setOnClickListener(v -> finish());

        btnContinue.setOnClickListener(v -> {
            Intent i = new Intent(this, AddItemReviewActivity.class);
            i.putExtra("front_photo_path", frontPhotoPath);
            i.putExtra("tag_front_path", tagFrontPath);
            i.putExtra("tag_back_path", tagBackPath);
            i.putExtra("genre", inputGenre.getText().toString());
            i.putExtra("notes", inputNotes.getText().toString());
            startActivity(i);
        });
    }
}
