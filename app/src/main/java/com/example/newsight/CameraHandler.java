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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;

public class CameraHandler {

    public interface FeatureProvider {
        String getActiveFeature();
    }

    private static final String TAG = "CameraHandler";

    public static void startCamera(Context context,
                                   FrameLayout container,
                                   ExecutorService executor,
                                   WebSocketManager wsManager,
                                   FeatureProvider featureProvider) {

        PreviewView previewView = new PreviewView(context);
        container.removeAllViews();
        container.addView(previewView, new FrameLayout.LayoutParams(
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

                String feature = featureProvider.getActiveFeature();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // ⚡ Choose analyzer based on feature
                if ("asl_detection".equals(feature)) {
                    imageAnalysis.setAnalyzer(executor,
                            new ASLFrameAnalyzer(wsManager, featureProvider::getActiveFeature));
                    Log.i(TAG, "Using ASLFrameAnalyzer for feature: " + feature);
                } else {
                    imageAnalysis.setAnalyzer(executor,
                            new FrameAnalyzer(wsManager, featureProvider::getActiveFeature));
                    Log.i(TAG, "Using FrameAnalyzer for feature: " + feature);
                }

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        (androidx.lifecycle.LifecycleOwner) context,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to start camera", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

}