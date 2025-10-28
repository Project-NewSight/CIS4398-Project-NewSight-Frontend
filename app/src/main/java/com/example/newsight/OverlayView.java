package com.example.newsight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();

    private List<Detection> results = new ArrayList<>();
    private int imageW = 1, imageH = 1; // 拍到的图片尺寸

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

    /** 设置检测结果与原图尺寸，然后调用 invalidate() 重绘 */
    public void setResults(List<Detection> detections, int imgW, int imgH) {
        this.results = detections != null ? detections : new ArrayList<>();
        this.imageW = Math.max(1, imgW);
        this.imageH = Math.max(1, imgH);
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results == null || results.isEmpty()) return;

        // ImageView 使用 fitCenter：等比缩放并居中显示
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
            // 映射到可见区域
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
