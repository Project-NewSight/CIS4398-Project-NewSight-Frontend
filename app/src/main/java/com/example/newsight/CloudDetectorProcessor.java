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

    // ⚠️ 改成你运行 backend.py 的电脑 IP
    // 例如电脑在局域网 IP 是 192.168.0.5:
    // private static final String BASE_URL = "http://192.168.0.5:8000";
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
    private final YuvToRgbConverter yuvToRgbConverter;
    private final Context appContext;

    // 用来控制只在第一次检测后端时弹 Toast
    private volatile boolean healthChecked = false;
    private volatile boolean backendReachable = false;

    public CloudDetectorProcessor(Context context, OverlayView overlayView) {
        this.overlayView = overlayView;
        this.appContext = context.getApplicationContext();
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.networkExecutor = Executors.newSingleThreadExecutor();
        this.yuvToRgbConverter = new YuvToRgbConverter(context);

        // 启动时先异步 ping 一下 /health
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
                            : "Backend health check failed: " + response.code();
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

        Bitmap bitmap = Bitmap.createBitmap(
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        try {
            yuvToRgbConverter.yuvToRgb(image, bitmap);
        } catch (Exception e) {
            // 防止 RenderScript 等问题直接 crash
            Log.e(TAG, "yuvToRgbConverter error", e);
            imageProxy.close();
            return;
        }

        final int imageWidth = bitmap.getWidth();
        final int imageHeight = bitmap.getHeight();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        bitmap.recycle();
        final byte[] jpegBytes = baos.toByteArray();

        imageProxy.close();

        // 如果 health check 失败，就直接返回，不再持续狂打后端，避免异常
        if (healthChecked && !backendReachable) {
            // 你也可以在这里 overlayView 显示 “后端不可用”
            overlayView.post(() ->
                    overlayView.setBackendResults(
                            null,
                            imageWidth,
                            imageHeight,
                            "Backend not reachable"
                    )
            );
            return;
        }

        networkExecutor.submit(() -> {
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
                        Log.w(TAG, "Backend /detect not successful: " + response.code());
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
                // 这里所有网络异常都 catch 掉，只打 log 和 Toast，不让它崩 App
                Log.e(TAG, "Error calling backend /detect", e);
                // 可选：只在第一次失败时提示
                if (backendReachable) {
                    backendReachable = false;
                    showToastOnUi("Lost connection to backend");
                }
            }
        });
    }
}
