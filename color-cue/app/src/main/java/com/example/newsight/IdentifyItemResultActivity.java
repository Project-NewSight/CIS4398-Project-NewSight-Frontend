package com.example.newsight;

import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.newsight.api.ApiClient;
import com.example.newsight.api.ColorCueApi;
import com.example.newsight.api.FileUtils;
import com.example.newsight.api.models.IdentifyResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IdentifyItemResultActivity extends AppCompatActivity {

    ImageView imgPreview;
    FrameLayout btnHome;

    TextView txtColor, txtCategory, txtMaterial, txtPattern, txtWashing, txtNotes;

    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_item_result);

        imgPreview = findViewById(R.id.imgResultPreview);

        txtColor = findViewById(R.id.txtColor);
        txtCategory = findViewById(R.id.txtCategory);
        txtMaterial = findViewById(R.id.txtMaterial);
        txtPattern = findViewById(R.id.txtPattern);
        txtWashing = findViewById(R.id.txtWashing);
        txtNotes = findViewById(R.id.txtNotes);

        btnHome = findViewById(R.id.btnHome);

        photoUri = Uri.parse(getIntent().getStringExtra("photo_uri"));

        Glide.with(this).load(photoUri).into(imgPreview);

        identifyItem();

        btnHome.setOnClickListener(v -> finish());
    }

    private void identifyItem() {

        ColorCueApi api = ApiClient.getClient().create(ColorCueApi.class);

        MultipartBody.Part photoPart = FileUtils.uriToPart(this, photoUri, "file");
        RequestBody closetId = FileUtils.text("1");

        api.identifyItem(closetId, photoPart).enqueue(new Callback<IdentifyResponse>() {

            @Override
            public void onResponse(Call<IdentifyResponse> call, Response<IdentifyResponse> resp) {

                if (!resp.isSuccessful() || resp.body() == null) {
                    txtColor.setText("Error identifying item.");
                    return;
                }

                IdentifyResponse r = resp.body();

                if (r.closest_item != null) {

                    // Build a natural-language description
                    String description =
                            "I think we have this in our inventory. \n\n" +
                                    "It is a " +
                                    safe(r.closest_item.color) + " " +
                                    safe(r.closest_item.category) + "\n" +
                                    "with a " + safe(r.closest_item.pattern) + " pattern.\n\n" +
                                    "Notes: " + safe(r.closest_item.notes);

                    txtColor.setText(description);

                    // Only set Category/Pattern fields individually if you still want them:
                    txtCategory.setText("Category: " + safe(r.closest_item.category));
                    txtPattern.setText("Pattern: " + safe(r.closest_item.pattern));
                    txtMaterial.setText("Material: ---");
                    txtWashing.setText("Washing Instructions: ---");
                    txtNotes.setText("Notes: " + safe(r.closest_item.notes));

                    // 🔥 IMPORTANT:
                    // Show the DATABASE item image, NOT the uploaded photo.
                    Glide.with(IdentifyItemResultActivity.this)
                            .load(r.closest_item.image_url)
                            .into(imgPreview);

                } else {
                    txtColor.setText("No similar item found.");
                }
            }


            @Override
            public void onFailure(Call<IdentifyResponse> call, Throwable t) {
                txtColor.setText("Failed: " + t.getMessage());
            }
        });
    }

    private String safe(String text) {
        return (text == null || text.trim().isEmpty()) ? "---" : text;
    }
}
