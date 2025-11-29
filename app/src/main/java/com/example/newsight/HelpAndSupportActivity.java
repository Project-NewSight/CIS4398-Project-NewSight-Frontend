package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;

public class HelpAndSupportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_and_support);

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        FrameLayout btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            startActivity(intent);
        });

        FrameLayout btnContactSupport = findViewById(R.id.btnContactSupport);
        btnContactSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@visionaid.com"));
            startActivity(intent);
        });

        FrameLayout btnDocumentation = findViewById(R.id.btnDocumentation);
        btnDocumentation.setOnClickListener(v -> {
            // Placeholder for documentation link
            // Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://visionaid.com/docs"));
            // startActivity(intent);
        });
    }
}
