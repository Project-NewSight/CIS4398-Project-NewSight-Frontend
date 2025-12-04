package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class RewardsActivity extends AppCompatActivity {

    private static final int POINTS_PER_DOLLAR = 1000;
    private static final int TIER_10_POINTS = 10000;

    private com.example.newsight.helpers.RewardsHelper rewardsHelper;

    private TextView textTotalPoints;
    private TextView textCurrentValue;
    private TextView textAwayFromNext;
    private Button buttonRedeemPoints;
    private ImageButton buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        // Initialize RewardsHelper
        rewardsHelper = new com.example.newsight.helpers.RewardsHelper(this);

        // Hook up views
        textTotalPoints = findViewById(R.id.textTotalPoints);
        textCurrentValue = findViewById(R.id.textCurrentValue);
        textAwayFromNext = findViewById(R.id.textAwayFromNext);
        buttonRedeemPoints = findViewById(R.id.buttonRedeemPoints);
        buttonBack = findViewById(R.id.buttonBack);

        updateSummaryUI();

        // Back button to navigate to home screen
        buttonBack.setOnClickListener(view -> {
            Intent intent = new Intent(RewardsActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // When user taps "Redeem Points"
        buttonRedeemPoints.setOnClickListener(view -> {
            Intent intent = new Intent(RewardsActivity.this, RedeemActivity.class);
            intent.putExtra("current_points", rewardsHelper.getPoints());
            startActivity(intent);
        });
    }

    private void updateSummaryUI() {
        int currentPoints = rewardsHelper.getPoints();

        // Show total points
        textTotalPoints.setText(String.format(Locale.US, "%,d", currentPoints));

        // Convert points to dollar value
        double dollars = currentPoints / (double) POINTS_PER_DOLLAR;
        textCurrentValue.setText(String.format(Locale.US, "Current value: $%.2f", dollars));

        // Show points needed for next $10 gift card
        int pointsToNext = Math.max(0, TIER_10_POINTS - currentPoints);
        textAwayFromNext.setText(
                String.format(Locale.US, "%,d points away from a $10 gift card", pointsToNext)
        );
    }
}
