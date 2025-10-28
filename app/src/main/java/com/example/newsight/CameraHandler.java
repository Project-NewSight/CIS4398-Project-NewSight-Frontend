package com.example.newsight;

import android.util.Log;
import android.widget.FrameLayout;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;

public class CameraHandler {

    private static final String TAG = "CameraHandler";

    public static void stopCamera() {

    }

    public interface FeatureProvider {
        String getFeature();
    }

    public static void startCamera(MainActivity activity,
                                   FrameLayout container,
                                   ExecutorService executor,
                                   WebSocketManager wsManager,
                                   FeatureProvider provider) {

        PreviewView previewView = new PreviewView(activity);
        container.removeAllViews();
        container.addView(previewView,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(activity);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(executor, new FrameAnalyzer(wsManager, (FrameAnalyzer.FeatureProvider) provider));

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageAnalysis);

                Log.i(TAG, "Camera bound successfully for feature: " + provider.getFeature());

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(activity));
    }
}
