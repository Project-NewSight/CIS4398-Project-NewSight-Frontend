package com.example.newsight;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class TrustedContactsActivity extends AppCompatActivity {

    private static final int ADD_CONTACT_REQUEST = 1;
    private LinearLayout layoutContactsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_contacts);

        layoutContactsList = findViewById(R.id.layout_contacts_list);

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        FrameLayout btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LogoutActivity.class);
            startActivity(intent);
        });

        FrameLayout btnAddContact = findViewById(R.id.btn_add_contact);
        btnAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddContactActivity.class);
            startActivityForResult(intent, ADD_CONTACT_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            String name = data.getStringExtra("name");
            String phone = data.getStringExtra("phone");
            String relationship = data.getStringExtra("relationship");
            addContactView(name, phone, relationship);
        }
    }

    private void addContactView(String name, String phone, String relationship) {
        View contactView = LayoutInflater.from(this).inflate(R.layout.item_contact_card, layoutContactsList, false);
        
        TextView tvName = contactView.findViewById(R.id.tvName);
        TextView tvPhone = contactView.findViewById(R.id.tvPhone);
        TextView tvRelationship = contactView.findViewById(R.id.tvRelationship);
        FrameLayout btnDelete = contactView.findViewById(R.id.btnDelete);

        tvName.setText(name);
        tvPhone.setText(phone);
        tvRelationship.setText(relationship);

        btnDelete.setOnClickListener(v -> layoutContactsList.removeView(contactView));

        layoutContactsList.addView(contactView);
    }
}
