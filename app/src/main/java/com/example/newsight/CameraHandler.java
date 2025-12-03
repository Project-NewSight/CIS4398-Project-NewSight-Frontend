package com.example.newsight;

import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;

public class CameraHandler {

    private static final String TAG = "CameraHandler";

    /**
     * Updated FeatureProvider interface
     * (supports both old `.getFeature()` and new `.getActiveFeature()`)
     */
    public interface FeatureProvider {
        String getActiveFeature();
    }

    public static void stopCamera() { /* Reserved for future use */ }

    public static void startCamera(Context context,
                                   FrameLayout container,
                                   ExecutorService executor,
                                   WebSocketManager wsManager,
                                   FeatureProvider featureProvider) {

        PreviewView previewView = new PreviewView(context);

        container.removeAllViews();
        container.addView(previewView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Get the current feature
                String feature = featureProvider.getActiveFeature();
                Log.i(TAG, "Selected feature: " + feature);

                // Create ImageAnalysis pipeline
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // --- Analyzer selection (merged logic) ---
                if ("asl_detection".equals(feature)) {
                    imageAnalysis.setAnalyzer(
                            executor,
                            new ASLFrameAnalyzer(wsManager, featureProvider::getActiveFeature)
                    );
                    Log.i(TAG, "Using ASLFrameAnalyzer");
                } else {
                    imageAnalysis.setAnalyzer(
                            executor,
                            new FrameAnalyzer(wsManager, featureProvider::getActiveFeature)
                    );
                    Log.i(TAG, "Using FrameAnalyzer");
                }

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        (LifecycleOwner) context,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

                Log.i(TAG, "Camera bound successfully for feature: " + feature);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }
}
