package com.example.newsight;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CloudDetectorProcessor implements ImageAnalysis.Analyzer {

    private static final String TAG = "CloudDetectorProcessor";

    // ⚠️ 改成你电脑的 IP
    private static final String BASE_URL = "http://192.168.1.153:8000";
    private static final String DETECT_URL = BASE_URL + "/detect";
    private static final String HEALTH_URL = BASE_URL + "/health";

    private static final long MIN_INTERVAL_MS = 200; // 5 FPS
    private long lastRequestTime = 0L;
    private int frameId = 0;

    private final OverlayView overlayView;
    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService networkExecutor;
    private final Context appContext;

    private volatile boolean healthChecked = false;
    private volatile boolean backendReachable = false;

    public CloudDetectorProcessor(Context context, OverlayView overlayView) {
        this.overlayView = overlayView;
        this.appContext = context.getApplicationContext();
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.networkExecutor = Executors.newSingleThreadExecutor();

        checkBackendHealthOnce();
    }

    private void showToastOnUi(String msg) {
        overlayView.post(() ->
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
        );
    }

    private void checkBackendHealthOnce() {
        networkExecutor.submit(() -> {
            try {
                Request request = new Request.Builder()
                        .url(HEALTH_URL)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    backendReachable = response.isSuccessful();
                    String msg = backendReachable
                            ? "Backend connected"
                            : "Backend health failed: " + response.code();
                    Log.i(TAG, msg);
                    showToastOnUi(msg);
                }
            } catch (Exception e) {
                backendReachable = false;
                String msg = "Backend unreachable: " + e.getClass().getSimpleName();
                Log.e(TAG, msg, e);
                showToastOnUi(msg);
            } finally {
                healthChecked = true;
            }
        });
    }

    @Override
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void analyze(@NonNull ImageProxy imageProxy) {
        try {
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

            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();

            // ✅ 极简版：只拿 Y 通道当成灰度图，压成 JPEG
            //    YOLO 收到的是灰度图，也照样能跑（只是没有颜色信息）
            byte[] yBytes = getYPlaneBytes(image);
            if (yBytes == null) {
                imageProxy.close();
                return;
            }

            // 把 Y 平铺成一个灰度 Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            int[] pixels = new int[width * height];
            for (int i = 0; i < width * height; i++) {
                int y = yBytes[i] & 0xFF;
                pixels[i] = 0xFF000000 | (y << 16) | (y << 8) | y; // 灰度
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            bitmap.recycle();
            byte[] jpegBytes = baos.toByteArray();

            imageProxy.close();

            if (healthChecked && !backendReachable) {
                overlayView.post(() ->
                        overlayView.setBackendResults(
                                null,
                                width,
                                height,
                                "Backend not reachable"
                        )
                );
                return;
            }

            // 发给后端
            networkExecutor.submit(() -> sendToBackend(jpegBytes, width, height));

        } catch (Throwable t) {
            // ✅ 兜底保护：不管什么异常，都不能让 app 崩
            Log.e(TAG, "analyze() crashed", t);
            try {
                imageProxy.close();
            } catch (Exception ignore) {
            }
        }
    }

    private byte[] getYPlaneBytes(Image image) {
        try {
            Image.Plane yPlane = image.getPlanes()[0];
            ByteBuffer yBuffer = yPlane.getBuffer();
            byte[] yBytes = new byte[yBuffer.remaining()];
            yBuffer.get(yBytes);
            return yBytes;
        } catch (Exception e) {
            Log.e(TAG, "getYPlaneBytes error", e);
            return null;
        }
    }

    private void sendToBackend(byte[] jpegBytes, int imageWidth, int imageHeight) {
        try {
            RequestBody fileBody = RequestBody.create(
                    jpegBytes,
                    MediaType.get("image/jpeg")
            );

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "frame_" + frameId + ".jpg", fileBody)
                    .addFormDataPart("frame_id", String.valueOf(frameId))
                    .build();

            Request request = new Request.Builder()
                    .url(DETECT_URL)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "/detect not successful: " + response.code());
                    return;
                }
                String json = response.body().string();
                CloudDetectionModels.DetectResponse detectResponse =
                        gson.fromJson(json, CloudDetectionModels.DetectResponse.class);

                if (detectResponse == null) {
                    Log.w(TAG, "DetectResponse is null");
                    return;
                }

                overlayView.post(() ->
                        overlayView.setBackendResults(
                                detectResponse.detections,
                                imageWidth,
                                imageHeight,
                                detectResponse.summary != null
                                        ? detectResponse.summary.message
                                        : ""
                        )
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calling backend /detect", e);
            if (backendReachable) {
                backendReachable = false;
                showToastOnUi("Lost connection to backend");
            }
        }
    }
}
