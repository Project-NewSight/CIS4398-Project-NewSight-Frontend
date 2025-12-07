package com.example.newsight;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class splash extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvSubtitle;
    private ImageView imgLogo;
    private final String subtitleText = "See Beyond Limits";
    private int charIndex = 0;
    private final long TYPE_DELAY = 100; // Delay between each character

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        imgLogo = findViewById(R.id.imgLogo);

        // Initial state
        tvTitle.setAlpha(0f);
        imgLogo.setAlpha(0f);
        tvSubtitle.setText("");

        // Start animations
        startAnimations();
    }

    private void startAnimations() {
        // 1. Animate Logo (Fade In + Scale)
        ObjectAnimator logoFade = ObjectAnimator.ofFloat(imgLogo, "alpha", 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(imgLogo, "scaleX", 0.5f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(imgLogo, "scaleY", 0.5f, 1f);

        // 2. Animate Title "NEWSIGHT" (Fade In + Slide Down)
        ObjectAnimator titleFade = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f);
        ObjectAnimator titleSlide = ObjectAnimator.ofFloat(tvTitle, "translationY", -50f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(logoFade, logoScaleX, logoScaleY, titleFade, titleSlide);
        set.setDuration(1500);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();

        // 3. Start Typewriter effect for subtitle after main animation
        new Handler().postDelayed(this::typeWriterEffect, 1500);
    }

    private void typeWriterEffect() {
        if (charIndex <= subtitleText.length()) {
            tvSubtitle.setText(subtitleText.substring(0, charIndex));
            charIndex++;
            new Handler().postDelayed(this::typeWriterEffect, TYPE_DELAY);
        } else {
            // Animation complete, navigate to MainActivity
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(splash.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, 1000); // Wait 1 second after typing finishes
        }
    }
}