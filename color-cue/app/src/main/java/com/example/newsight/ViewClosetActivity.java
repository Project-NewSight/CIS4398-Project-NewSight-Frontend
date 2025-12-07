package com.example.newsight;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsight.adapters.ClosetAdapter;
import com.example.newsight.api.ApiClient;
import com.example.newsight.api.ColorCueApi;
import com.example.newsight.api.models.ClosetResponse;
import com.example.newsight.api.models.ClothingItem;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ViewClosetActivity extends AppCompatActivity {

    Spinner spinnerColor, spinnerCategory;
    GridView gridCloset;
    ClosetAdapter adapter;

    ArrayList<ClothingItem> closetItems = new ArrayList<>();

    int USER_ID = 1;  // TODO: Replace later with logged-in user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_closet);

        spinnerColor = findViewById(R.id.spinnerColor);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        gridCloset = findViewById(R.id.gridCloset);

        adapter = new ClosetAdapter(this, closetItems);
        gridCloset.setAdapter(adapter);

        loadFilters();
        loadCloset("", "");
    }

    private void loadFilters() {
        String[] colors = {"", "black", "white", "gray", "red", "green", "blue", "yellow"};
        String[] categories = {"", "t-shirt", "hoodie", "dress", "pants", "shorts", "skirt"};

        spinnerColor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colors));
        spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories));

        spinnerColor.setOnItemSelectedListener(filterChanged);
        spinnerCategory.setOnItemSelectedListener(filterChanged);
    }

    private final AdapterView.OnItemSelectedListener filterChanged = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String color = spinnerColor.getSelectedItem().toString();
            String category = spinnerCategory.getSelectedItem().toString();
            loadCloset(color, category);
        }
        @Override public void onNothingSelected(AdapterView<?> parent) {}
    };

    private void loadCloset(String color, String category) {
        ColorCueApi api = ApiClient.getClient().create(ColorCueApi.class);

        api.getCloset(USER_ID, color, category).enqueue(new Callback<ClosetResponse>() {
            @Override
            public void onResponse(Call<ClosetResponse> call, Response<ClosetResponse> response) {
                if (response.body() != null) {
                    closetItems.clear();
                    closetItems.addAll(response.body().items);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ClosetResponse> call, Throwable t) {
                Toast.makeText(ViewClosetActivity.this, "Failed to load closet", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
