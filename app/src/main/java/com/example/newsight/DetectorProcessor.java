package com.example.newsight;

import android.content.Context;
import android.graphics.Bitmap;
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

public class DetectorProcessor implements ImageAnalysis.Analyzer {

    private final ObjectDetector detector;
    private final OverlayView overlayView;
    private static final String MODEL = "efficientdet-lite0.tflite";

    public DetectorProcessor(Context context, OverlayView overlayView) throws Exception {
        this.overlayView = overlayView;
        BaseOptions baseOptions = BaseOptions.builder().setNumThreads(4).build();
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(0.5f)
                .setMaxResults(10)
                .build();
        detector = ObjectDetector.createFromFileAndOptions(context, MODEL, options);
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

        overlayView.post(() -> overlayView.setResults(detections, rotatedImage.getWidth(), rotatedImage.getHeight()));
        
        imageProxy.close();
    }
}
