package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ViewGroup;
import android.view.View;
import java.util.Set;
import java.util.HashSet;

public class TrustedContactsActivity extends AppCompatActivity {
    private LinearLayout contactListContainer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_contacts);

        prefs = getSharedPreferences("TrustedContacts", MODE_PRIVATE);
        contactListContainer = findViewById(R.id.contactListContainer);

        loadContacts();

        FrameLayout btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class))
        );

        FrameLayout btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v ->
                startActivity(new Intent(this, VoiceCommandActivity.class))
        );

        FrameLayout btnAddContact = findViewById(R.id.btnAddContact);
        btnAddContact.setOnClickListener(v ->
                startActivity(new Intent(this, AddContactActivity.class))
        );

        FrameLayout btnRemoveContact = findViewById(R.id.btnRemoveContact);
        btnRemoveContact.setOnClickListener(v ->
                startActivity(new Intent(this, RemoveContactActivity.class))
        );

        // Check if new contact was passed back
        String name = getIntent().getStringExtra("contact_name");
        String phone = getIntent().getStringExtra("contact_phone");
        if (name != null && phone != null) {
            addContact(name, phone);
        }
    }

    private void loadContacts() {
        contactListContainer.removeAllViews();
        Set<String> contacts = prefs.getStringSet("contacts", new HashSet<>());

        for (String contact : contacts) {
            TextView contactView = new TextView(this);
            contactView.setText(contact);
            contactView.setTextSize(28);
            contactView.setTextColor(getColor(android.R.color.white));
            contactView.setGravity(android.view.Gravity.CENTER);
            contactView.setPadding(0, 30, 0, 30);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 20, 0, 20);
            contactView.setLayoutParams(params);
            contactView.setBackgroundResource(R.drawable.rectangle_light_blue);

            contactView.setOnClickListener(v -> {
                String[] parts = contact.split(":");
                Intent editIntent = new Intent(this, AddContactActivity.class);
                editIntent.putExtra("old_contact", contact);
                editIntent.putExtra("contact_name", parts[0]);
                editIntent.putExtra("contact_phone", parts.length > 1 ? parts[1] : "");
                startActivity(editIntent);
            });


            contactListContainer.addView(contactView);
        }
    }

    private void addContact(String name, String phone) {
        Set<String> contacts = prefs.getStringSet("contacts", new HashSet<>());
        contacts.add(name + ":" + phone);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("contacts", contacts);
        editor.apply();

        loadContacts();
    }
}
