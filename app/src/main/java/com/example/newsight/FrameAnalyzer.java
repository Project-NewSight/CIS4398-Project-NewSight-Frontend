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
            // Increase quality from 40 to 85 for better OCR accuracy (quality range: 0-100)
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 85, jpegStream);

            byte[] jpegBytes = jpegStream.toByteArray();

            String activeFeature = featureProvider.getActiveFeature();

            if (wsManager != null && wsManager.isConnected() && activeFeature != null) {
                wsManager.sendFrame(jpegBytes, activeFeature);
                Log.d(TAG, "Frame sent for feature: " + activeFeature);
            } else {
                long now = System.currentTimeMillis();
                if (now - lastLogTime > 2000) {
                    Log.d(TAG, "Skipping frame - wsManager: " + (wsManager != null) + 
                          ", connected: " + (wsManager != null && wsManager.isConnected()) + 
                          ", feature: " + activeFeature);
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
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        yBuffer.rewind();
        uBuffer.rewind();
        vBuffer.rewind();

        int ySize = yBuffer.remaining();
        yBuffer.get(out, 0, ySize);

        // Calculate the expected UV size for NV21
        int width = image.getWidth();
        int height = image.getHeight();
        int uvSize = width * height / 4; // Each UV plane is 1/4 the size of Y
        
        int uvPos = ySize;
        int maxUvPos = out.length; // Don't exceed buffer bounds
        
        // Interleave V and U into NV21 format (VU VU VU...)
        for (int i = 0; i < uvSize && uvPos + 1 < maxUvPos; i++) {
            if (vBuffer.hasRemaining() && uBuffer.hasRemaining()) {
                out[uvPos++] = vBuffer.get();
                out[uvPos++] = uBuffer.get();
            } else {
                break;
            }
        }
    }
}
