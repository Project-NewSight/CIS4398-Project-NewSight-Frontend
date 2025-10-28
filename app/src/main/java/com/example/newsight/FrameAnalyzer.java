package com.example.newsight;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "FrameAnalyzer";
    private final WebSocketManager wsManager;
    private final FeatureProvider featureProvider;          // <-- keep provider reference

    private final ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
    private byte[] nv21Buffer = null;
    private long lastLogTime = 0;

    public interface FeatureProvider {
        String getActiveFeature();
    }

    public FrameAnalyzer(WebSocketManager manager, FeatureProvider provider) {
        this.wsManager = manager;
        this.featureProvider = provider;                    // <-- IMPORTANT: do NOT set () -> null
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) { imageProxy.close(); return; }

        try {
            // ---- YUV_420_888 -> NV21 -> JPEG (same as you had) ----
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            if (nv21Buffer == null || nv21Buffer.length < ySize + uSize + vSize) {
                nv21Buffer = new byte[ySize + uSize + vSize];
            }

            // NV21 = Y + V + U
            yBuffer.get(nv21Buffer, 0, ySize);
            vBuffer.get(nv21Buffer, ySize, vSize);
            uBuffer.get(nv21Buffer, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21Buffer, ImageFormat.NV21,
                    imageProxy.getWidth(), imageProxy.getHeight(), null);

            jpegStream.reset();
            yuvImage.compressToJpeg(new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
                    80, jpegStream);
            byte[] jpegBytes = jpegStream.toByteArray();

            // ---- Send only if feature is active and socket connected ----
            String feature = (featureProvider != null) ? featureProvider.getActiveFeature() : null;
            if (feature != null && wsManager != null && wsManager.isConnected()) {
                // Choose ONE of these based on your backend:

                // A) text control + binary (preferred, matches current WebSocketManager.sendFrame)
                wsManager.sendFrame(jpegBytes, feature);

                // B) single JSON with base64 (if your FastAPI expects one message)
                // wsManager.sendFrameAsJsonBase64(jpegBytes, feature);

            } else {
                long now = System.currentTimeMillis();
                if (now - lastLogTime > 2000) {
                    Log.d(TAG, "Skip send (feature=" + feature +
                            ", connected=" + (wsManager != null && wsManager.isConnected()) + ")");
                    lastLogTime = now;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "analyze error", e);
        } finally {
            imageProxy.close();
        }
    }
}
