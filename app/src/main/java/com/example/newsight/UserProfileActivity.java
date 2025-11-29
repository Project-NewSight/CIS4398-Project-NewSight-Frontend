package com.example.newsight;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class UserProfileActivity extends AppCompatActivity {
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        FrameLayout btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            startActivity(intent);
        });

        // Edit Profile Logic
        FrameLayout btnEditProfile = findViewById(R.id.btnEditProfile);
        LinearLayout formEditProfile = findViewById(R.id.formEditProfile);
        TextView textName = findViewById(R.id.textName);
        TextView textEmail = findViewById(R.id.textEmail);
        EditText inputName = findViewById(R.id.inputName);
        EditText inputEmail = findViewById(R.id.inputEmail);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);

        btnEditProfile.setOnClickListener(v -> {
            isEditing = !isEditing;
            formEditProfile.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        });

        btnSaveProfile.setOnClickListener(v -> {
            // Save changes
            textName.setText(inputName.getText().toString());
            textEmail.setText(inputEmail.getText().toString());
            
            // Hide form
            isEditing = false;
            formEditProfile.setVisibility(View.GONE);
        });
    }
}
