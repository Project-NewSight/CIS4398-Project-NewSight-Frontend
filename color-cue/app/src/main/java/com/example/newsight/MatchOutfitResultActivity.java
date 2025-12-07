package com.example.newsight;

import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.api.ApiClient;
import com.example.newsight.api.ColorCueApi;
import com.example.newsight.api.FileUtils;
import com.example.newsight.api.models.MatchResponse; // update this to your model

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchOutfitResultActivity extends AppCompatActivity {

    FrameLayout btnBack, btnHome;
    TextView txtMatchExplanation;

    Uri imgTop, imgBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_outfit_result);

        btnBack = findViewById(R.id.btnBack);
        btnHome = findViewById(R.id.btnHome);
        txtMatchExplanation = findViewById(R.id.txtMatchExplanation);

        imgTop = Uri.parse(getIntent().getStringExtra("photo_uri_1"));
        imgBottom = Uri.parse(getIntent().getStringExtra("photo_uri_2"));

        btnBack.setOnClickListener(v -> finish());
        btnHome.setOnClickListener(v -> finish());

        compareTwoItems();
    }

    private void compareTwoItems() {
        txtMatchExplanation.setText("Analyzing outfit compatibility...");

        ColorCueApi api = ApiClient.getClient().create(ColorCueApi.class);

        MultipartBody.Part topPart =
                FileUtils.uriToPart(this, imgTop, "top");

        MultipartBody.Part bottomPart =
                FileUtils.uriToPart(this, imgBottom, "bottom");

        api.compareItems(topPart, bottomPart).enqueue(new Callback<MatchResponse>() {
            @Override
            public void onResponse(Call<MatchResponse> call, Response<MatchResponse> resp) {

                if (!resp.isSuccessful() || resp.body() == null) {
                    txtMatchExplanation.setText("Error analyzing outfit match.");
                    return;
                }

                MatchResponse r = resp.body();

                String text = "";

                text += r.match ? "✔ These items match\n\n" : "✖ These items do not match\n\n";

                if (r.TTS_Output != null && r.TTS_Output.message != null) {
                    text += r.TTS_Output.message;
                }


                txtMatchExplanation.setText(text);
            }

            @Override
            public void onFailure(Call<MatchResponse> call, Throwable t) {
                txtMatchExplanation.setText("Network error: " + t.getMessage());
            }
        });
    }
}
