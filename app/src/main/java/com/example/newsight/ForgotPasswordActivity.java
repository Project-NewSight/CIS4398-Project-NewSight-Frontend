package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnReset;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        btnReset = findViewById(R.id.btnReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Initialize label for highlighting
        TextView tvEmailLabel = findViewById(R.id.tvEmailLabel);

        // Focus listener for label highlighting
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            tvEmailLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, 
                hasFocus ? R.color.primary : R.color.muted_foreground));
        });

        // Reset button click
        btnReset.setOnClickListener(v -> handleReset());

        // Back to login link click
        tvBackToLogin.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void handleReset() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement actual password reset logic here
        Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();

        // Go back to login after a short delay
        etEmail.postDelayed(() -> finish(), 2000);
    }
}
