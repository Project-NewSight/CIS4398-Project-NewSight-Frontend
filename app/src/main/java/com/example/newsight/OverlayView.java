package com.example.newsight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    private static class Box {
        RectF rect;     // in "backend image" coordinates
        String label;
        float score;

        Box(RectF rect, String label, float score) {
            this.rect = rect;
            this.label = label;
            this.score = score;
        }
    }

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();
    private final Paint hudTextPaint = new Paint();

    private final List<Box> boxes = new ArrayList<>();
    private final List<Box> lastBoxes = new ArrayList<>();

    private int imageWidth = 0;   // backend input width
    private int imageHeight = 0;  // backend input height

    private String summaryMessage = "";
    private String lastSpokenSummary = ""; // Track last spoken message to avoid repetition
    private long hudLatencyMs = 0L;
    private float hudFps = 0f;

    private TtsHelper ttsHelper; // TTS helper for speaking summary messages

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setAntiAlias(true);

        bgPaint.setColor(Color.argb(160, 0, 0, 0));
        bgPaint.setStyle(Paint.Style.FILL);

        hudTextPaint.setColor(Color.WHITE);
        hudTextPaint.setTextSize(30f);
        hudTextPaint.setAntiAlias(true);
    }

    /**
     * Set the TtsHelper instance for speaking summary messages.
     * This should be called from the Activity that creates this OverlayView.
     */
    public void setTtsHelper(TtsHelper ttsHelper) {
        this.ttsHelper = ttsHelper;
    }

    /**
     * Called from the detector thread (via post()) when new results are available.
     * All bbox coordinates are normalized [0,1] on backend input image; we convert
     * them into absolute image coords here.
     */
    public synchronized void setBackendResults(
            List<CloudDetectionModels.BackendDetection> detections,
            int imageWidth,
            int imageHeight,
            String summaryMessage,
            long latencyMs,
            float fps
    ) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.summaryMessage = summaryMessage != null ? summaryMessage : "";
        this.hudLatencyMs = latencyMs;
        this.hudFps = fps;

        // Speak the summary message if it has changed and TtsHelper is available
        if (ttsHelper != null && !this.summaryMessage.isEmpty() && !this.summaryMessage.equals(lastSpokenSummary)) {
            ttsHelper.speak(this.summaryMessage);
            lastSpokenSummary = this.summaryMessage;
        }

        lastBoxes.clear();
        lastBoxes.addAll(boxes);

        boxes.clear();
        if (detections != null) {
            for (CloudDetectionModels.BackendDetection d : detections) {
                CloudDetectionModels.BBox b = d.bbox;
                if (b == null) continue;

                RectF rect = new RectF(
                        b.x_min * imageWidth,
                        b.y_min * imageHeight,
                        b.x_max * imageWidth,
                        b.y_max * imageHeight
                );
                String label = d.cls != null ? d.cls : "obj";
                float score = d.confidence;

                boxes.add(new Box(rect, label, score));
            }
        }

        // Simple exponential smoothing to reduce jitter when box counts match
        if (!lastBoxes.isEmpty() && lastBoxes.size() == boxes.size()) {
            float alpha = 0.4f; // 0..1, lower = smoother
            for (int i = 0; i < boxes.size(); i++) {
                Box cur = boxes.get(i);
                Box prev = lastBoxes.get(i);

                cur.rect.left   = prev.rect.left   * (1 - alpha) + cur.rect.left   * alpha;
                cur.rect.top    = prev.rect.top    * (1 - alpha) + cur.rect.top    * alpha;
                cur.rect.right  = prev.rect.right  * (1 - alpha) + cur.rect.right  * alpha;
                cur.rect.bottom = prev.rect.bottom * (1 - alpha) + cur.rect.bottom * alpha;
            }
        }

        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (imageWidth == 0 || imageHeight == 0) return;

        float viewW = getWidth();
        float viewH = getHeight();

        // Fit backend image into the view without stretching.
        float scale = Math.min(viewW / imageWidth, viewH / imageHeight);
        float drawnImgW = imageWidth * scale;
        float drawnImgH = imageHeight * scale;
        float offsetX = (viewW - drawnImgW) / 2f;
        float offsetY = (viewH - drawnImgH) / 2f;

        // Summary bar at the top of the image region
        float barHeight = 60f;
        if (!summaryMessage.isEmpty()) {
            RectF bar = new RectF(offsetX, offsetY, offsetX + drawnImgW, offsetY + barHeight);
            canvas.drawRect(bar, bgPaint);
            canvas.drawText(summaryMessage, offsetX + 16f, offsetY + 40f, textPaint);
        }

        // Draw boxes
        for (Box box : boxes) {
            RectF scaled = new RectF(
                    offsetX + box.rect.left * scale,
                    offsetY + box.rect.top * scale,
                    offsetX + box.rect.right * scale,
                    offsetY + box.rect.bottom * scale
            );

            canvas.drawRoundRect(scaled, 12f, 12f, boxPaint);

            String text = box.label + " " + Math.round(box.score * 100) + "%";

            float textMargin = 8f;
            float labelTextSize = textPaint.getTextSize();
            float th = labelTextSize + 12f;
            float tw = textPaint.measureText(text) + 16f;

            // Default: try to place label above the box
            float tx = scaled.left + textMargin;
            float ty = scaled.top - textMargin;

            float labelTop = ty - th;
            float minTopAllowed = offsetY + barHeight + 8f; // avoid overlapping summary bar

            if (labelTop < minTopAllowed) {
                // If it would overlap the summary, move label inside the box at the top.
                labelTop = scaled.top + textMargin;
                ty = labelTop + th - 12f;
            }

            RectF bg = new RectF(
                    tx - 8f,
                    labelTop,
                    tx - 8f + tw,
                    labelTop + th
            );

            canvas.drawRoundRect(bg, 8f, 8f, bgPaint);
            canvas.drawText(text, tx, ty, textPaint);
        }

        // HUD at the bottom-left: FPS + latency
        String hudText = String.format("FPS: %.1f   Latency: %d ms", hudFps, hudLatencyMs);
        float hudPadding = 10f;
        float hudHeight = 40f;
        float hudWidth = hudTextPaint.measureText(hudText) + 2 * hudPadding;

        float hudLeft = offsetX;
        float hudTop = offsetY + drawnImgH - hudHeight - hudPadding;

        RectF hudBg = new RectF(
                hudLeft,
                hudTop,
                hudLeft + hudWidth,
                hudTop + hudHeight
        );
        canvas.drawRoundRect(hudBg, 10f, 10f, bgPaint);
        canvas.drawText(
                hudText,
                hudLeft + hudPadding,
                hudTop + hudHeight - 12f,
                hudTextPaint
        );
    }
}
