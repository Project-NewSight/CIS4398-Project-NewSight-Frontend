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
        RectF rect;
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

    private final List<Box> boxes = new ArrayList<>();

    private int imageWidth = 0;
    private int imageHeight = 0;
    private String summaryMessage = "";

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

        bgPaint.setColor(Color.argb(160, 0, 0, 0)); // 半透明黑
        bgPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * 后端检测结果：传入归一化 bbox + 标签 + 分数。
     */
    public synchronized void setBackendResults(
            List<CloudDetectionModels.BackendDetection> detections,
            int imageWidth,
            int imageHeight,
            String summaryMessage
    ) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.summaryMessage = summaryMessage != null ? summaryMessage : "";

        boxes.clear();
        if (detections != null) {
            for (CloudDetectionModels.BackendDetection d : detections) {
                CloudDetectionModels.BBox b = d.bbox;
                if (b == null) continue;

                // 先按照原始图像坐标构造，再在 onDraw 里按比例缩放
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
        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (imageWidth == 0 || imageHeight == 0) {
            return;
        }

        float scaleX = getWidth() / (float) imageWidth;
        float scaleY = getHeight() / (float) imageHeight;

        // 画 bbox
        for (Box box : boxes) {
            RectF scaled = new RectF(
                    box.rect.left * scaleX,
                    box.rect.top * scaleY,
                    box.rect.right * scaleX,
                    box.rect.bottom * scaleY
            );

            canvas.drawRoundRect(scaled, 12f, 12f, boxPaint);

            String text = box.label + " " + Math.round(box.score * 100) + "%";

            float tx = scaled.left + 8f;
            float ty = scaled.top - 10f;
            float tw = textPaint.measureText(text) + 16f;
            float th = 36f + 12f;
            RectF bg = new RectF(tx - 8f, ty - th, tx - 8f + tw, ty);

            canvas.drawRoundRect(bg, 8f, 8f, bgPaint);
            canvas.drawText(text, tx, ty - 12f, textPaint);
        }

        // 最上面画 summary（比如“Front 1.8m: person detected”）
        if (!summaryMessage.isEmpty()) {
            float w = getWidth();
            RectF bar = new RectF(0, 0, w, 60);
            canvas.drawRect(bar, bgPaint);
            canvas.drawText(summaryMessage, 16f, 40f, textPaint);
        }
    }
}
