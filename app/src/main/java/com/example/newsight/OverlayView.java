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
        RectF rect;     // 以“原始图像坐标”存储
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

    // 后端给的原始图像尺寸
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

        bgPaint.setColor(Color.argb(160, 0, 0, 0)); // 半透明黑底
        bgPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * 后端检测结果：detections 中的 bbox 为 0~1 归一化坐标
     */
    public synchronized void setBackendResults(
            java.util.List<CloudDetectionModels.BackendDetection> detections,
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

                // 存储为“原始图像坐标”
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

        if (imageWidth == 0 || imageHeight == 0) return;

        float viewW = getWidth();
        float viewH = getHeight();

        // 1. 按比例缩放，保证不拉伸：取同一个 scale，居中显示
        float scale = Math.min(viewW / imageWidth, viewH / imageHeight);

        // 图像在 View 中的偏移（留出黑边）
        float drawnImgW = imageWidth * scale;
        float drawnImgH = imageHeight * scale;
        float offsetX = (viewW - drawnImgW) / 2f;
        float offsetY = (viewH - drawnImgH) / 2f;

        // 2. summary bar 画在“图像区域的最上方”，避免太贴顶
        float barHeight = 60f;
        if (!summaryMessage.isEmpty()) {
            RectF bar = new RectF(offsetX, offsetY, offsetX + drawnImgW, offsetY + barHeight);
            canvas.drawRect(bar, bgPaint);
            canvas.drawText(summaryMessage, offsetX + 16f, offsetY + 40f, textPaint);
        }

        // 3. 画 bbox 和 label，注意不要和 summary bar 重叠
        for (Box box : boxes) {
            // 原始坐标 -> View 坐标：先缩放，再加偏移
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

            // 默认打算画在框的上方
            float tx = scaled.left + textMargin;
            float ty = scaled.top - textMargin;

            // 计算 label 背景框的位置
            float labelTop = ty - th;
            float minTopAllowed = offsetY + barHeight + 8f; // 至少要在 summary bar 下面一点

            if (labelTop < minTopAllowed) {
                // 如果会和 summary 冲突，就画在框的“内部顶端”
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
    }
}
