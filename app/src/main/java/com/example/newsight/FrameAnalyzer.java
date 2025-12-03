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

            // Reuse buffer for NV21
            int requiredLength = width * height * 3 / 2;
            if (nv21Buffer == null || nv21Buffer.length < requiredLength) {
                nv21Buffer = new byte[requiredLength];
            }

            yuv420ToNv21(image, nv21Buffer);

            jpegStream.reset();
            YuvImage yuvImage = new YuvImage(nv21Buffer, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 85, jpegStream);

            byte[] jpegBytes = jpegStream.toByteArray();
            String activeFeature = featureProvider.getActiveFeature();

            if (wsManager != null && wsManager.isConnected() && activeFeature != null) {
                // Send frames as JSON + Base64
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

        int offset = 0;

        // Copy Y plane
        ByteBuffer yBuffer = planes[0].getBuffer();
        int rowStrideY = planes[0].getRowStride();
        int pixelStrideY = planes[0].getPixelStride();

        for (int row = 0; row < height; row++) {
            int length = width;
            yBuffer.position(row * rowStrideY);
            if (pixelStrideY == 1) {
                yBuffer.get(out, offset, length);
                offset += length;
            } else {
                for (int col = 0; col < width; col++) {
                    out[offset++] = yBuffer.get(row * rowStrideY + col * pixelStrideY);
                }
            }
        }

        // Copy UV planes into NV21 format (V then U)
        int chromaHeight = height / 2;

        for (int planeIndex = 1; planeIndex <= 2; planeIndex++) {
            ByteBuffer buffer = planes[planeIndex].getBuffer();
            int rowStride = planes[planeIndex].getRowStride();
            int pixelStride = planes[planeIndex].getPixelStride();

            for (int row = 0; row < chromaHeight; row++) {
                for (int col = 0; col < width / 2; col++) {
                    int bufferIndex = row * rowStride + col * pixelStride;
                    byte value = buffer.get(bufferIndex);

                    // Plane 1 = U, Plane 2 = V → NV21 = V first, then U
                    if (planeIndex == 2) {
                        out[offset++] = value; // V
                    } else {
                        out[offset++] = value; // U
                    }
                }
            }
        }
    }

}
