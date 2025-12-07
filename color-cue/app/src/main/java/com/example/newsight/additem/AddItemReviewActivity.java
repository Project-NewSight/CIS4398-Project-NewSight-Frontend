package com.example.newsight.additem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.newsight.R;
import com.example.newsight.api.ApiClient;
import com.example.newsight.api.ColorCueApi;
import com.example.newsight.api.FileUtils;
import com.example.newsight.api.models.ClothingItem;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddItemReviewActivity extends AppCompatActivity {

    FrameLayout btnSaveItem, btnEdit, btnHome, btnMic, loadingOverlay;

    ImageView imgReviewPreview;
    TextView txtSummary;

    String frontPhotoPath, tagFrontPath, tagBackPath, genre, notes;

    // ---- Crash-proof state retention ----
    private boolean isUploading = false;
    private Call<ClothingItem> currentCall = null;
    private Handler mainHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item_review);

        btnSaveItem = findViewById(R.id.btnSaveItem);
        btnEdit = findViewById(R.id.btnEdit);
        btnHome = findViewById(R.id.btnHome);
        btnMic = findViewById(R.id.btnMic);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        imgReviewPreview = findViewById(R.id.imgReviewPreview);
        txtSummary = findViewById(R.id.txtSummary);

        // ---- Restore saved state if restart occurred ----
        if (savedInstanceState != null) {
            isUploading = savedInstanceState.getBoolean("isUploading", false);
        }

        frontPhotoPath = getIntent().getStringExtra("front_photo_path");
        tagFrontPath = getIntent().getStringExtra("tag_front_path");
        tagBackPath = getIntent().getStringExtra("tag_back_path");
        genre = getIntent().getStringExtra("genre");
        notes = getIntent().getStringExtra("notes");

        if (frontPhotoPath != null) {
            Glide.with(this).load(Uri.parse(frontPhotoPath)).into(imgReviewPreview);
        }

        txtSummary.setText(
                "Detected Item\n\n" +
                        "Genre: " + safe(genre) + "\n" +
                        "Notes: " + safe(notes)
        );

        btnSaveItem.setOnClickListener(v -> {
            if (!isUploading) uploadItem();
        });

        btnEdit.setOnClickListener(v -> finish());
        btnHome.setOnClickListener(v -> finish());

        if (isUploading) {
            loadingOverlay.setVisibility(View.VISIBLE);
            disableButtons();
        }
    }


    // -------------------------------------------------------------------------
    // 💾 Persist upload-in-progress across Activity restarts
    // -------------------------------------------------------------------------
    @Override
    protected void onSaveInstanceState(@Nullable Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putBoolean("isUploading", isUploading);
        }
    }


    // -------------------------------------------------------------------------
    // 🚫 Prevent Samsung from killing the Activity onStop
    // -------------------------------------------------------------------------
    @Override
    protected void onStop() {
        super.onStop();

        // DO NOT cancel upload here — we want the request to finish
        // Do not call finish(), do not hide overlay
        // Simply keep the Retrofit call alive
    }

    // -------------------------------------------------------------------------
    // ⛔ Prevent hard kill when display changes (Samsung VR/Dex)
    // -------------------------------------------------------------------------
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Ignore visibility changes during upload
    }



    // -------------------------------------------------------------------------
    // 📤 UPLOAD LOGIC (crash-proof)
    // -------------------------------------------------------------------------
    private void uploadItem() {

        isUploading = true;
        loadingOverlay.setVisibility(View.VISIBLE);
        disableButtons();

        ColorCueApi api = ApiClient.getClient().create(ColorCueApi.class);

        MultipartBody.Part front = FileUtils.uriToPart(this, Uri.parse(frontPhotoPath), "file_front");
        MultipartBody.Part tagFront = null;
        MultipartBody.Part tagBack = null;

        if (tagFrontPath != null && !tagFrontPath.isEmpty()) {
            tagFront = FileUtils.uriToPart(this, Uri.parse(tagFrontPath), "tag_side_a");
        }

        if (tagBackPath != null && !tagBackPath.isEmpty()) {
            tagBack = FileUtils.uriToPart(this, Uri.parse(tagBackPath), "tag_side_b");
        }

        RequestBody closetId = FileUtils.text("1");
        RequestBody genreBody = FileUtils.text(genre == null ? "" : genre);
        RequestBody notesBody = FileUtils.text(notes == null ? "" : notes);

        currentCall = api.uploadItem(closetId, genreBody, notesBody, front, tagFront, tagBack);

        currentCall.enqueue(new Callback<ClothingItem>() {

            @Override
            public void onResponse(Call<ClothingItem> call, Response<ClothingItem> response) {
                safeUi(() -> {
                    isUploading = false;
                    loadingOverlay.setVisibility(View.GONE);
                    enableButtons();

                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(AddItemReviewActivity.this,
                                "Upload failed (server error)",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    ClothingItem item = response.body();

                    Intent detail = new Intent(AddItemReviewActivity.this, ItemDetailActivity.class);
                    detail.putExtra("image_url", item.image_url);
                    detail.putExtra("color", item.color);
                    detail.putExtra("category", item.category);
                    detail.putExtra("material", item.material);
                    detail.putExtra("washing_instructions", item.washing_instructions);
                    detail.putExtra("printed_text", item.printed_text);
                    detail.putExtra("pattern", item.pattern);
                    detail.putExtra("style", item.style);
                    detail.putExtra("genre", item.genre);
                    detail.putExtra("printed_text", item.printed_text);
                    detail.putExtra("notes", item.notes);

                    startActivity(detail);
                });
            }

            @Override
            public void onFailure(Call<ClothingItem> call, Throwable t) {
                safeUi(() -> {
                    isUploading = false;
                    loadingOverlay.setVisibility(View.GONE);
                    enableButtons();

                    Toast.makeText(AddItemReviewActivity.this,
                            "Upload failed: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    // -------------------------------------------------------------------------
    // THREAD-SAFE UI WRAPPER
    // -------------------------------------------------------------------------
    @MainThread
    private void safeUi(Runnable run) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            run.run();
        } else {
            mainHandler.post(run);
        }
    }


    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private String safe(String text) {
        return (text == null || text.trim().isEmpty()) ? "---" : text;
    }

    private void disableButtons() {
        btnSaveItem.setEnabled(false);
        btnEdit.setEnabled(false);
        btnHome.setEnabled(false);
        btnMic.setEnabled(false);
    }

    private void enableButtons() {
        btnSaveItem.setEnabled(true);
        btnEdit.setEnabled(true);
        btnHome.setEnabled(true);
        btnMic.setEnabled(true);
    }
}
