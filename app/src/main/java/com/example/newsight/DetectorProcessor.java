package com.example.newsight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.util.List;

/**
 * DetectorProcessor - Object Detection with Haptic Feedback Integration
 *
 * Processes camera frames for object detection and notifies ObstacleActivity
 * when obstacles are detected to trigger haptic feedback.
 */
public class DetectorProcessor implements ImageAnalysis.Analyzer {

    private static final String TAG = "DetectorProcessor";
    private static final String MODEL = "efficientdet-lite0.tflite";

    private final ObjectDetector detector;
    private final OverlayView overlayView;
    private final ObstacleDetectionCallback obstacleCallback;

    /**
     * Callback interface for obstacle detection events
     */
    public interface ObstacleDetectionCallback {
        /**
         * Called when obstacles are detected in the frame
         *
         * @param count Number of obstacles detected
         * @param largestSize Size of largest obstacle (0.0 to 1.0, relative to screen)
         * @param closestType Label of the closest/largest obstacle
         */
        void onObstaclesDetected(int count, float largestSize, String closestType);
    }

    public DetectorProcessor(Context context, OverlayView overlayView,
                             ObstacleDetectionCallback callback) throws Exception {
        this.overlayView = overlayView;
        this.obstacleCallback = callback;

        BaseOptions baseOptions = BaseOptions.builder().setNumThreads(4).build();
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(0.5f)
                .setMaxResults(10)
                .build();

        detector = ObjectDetector.createFromFileAndOptions(context, MODEL, options);
        Log.d(TAG, "✅ Object detector initialized with haptic callback");
    }

    @Override
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void analyze(@NonNull ImageProxy imageProxy) {
        Bitmap bitmap = imageProxy.toBitmap();
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new Rot90Op(-imageProxy.getImageInfo().getRotationDegrees() / 90))
                .build();

        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
        TensorImage rotatedImage = imageProcessor.process(tensorImage);

        List<Detection> detections = detector.detect(rotatedImage);

        // Update overlay view
        overlayView.post(() -> overlayView.setResults(detections,
                rotatedImage.getWidth(), rotatedImage.getHeight()));

        // Process detections for haptic feedback
        if (obstacleCallback != null && detections != null && !detections.isEmpty()) {
            processDetectionsForHaptic(detections, rotatedImage.getWidth(),
                    rotatedImage.getHeight());
        }

        imageProxy.close();
    }

    /**
     * Process detections and trigger haptic callback
     */
    private void processDetectionsForHaptic(List<Detection> detections, int imageWidth, int imageHeight) {
        if (detections == null || detections.isEmpty()) {
            return;
        }

        int obstacleCount = detections.size();
        float largestObstacleSize = 0.0f;
        String closestObstacleType = "";

        // Calculate obstacle sizes and find the largest (closest) one
        for (Detection detection : detections) {
            RectF boundingBox = detection.getBoundingBox();

            // Calculate relative size (0.0 to 1.0)
            float boxWidth = boundingBox.width() / imageWidth;
            float boxHeight = boundingBox.height() / imageHeight;
            float relativeSize = (boxWidth + boxHeight) / 2.0f; // Average of width and height

            // Track the largest obstacle (likely the closest)
            if (relativeSize > largestObstacleSize) {
                largestObstacleSize = relativeSize;

                // Get object label
                if (!detection.getCategories().isEmpty()) {
                    closestObstacleType = detection.getCategories().get(0).getLabel();
                }
            }
        }

        // Notify callback with detection results
        if (largestObstacleSize > 0.0f) {
            obstacleCallback.onObstaclesDetected(obstacleCount, largestObstacleSize,
                    closestObstacleType);

            Log.d(TAG, String.format("📊 Detected: %d obstacles, largest: %.2f (%s)",
                    obstacleCount, largestObstacleSize, closestObstacleType));
        }
    }
}