package com.example.newsight;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;

public class ReadTextFrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ReadTextFrameAnalyzer";
    private final WebSocketManager wsManager;
    private final FeatureProvider featureProvider;
    private final ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
    private long lastLogTime = 0;
    
    // Frame throttling for text detection
    private long lastFrameSentTime = 0;
    private static final long MIN_FRAME_INTERVAL_MS = 400; // Send max 2.5 frames/second (was unlimited!)

    public interface FeatureProvider {
        String getActiveFeature();
    }

    public ReadTextFrameAnalyzer(WebSocketManager manager, FeatureProvider provider) {
        this.wsManager = manager;
        this.featureProvider = provider;
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        try {
            String activeFeature = featureProvider.getActiveFeature();

            // Only process frames if a feature is active and the socket is connected
            if (wsManager != null && wsManager.isConnected() && activeFeature != null) {

                // THROTTLE: Check if enough time has passed since last frame
                long currentTime = System.currentTimeMillis();
                long timeSinceLastFrame = currentTime - lastFrameSentTime;
                
                if (timeSinceLastFrame < MIN_FRAME_INTERVAL_MS) {
                    // Skip this frame - too soon!
                    return; // Important: still need to close imageProxy in finally block
                }
                
                // Update last sent time
                lastFrameSentTime = currentTime;
                
                Log.d(TAG, "Sending frame (interval: " + timeSinceLastFrame + "ms)");

                // 1. Convert ImageProxy to a Bitmap (handles YUV conversion)
                Bitmap bitmap = imageProxy.toBitmap();

                // 2. Get the rotation degrees
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

                // 3. Rotate the Bitmap if necessary
                Bitmap rotatedBitmap = bitmap;
                if (rotationDegrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }

                // 4. Compress the final, corrected Bitmap to JPEG
                jpegStream.reset();
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, jpegStream);
                byte[] jpegBytes = jpegStream.toByteArray();

                // 5. Send the frame
                wsManager.sendFrame(jpegBytes, activeFeature);

            } else {
                long now = System.currentTimeMillis();
                if (now - lastLogTime > 2000) {
                    // (Your existing detailed logging remains here)
                    boolean featureOk = activeFeature != null;
                    if (wsManager == null) {
                        if (!featureOk) {
                            Log.d(TAG, "Skipping frame: ALL systems down (wsManager is null AND no feature selected)");
                        } else {
                            Log.d(TAG, "Skipping frame: wsManager is null");
                        }
                    } else if (!wsManager.isConnected()) {
                        if (!featureOk) {
                            Log.d(TAG, "Skipping frame: ALL systems down (wsManager not connected AND no feature selected)");
                        } else {
                            Log.d(TAG, "Skipping frame: wsManager is not connected");
                        }
                    } else if (!featureOk) {
                        Log.d(TAG, "Skipping frame: No feature selected");
                    }
                    lastLogTime = now;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Frame processing failed", e);
        } finally {
            imageProxy.close();
        }
    }
}
