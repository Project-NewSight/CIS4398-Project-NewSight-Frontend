package com.example.newsight;

import android.os.Bundle;
import android.content.Intent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;



public class AddContactActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_add_edit);

        EditText inputName = findViewById(R.id.inputName);
        EditText inputPhone = findViewById(R.id.inputPhone);
        FrameLayout btnSave = findViewById(R.id.btnSaveContact);

        // Home Button
        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
        });

        // Mic Button
        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> {
            startActivity(new Intent(this, VoiceCommandActivity.class));
        });

        btnSave.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String phone = inputPhone.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please enter both name and phone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            int userId = 7;
            ApiClient.INSTANCE.postContact(
                    userId,
                    name,
                    phone,
                    "Friend",
                    "Unknown",
                    new ApiCallback() {
                        @Override
                        public void onResult(boolean success, String msg) {
                            runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(AddContactActivity.this, "Contact added sucessfully", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(AddContactActivity.this, TrustedContactsActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(AddContactActivity.this, "Error: " + msg, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
            );
        });
    }
}




