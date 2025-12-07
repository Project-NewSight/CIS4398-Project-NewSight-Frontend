package com.example.newsight;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;
import java.util.HashSet;


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

            SharedPreferences prefs = getSharedPreferences("TrustedContacts", MODE_PRIVATE);
            Set<String> contacts = prefs.getStringSet("contacts", new HashSet<>());
            contacts.add(name + ":" + phone);
            prefs.edit().putStringSet("contacts", contacts).apply();

            Toast.makeText(this, "Contact saved successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, TrustedContactsActivity.class));
            finish();
        });

    }
}
