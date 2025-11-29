package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.view.MotionEvent;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import androidx.core.content.ContextCompat;

public class SignupActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvAlreadyHaveAccount, tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);
        tvError = findViewById(R.id.tvError);

        // Initialize labels for highlighting
        TextView tvNameLabel = findViewById(R.id.tvNameLabel);
        TextView tvEmailLabel = findViewById(R.id.tvEmailLabel);
        TextView tvPasswordLabel = findViewById(R.id.tvPasswordLabel);
        TextView tvConfirmPasswordLabel = findViewById(R.id.tvConfirmPasswordLabel);

        // Focus listeners for label highlighting
        etName.setOnFocusChangeListener((v, hasFocus) -> {
            tvNameLabel.setTextColor(ContextCompat.getColor(this, 
                hasFocus ? R.color.primary : R.color.muted_foreground));
        });

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            tvEmailLabel.setTextColor(ContextCompat.getColor(this, 
                hasFocus ? R.color.primary : R.color.muted_foreground));
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            tvPasswordLabel.setTextColor(ContextCompat.getColor(this, 
                hasFocus ? R.color.primary : R.color.muted_foreground));
        });

        etConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            tvConfirmPasswordLabel.setTextColor(ContextCompat.getColor(this, 
                hasFocus ? R.color.primary : R.color.muted_foreground));
        });

        // Setup password toggles
        setupPasswordToggle(etPassword);
        setupPasswordToggle(etConfirmPassword);

        // Signup button click
        btnSignup.setOnClickListener(v -> handleSignup());

        // Already have account link click
        tvAlreadyHaveAccount.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void setupPasswordToggle(EditText editText) {
        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    int selection = editText.getSelectionEnd();
                    if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                        editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
                    } else {
                        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
                    }
                    editText.setSelection(selection);
                    // Re-apply tint
                    for (android.graphics.drawable.Drawable drawable : editText.getCompoundDrawables()) {
                        if (drawable != null) {
                            drawable.setTint(ContextCompat.getColor(SignupActivity.this, R.color.muted_foreground));
                        }
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void handleSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Reset error visibility
        tvError.setVisibility(android.view.View.GONE);

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            tvError.setText("Passwords do not match");
            tvError.setVisibility(android.view.View.VISIBLE);
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
