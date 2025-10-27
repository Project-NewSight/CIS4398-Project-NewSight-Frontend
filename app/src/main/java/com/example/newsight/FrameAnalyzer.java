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
            byte[] nv21 = yuv420ToNv21(image);
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(),
                    image.getHeight(), null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 40, out);
            byte[] jpegBytes = out.toByteArray();

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

    private byte[] yuv420ToNv21(Image image) {
        Image.Plane[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();

        int ySize = planes[0].getBuffer().remaining();
        int uvSize = planes[1].getBuffer().remaining() + planes[2].getBuffer().remaining();
        byte[] nv21 = new byte[ySize + uvSize];

        planes[0].getBuffer().get(nv21, 0, ySize);

        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int uvPos = ySize;

        uBuffer.rewind();
        vBuffer.rewind();
        while (vBuffer.hasRemaining() && uBuffer.hasRemaining()) {
            nv21[uvPos++] = vBuffer.get();
            nv21[uvPos++] = uBuffer.get();
        }
        return nv21;
    }
}
