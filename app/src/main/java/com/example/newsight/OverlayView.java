package com.example.newsight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.ArrayList;
import java.util.List;

/**
 * OverlayView - Draws bounding boxes and triggers haptic feedback
 *
 * Features:
 * - Draws detection bounding boxes on screen
 * - Calculates obstacle proximity based on bounding box size
 * - Triggers vibration patterns based on proximity zones
 */
public class OverlayView extends View {

    private static final String TAG = "OverlayView";

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();

    private List<Detection> results = new ArrayList<>();
    private int imageW = 1, imageH = 1; // Original image dimensions

    // Haptic feedback components
    private Vibrator vibrator;
    private long lastVibrationTime = 0;
    private static final long VIBRATION_COOLDOWN_MS = 500; // 500ms cooldown

    public OverlayView(Context c, AttributeSet a) {
        super(c, a);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);
        boxPaint.setColor(Color.parseColor("#22AAFF"));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setAntiAlias(true);

        bgPaint.setColor(Color.parseColor("#66000000"));
        bgPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Set the Vibrator service for haptic feedback
     */
    public void setVibrator(Vibrator vibrator) {
        this.vibrator = vibrator;
        Log.d(TAG, "✅ Vibrator service set");
    }

    /**
     * Set detection results and original image size, then trigger redraw
     */
    public void setResults(List<Detection> detections, int imgW, int imgH) {
        this.results = detections != null ? detections : new ArrayList<>();
        this.imageW = Math.max(1, imgW);
        this.imageH = Math.max(1, imgH);

        // Trigger haptic feedback based on detections
        if (!results.isEmpty()) {
            triggerHapticFeedback();
        }

        postInvalidateOnAnimation();
    }

    /**
     * Calculate proximity and trigger appropriate vibration pattern
     */
    private void triggerHapticFeedback() {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastVibrationTime) < VIBRATION_COOLDOWN_MS) {
            return; // Still in cooldown period
        }

        // Check if vibrator is available
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        // Calculate maximum bounding box area relative to screen
        float maxRelativeArea = calculateMaxRelativeArea();

        if (maxRelativeArea <= 0) {
            return; // No valid detections
        }

        // Determine proximity zone and trigger vibration
        if (maxRelativeArea > 0.6f) {
            // Very Close (>60% area): Rapid alternating pulses (High urgency)
            vibratePattern(new long[]{0, 100, 100, 100, 100, 100}, new int[]{0, 255, 0, 255, 0, 255});
            Log.d(TAG, "🚨 CRITICAL: Very close obstacle - area: " + (maxRelativeArea * 100) + "%");

        } else if (maxRelativeArea > 0.4f) {
            // Close (40-60% area): High intensity pattern
            vibratePattern(new long[]{0, 300}, new int[]{0, 220});
            Log.d(TAG, "⚠️ WARNING: Close obstacle - area: " + (maxRelativeArea * 100) + "%");

        } else if (maxRelativeArea > 0.2f) {
            // Medium (20-40% area): Medium intensity pattern
            vibratePattern(new long[]{0, 250}, new int[]{0, 180});
            Log.d(TAG, "⚡ CAUTION: Medium obstacle - area: " + (maxRelativeArea * 100) + "%");

        } else {
            // Far (<20% area): Low intensity pattern
            vibratePattern(new long[]{0, 200}, new int[]{0, 120});
            Log.d(TAG, "ℹ️ NOTICE: Distant obstacle - area: " + (maxRelativeArea * 100) + "%");
        }

        lastVibrationTime = currentTime;
    }

    /**
     * Calculate the maximum bounding box area relative to screen size
     */
    private float calculateMaxRelativeArea() {
        if (results == null || results.isEmpty()) {
            return 0f;
        }

        float maxArea = 0f;
        float screenArea = (float) imageW * imageH;

        for (Detection detection : results) {
            RectF box = detection.getBoundingBox();
            float width = box.width();
            float height = box.height();
            float boxArea = width * height;

            // Calculate relative area (0.0 to 1.0)
            float relativeArea = boxArea / screenArea;

            if (relativeArea > maxArea) {
                maxArea = relativeArea;
            }
        }

        return maxArea;
    }

    /**
     * Trigger vibration with pattern (modern API for Android O+)
     */
    private void vibratePattern(long[] timings, int[] amplitudes) {
        if (vibrator == null) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Modern API: Use VibrationEffect
                VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
                vibrator.vibrate(effect);
            } else {
                // Legacy API: Use simple pattern (no amplitude control)
                vibrator.vibrate(timings, -1);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error triggering vibration: " + e.getMessage());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results == null || results.isEmpty()) return;

        // ImageView uses fitCenter: scale proportionally and center
        float viewW = getWidth();
        float viewH = getHeight();
        float scale = Math.min(viewW / imageW, viewH / imageH);
        float drawnW = imageW * scale;
        float drawnH = imageH * scale;
        float dx = (viewW - drawnW) / 2f;
        float dy = (viewH - drawnH) / 2f;

        for (Detection d : results) {
            if (d.getCategories() == null || d.getCategories().isEmpty()) continue;

            RectF b = new RectF(d.getBoundingBox());
            // Map to visible area
            b.left   = dx + b.left   * scale;
            b.top    = dy + b.top    * scale;
            b.right  = dx + b.right  * scale;
            b.bottom = dy + b.bottom * scale;

            canvas.drawRect(b, boxPaint);

            String label = d.getCategories().get(0).getLabel();
            float score = d.getCategories().get(0).getScore();
            String text = label + " " + Math.round(score * 100) + "%";

            float tx = b.left + 8f;
            float ty = b.top - 10f;
            float tw = textPaint.measureText(text) + 16f;
            float th = 36f + 12f;
            RectF bg = new RectF(tx - 8f, ty - th, tx - 8f + tw, ty);
            canvas.drawRoundRect(bg, 8f, 8f, bgPaint);
            canvas.drawText(text, tx, ty - 12f, textPaint);
        }
    }
}