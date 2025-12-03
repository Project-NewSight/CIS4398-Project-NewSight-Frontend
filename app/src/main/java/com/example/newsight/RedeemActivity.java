package com.example.newsight;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class RedeemActivity extends AppCompatActivity {

    private static final int TIER_10_POINTS = 10_000;
    private static final int TIER_50_POINTS = 50_000;
    private static final int TIER_100_POINTS = 100_000;

    private int currentPoints = 1250;

    private RadioGroup radioGroupRewards;
    private Button buttonConfirmRedemption;
    private ImageButton buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redeem);

        // Get points from RewardsActivity if sent
        if (getIntent() != null && getIntent().hasExtra("current_points")) {
            currentPoints = getIntent().getIntExtra("current_points", currentPoints);
        }

        // Hook up views
        radioGroupRewards = findViewById(R.id.radioGroupRewards);
        buttonConfirmRedemption = findViewById(R.id.buttonConfirmRedemption);
        buttonBack = findViewById(R.id.buttonBack);

        // Back button to navigate back
        buttonBack.setOnClickListener(view -> finish());

        // Handle "Confirm Redemption"
        buttonConfirmRedemption.setOnClickListener(view -> handleRedemption());
    }

    private void handleRedemption() {
        int selectedId = radioGroupRewards.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Please choose a reward tier first.", Toast.LENGTH_SHORT).show();
            return;
        }

        int costPoints;
        String rewardLabel;

        if (selectedId == R.id.radioSmallReward) {
            costPoints = TIER_10_POINTS;
            rewardLabel = "$10 gift card";
        } else if (selectedId == R.id.radioMediumReward) {
            costPoints = TIER_50_POINTS;
            rewardLabel = "$50 gift card";
        } else if (selectedId == R.id.radioLargeReward) {
            costPoints = TIER_100_POINTS;
            rewardLabel = "$100 gift card";
        } else {
            Toast.makeText(this, "Unknown reward option selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Not enough points
        if (currentPoints < costPoints) {
            int missing = costPoints - currentPoints;
            String msg = "You don't have enough points yet for this reward.\n" +
                    String.format(Locale.US, "You still need %,d more points.", missing);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }

        // Successful redemption
        currentPoints -= costPoints;
        String successMsg = String.format(
                Locale.US,
                "Redemption successful! You redeemed a %s.\nRemaining balance: %,d pts.",
                rewardLabel,
                currentPoints
        );
        Toast.makeText(this, successMsg, Toast.LENGTH_LONG).show();

        finish();
    }
}
