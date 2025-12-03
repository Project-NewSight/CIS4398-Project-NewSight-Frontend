package com.example.newsight;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/**
 * Analyzes camera frames for ASL detection.
 * Extracts the Y-plane (grayscale) from YUV_420_888 camera frames and sends to backend via WebSocket.
 */
public class ASLFrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ASLFrameAnalyzer";
    private final WebSocketManager wsManager;
    private final FeatureProvider featureProvider;

    // Throttle: send at most one frame every 100ms to reduce network load
    private static final long MIN_INTERVAL_MS = 100;
    private long lastFrameSentTime = 0;

    public ASLFrameAnalyzer(WebSocketManager manager, FeatureProvider provider) {
        this.wsManager = manager;
        this.featureProvider = provider;
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        // Throttle frame rate
        long now = System.currentTimeMillis();
        if (now - lastFrameSentTime < MIN_INTERVAL_MS) {
            imageProxy.close();
            return;
        }
        lastFrameSentTime = now;

        Image image = imageProxy.getImage();
        if (image == null) {
            imageProxy.close();
            return;
        }

        try {
            // Check if ASL detection is active and WebSocket is connected
            String feature = featureProvider.getActiveFeature();
            if (!"asl_detection".equals(feature) || wsManager == null || !wsManager.isConnected()) {
                imageProxy.close();
                return;
            }

            // Verify camera format (expect YUV_420_888)
            if (image.getFormat() != ImageFormat.YUV_420_888) {
                Log.w(TAG, "Unexpected image format: " + image.getFormat());
                imageProxy.close();
                return;
            }

            // Extract Y-plane (grayscale) from YUV image
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            byte[] grayBytes = new byte[yBuffer.remaining()];
            yBuffer.get(grayBytes);

            Log.d(TAG, "Extracted grayscale frame: " + image.getWidth() + "x" + image.getHeight() +
                    ", bytes=" + grayBytes.length);

            // Send full-resolution grayscale to backend
            // Backend will handle resizing and model inference
            wsManager.sendFrame(grayBytes, feature);

        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze ASL frame", e);
        } finally {
            imageProxy.close();
        }
    }
}
