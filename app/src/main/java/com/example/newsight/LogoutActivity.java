package com.example.newsight;

import android.os.Bundle;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class LogoutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Clear session data if any (e.g., SharedPreferences)
        // SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        // prefs.edit().clear().apply();

        // Navigate to Login Screen (MainActivity) and clear back stack
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
