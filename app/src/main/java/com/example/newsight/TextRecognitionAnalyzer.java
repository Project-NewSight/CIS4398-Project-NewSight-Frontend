package com.example.newsight;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class TextRecognitionAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "TextRecognitionAnalyzer";
    private final TextListener listener;

    public interface TextListener {
        void onTextFound(String text);
    }

    public TextRecognitionAnalyzer(TextListener listener) {
        this.listener = listener;
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(ImageProxy imageProxy) {

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(result -> {
                    StringBuilder detected = new StringBuilder();

                    for (Text.TextBlock block : result.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            detected.append(line.getText()).append("\n");
                        }
                    }

                    if (detected.length() > 0) {
                        listener.onTextFound(detected.toString());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Text recognition failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }
}
