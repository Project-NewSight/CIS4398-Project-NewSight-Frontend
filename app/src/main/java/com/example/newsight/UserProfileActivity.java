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
            String newName = inputName.getText().toString();
            String newEmail = inputEmail.getText().toString();
            
            textName.setText(newName);
            textEmail.setText(newEmail);
            
            // Persist changes
            getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .edit()
                .putString("user_name", newName)
                .putString("user_email", newEmail)
                .apply();
            
            // Hide form
            isEditing = false;
            formEditProfile.setVisibility(View.GONE);
        });

        // Load User Data
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String email = prefs.getString("user_email", "No Email");
        String name = prefs.getString("user_name", "Guest User");
        
        textName.setText(name);
        textEmail.setText(email);
        inputName.setText(name);
        inputEmail.setText(email);
    }
}
