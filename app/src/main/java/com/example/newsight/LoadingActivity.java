package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {

    private ImageView outerRing, innerRing;
    private View pulsingCore, dot1, dot2, dot3;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Initialize views
        outerRing = findViewById(R.id.outerRing);
        innerRing = findViewById(R.id.innerRing);
        pulsingCore = findViewById(R.id.pulsingCore);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        handler = new Handler();

        // Start animations
        startRotatingRings();
        startPulsingCore();
        startAnimatedDots();

        // Navigate to Home after 3.5 seconds
        handler.postDelayed(() -> {
            Intent intent = new Intent(LoadingActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }, 3500);
    }

    private void startRotatingRings() {
        // Outer ring - rotate clockwise
        RotateAnimation rotateOuter = new RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotateOuter.setDuration(8000);
        rotateOuter.setRepeatCount(Animation.INFINITE);
        rotateOuter.setInterpolator(new LinearInterpolator());
        outerRing.startAnimation(rotateOuter);

        // Inner ring - rotate counter-clockwise
        RotateAnimation rotateInner = new RotateAnimation(
                0f, -360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotateInner.setDuration(12000);
        rotateInner.setRepeatCount(Animation.INFINITE);
        rotateInner.setInterpolator(new LinearInterpolator());
        innerRing.startAnimation(rotateInner);
    }

    private void startPulsingCore() {
        // Scale animation
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.2f,
                1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(2000);
        scaleAnimation.setRepeatCount(Animation.INFINITE);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        // Alpha animation
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.5f, 1.0f);
        alphaAnimation.setDuration(2000);
        alphaAnimation.setRepeatCount(Animation.INFINITE);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        pulsingCore.startAnimation(scaleAnimation);
        pulsingCore.startAnimation(alphaAnimation);
    }

    private void startAnimatedDots() {
        animateDot(dot1, 0);
        animateDot(dot2, 200);
        animateDot(dot3, 400);
    }

    private void animateDot(View dot, long delay) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(1500);
        alphaAnimation.setRepeatCount(Animation.INFINITE);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        alphaAnimation.setStartOffset(delay);
        alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        dot.startAnimation(alphaAnimation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
