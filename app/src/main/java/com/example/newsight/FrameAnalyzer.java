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
    private final FeatureProvider featureProvider;
    private final ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
    private byte[] nv21Buffer = null; // reuse buffer
    private long lastLogTime = 0;

    public interface FeatureProvider {
        String getActiveFeature();
    }

    public FrameAnalyzer(WebSocketManager manager, FeatureProvider provider) {
        this.wsManager = manager;
        this.featureProvider = provider;
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            imageProxy.close();
            return;
        }

        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // Reuse buffer
            int requiredLength = width * height * 3 / 2; // NV21 size
            if (nv21Buffer == null || nv21Buffer.length < requiredLength) {
                nv21Buffer = new byte[requiredLength];
            }

            yuv420ToNv21(image, nv21Buffer);

            jpegStream.reset(); // reuse ByteArrayOutputStream
            YuvImage yuvImage = new YuvImage(nv21Buffer, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, jpegStream);

            byte[] jpegBytes = jpegStream.toByteArray();

            String activeFeature = featureProvider.getActiveFeature();

            if (wsManager != null && wsManager.isConnected() && activeFeature != null) {
                wsManager.sendFrame(jpegBytes, activeFeature);
            } else {
                long now = System.currentTimeMillis();
                if (now - lastLogTime > 2000) {
                    Log.d(TAG, "Skipping frame, no feature active or WS not connected");
                    lastLogTime = now;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Frame conversion failed", e);
        } finally {
            imageProxy.close();
        }
    }

    private void yuv420ToNv21(Image image, byte[] out) {
        Image.Plane[] planes = image.getPlanes();
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Y plane
        ByteBuffer yBuffer = planes[0].getBuffer();
        int ySize = yBuffer.remaining();
        yBuffer.get(out, 0, ySize);
        
        // U and V planes (interleaved for NV21)
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        
        int uvPixelStride = planes[1].getPixelStride(); // typically 1 or 2
        int uvRowStride = planes[1].getRowStride();
        
        int uvPos = ySize;
        
        for (int row = 0; row < uvHeight; row++) {
            for (int col = 0; col < uvWidth; col++) {
                int uvIndex = row * uvRowStride + col * uvPixelStride;
                // NV21 format: VUVUVU... (V first, then U)
                out[uvPos++] = vBuffer.get(uvIndex);
                out[uvPos++] = uBuffer.get(uvIndex);
            }
        }
    }
}
