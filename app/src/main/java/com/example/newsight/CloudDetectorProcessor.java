package com.example.newsight;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * 把 CameraX 的帧发到 Python 后端做 YOLO 推理，
 * 然后把返回的检测结果丢给 OverlayView。
 */
public class CloudDetectorProcessor implements ImageAnalysis.Analyzer {

    private static final String TAG = "CloudDetectorProcessor";

    // TODO: 改成你后端的地址（本机测试可以先用电脑局域网 IP）
    private static final String BACKEND_URL = "http://100.19.30.133:8000/detect";

    private static final long MIN_INTERVAL_MS = 200; // 约 5 FPS
    private long lastRequestTime = 0L;
    private int frameId = 0;

    private final OverlayView overlayView;
    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService networkExecutor;
    private final YuvToRgbConverter yuvToRgbConverter;

    public CloudDetectorProcessor(Context context, OverlayView overlayView) {
        this.overlayView = overlayView;
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.networkExecutor = Executors.newSingleThreadExecutor();
        this.yuvToRgbConverter = new YuvToRgbConverter(context);
    }

    @Override
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void analyze(@NonNull ImageProxy imageProxy) {
        long now = System.currentTimeMillis();
        if (now - lastRequestTime < MIN_INTERVAL_MS) {
            imageProxy.close();
            return;
        }
        lastRequestTime = now;
        frameId++;

        Image image = imageProxy.getImage();
        if (image == null) {
            imageProxy.close();
            return;
        }

        // 1. ImageProxy -> Bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        yuvToRgbConverter.yuvToRgb(image, bitmap);

        // 如果有旋转角度，可以在这里对 bitmap 做旋转处理（略）

        int imageWidth = bitmap.getWidth();
        int imageHeight = bitmap.getHeight();

        // 2. Bitmap -> JPEG byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        bitmap.recycle();
        byte[] jpegBytes = baos.toByteArray();

        // 3. 一定要尽快 close，避免卡住 Camera
        imageProxy.close();

        // 4. 异步发给后端（后台线程）
        networkExecutor.submit(() -> {
            try {
                RequestBody fileBody = RequestBody.create(
                        jpegBytes,
                        MediaType.get("image/jpeg")
                );

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                                "file",
                                "frame_" + frameId + ".jpg",
                                fileBody
                        )
                        .addFormDataPart("frame_id", String.valueOf(frameId))
                        .build();

                Request request = new Request.Builder()
                        .url(BACKEND_URL)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.w(TAG, "Backend response not successful: " + response.code());
                        return;
                    }
                    String json = response.body().string();

                    CloudDetectionModels.DetectResponse detectResponse =
                            gson.fromJson(json, CloudDetectionModels.DetectResponse.class);

                    if (detectResponse == null) {
                        return;
                    }

                    // 把后端的 detections + 图像尺寸传给 OverlayView
                    overlayView.post(() ->
                            overlayView.setBackendResults(
                                    detectResponse.detections,
                                    imageWidth,
                                    imageHeight,
                                    detectResponse.summary != null ? detectResponse.summary.message : null
                            )
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling backend", e);
            }
        });
    }
}
