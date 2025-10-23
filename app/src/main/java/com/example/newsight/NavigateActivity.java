package com.example.newsight;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.content.Intent;
import android.widget.Toast;

public class NavigateActivity extends AppCompatActivity{
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, HomeActivity.class);
            startActivity(intent);
        });
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            Toast.makeText(this, "Voice command activated", Toast.LENGTH_SHORT).show();
        });

        FrameLayout btnStartNav = findViewById(R.id.btnStartNav);
        btnStartNav.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, StartNavigationActivity.class);
            startActivity(intent);
        });

        FrameLayout btnSelectDest = findViewById(R.id.btnSelectDest);
        btnSelectDest.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, SelectDestinationActivity.class);
            startActivity(intent);
        });

        FrameLayout btnWhereAmI = findViewById(R.id.btnWhereAmI);
        btnWhereAmI.setOnClickListener(v -> {
            Intent intent = new Intent(NavigateActivity.this, WhereAmIActivity.class);
            startActivity(intent);
        });
    }
}