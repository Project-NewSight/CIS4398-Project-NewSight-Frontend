package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnSignup;
    private TextView tvAlreadyHaveAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);

        // Signup button click
        btnSignup.setOnClickListener(v -> handleSignup());

        // Already have account link click
        tvAlreadyHaveAccount.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void handleSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement actual signup logic here
        Toast.makeText(this, "Account created for " + name, Toast.LENGTH_SHORT).show();

        // Navigate to loading screen
        Intent intent = new Intent(SignupActivity.this, LoadingActivity.class);
        startActivity(intent);
        finish();
    }
}
