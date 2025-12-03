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

    // TODO: set this to your machine’s IP
    private static final String BASE_URL = "http://100.19.30.133/object-detection";
    private static final String DETECT_URL = BASE_URL + "/detect";
    private static final String HEALTH_URL = BASE_URL + "/health";

    // Target size we send to the backend (keeps bandwidth and YOLO cost low)
    private static final int TARGET_WIDTH = 640;
    private static final int TARGET_HEIGHT = 480;

    // Throttle how often we send frames to the backend
    private static final long MIN_INTERVAL_MS = 200;       // ~5 FPS
    // When the backend is unreachable, wait this long before trying again
    private static final long RETRY_INTERVAL_MS = 5000;    // 5 seconds

    private long lastRequestTime = 0L;
    private int frameId = 0;

    private final OverlayView overlayView;
    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService networkExecutor;
    private final Context appContext;

    // Connection / reconnection state
    private volatile boolean backendReachable = true;
    private volatile long lastConnectionFailMs = 0L;

    // One-in-flight control for /detect
    private volatile boolean requestInFlight = false;

    // HUD metrics
    private volatile long lastLatencyMs = 0L;
    private volatile float approxFps = 0f;
    private volatile long lastResponseTimeMs = 0L;

    // Reusable buffers to reduce GC
    private int[] rgbBuffer = null;
    private ByteArrayOutputStream jpegStream = null;
    private int lastFullWidth = -1;
    private int lastFullHeight = -1;

    public CloudDetectorProcessor(Context context, OverlayView overlayView) {
        this.overlayView = overlayView;
        this.appContext = context.getApplicationContext();
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.networkExecutor = Executors.newSingleThreadExecutor();

        // Optional: initial health check and toast
        checkBackendHealthOnce();
    }

    private void showToastOnUi(String msg) {
        overlayView.post(() ->
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * One-time health check on startup, just to give a helpful toast.
     * Reconnect logic is handled by /detect + RETRY_INTERVAL_MS.
     */
    private void checkBackendHealthOnce() {
        networkExecutor.submit(() -> {
            try {
                Request request = new Request.Builder()
                        .url(HEALTH_URL)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    boolean ok = response.isSuccessful();
                    backendReachable = ok;
                    if (ok) {
                        Log.i(TAG, "Initial backend health OK");
                        showToastOnUi("Backend connected");
                    } else {
                        Log.w(TAG, "Initial backend health failed: " + response.code());
                        backendReachable = false;
                        lastConnectionFailMs = System.currentTimeMillis();
                        showToastOnUi("Backend health failed: " + response.code());
                    }
                }
            } catch (Exception e) {
                backendReachable = false;
                lastConnectionFailMs = System.currentTimeMillis();
                String msg = "Backend unreachable: " + e.getClass().getSimpleName();
                Log.e(TAG, msg, e);
                showToastOnUi(msg);
            }
        });
    }

    @Override
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void analyze(@NonNull ImageProxy imageProxy) {
        try {
            long now = System.currentTimeMillis();

            // Global FPS throttle
            if (now - lastRequestTime < MIN_INTERVAL_MS) {
                imageProxy.close();
                return;
            }
            lastRequestTime = now;

            // Reconnect logic: if backend is currently marked unreachable,
            // only allow a retry every RETRY_INTERVAL_MS.
            if (!backendReachable) {
                if (now - lastConnectionFailMs < RETRY_INTERVAL_MS) {
                    imageProxy.close();
                    return;
                } else {
                    // Allow a new attempt; if it fails again, we will mark it unreachable again.
                    backendReachable = true;
                }
            }

            frameId++;

            Image image = imageProxy.getImage();
            if (image == null) {
                imageProxy.close();
                return;
            }

            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();

            // Allocate reusable buffers if needed
            if (rgbBuffer == null || width != lastFullWidth || height != lastFullHeight) {
                rgbBuffer = new int[width * height];
                lastFullWidth = width;
                lastFullHeight = height;
            }
            if (jpegStream == null) {
                jpegStream = new ByteArrayOutputStream(1024 * 1024);
            } else {
                jpegStream.reset();
            }

            // Convert YUV_420_888 -> ARGB int[] (color)
            yuv420ToArgb(image, rgbBuffer, width, height);

            // Create a full-size bitmap, then downscale to target size
            Bitmap fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            fullBitmap.setPixels(rgbBuffer, 0, width, 0, 0, width, height);

            // Keep aspect ratio while fitting into TARGET_WIDTH x TARGET_HEIGHT
            float scale = Math.min(
                    TARGET_WIDTH * 1f / width,
                    TARGET_HEIGHT * 1f / height
            );
            int outW = Math.round(width * scale);
            int outH = Math.round(height * scale);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, outW, outH, true);
            fullBitmap.recycle();

            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, jpegStream);
            scaledBitmap.recycle();

            byte[] jpegBytes = jpegStream.toByteArray();

            imageProxy.close();

            // Only send if no /detect request is currently in flight
            if (requestInFlight) {
                return;
            }
            requestInFlight = true;

            final int backendImageWidth = outW;
            final int backendImageHeight = outH;
            final byte[] payload = jpegBytes;

            networkExecutor.submit(() ->
                    sendToBackend(payload, backendImageWidth, backendImageHeight)
            );

        } catch (Throwable t) {
            Log.e(TAG, "analyze() crashed", t);
            try {
                imageProxy.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Convert an android.media.Image in format YUV_420_888 to ARGB8888 pixels.
     */
    private void yuv420ToArgb(Image image, int[] out, int width, int height) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uRowStride = planes[1].getRowStride();
        int vRowStride = planes[2].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vPixelStride = planes[2].getPixelStride();

        for (int y = 0; y < height; y++) {
            int yRow = yRowStride * y;
            int uvRow = uRowStride * (y / 2);
            for (int x = 0; x < width; x++) {
                int yIndex = yRow + x;
                int uvIndex = uvRow + (x / 2) * uPixelStride;

                int Y = yBuffer.get(yIndex) & 0xff;
                int U = (uBuffer.get(uvIndex) & 0xff) - 128;
                int V = (vBuffer.get(uvIndex) & 0xff) - 128;

                float fY = (float) Y;
                int r = (int) (fY + 1.370705f * V);
                int g = (int) (fY - 0.337633f * U - 0.698001f * V);
                int b = (int) (fY + 1.732446f * U);

                r = clamp(r, 0, 255);
                g = clamp(g, 0, 255);
                b = clamp(b, 0, 255);

                out[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    private int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    /**
     * Sends a JPEG frame to the backend /detect endpoint.
     * Handles latency/FPS HUD, error logging, and auto-reconnect state.
     */
    private void sendToBackend(byte[] jpegBytes, int imageWidth, int imageHeight) {
        long start = System.currentTimeMillis();
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
                long end = System.currentTimeMillis();
                long latency = end - start;
                lastLatencyMs = latency;

                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "/detect not successful: " + response.code());
                    // Consider this a failure for reconnect logic
                    backendReachable = false;
                    lastConnectionFailMs = System.currentTimeMillis();
                    overlayView.post(() ->
                            overlayView.setBackendResults(
                                    null,
                                    imageWidth,
                                    imageHeight,
                                    "Backend not reachable",
                                    lastLatencyMs,
                                    approxFps
                            )
                    );
                    return;
                }

                String json = response.body().string();
                CloudDetectionModels.DetectResponse detectResponse =
                        gson.fromJson(json, CloudDetectionModels.DetectResponse.class);

                if (detectResponse == null) {
                    Log.w(TAG, "DetectResponse is null");
                    backendReachable = false;
                    lastConnectionFailMs = System.currentTimeMillis();
                    overlayView.post(() ->
                            overlayView.setBackendResults(
                                    null,
                                    imageWidth,
                                    imageHeight,
                                    "Backend not reachable",
                                    lastLatencyMs,
                                    approxFps
                            )
                    );
                    return;
                }

                // Success: backend is reachable again
                backendReachable = true;

                // Update approximate FPS based on time between responses
                long now = System.currentTimeMillis();
                if (lastResponseTimeMs > 0) {
                    long dt = now - lastResponseTimeMs;
                    if (dt > 0) {
                        approxFps = 1000f / (float) dt;
                    }
                }
                lastResponseTimeMs = now;

                overlayView.post(() ->
                        overlayView.setBackendResults(
                                detectResponse.detections,
                                imageWidth,
                                imageHeight,
                                detectResponse.summary != null
                                        ? detectResponse.summary.message
                                        : "",
                                lastLatencyMs,
                                approxFps
                        )
                );
            }
        } catch (Exception e) {
            long end = System.currentTimeMillis();
            long latency = end - start;
            lastLatencyMs = latency;

            Log.e(TAG, "Error calling backend /detect", e);

            // Mark backend unreachable and remember when it failed
            backendReachable = false;
            lastConnectionFailMs = System.currentTimeMillis();

            overlayView.post(() ->
                    overlayView.setBackendResults(
                            null,
                            imageWidth,
                            imageHeight,
                            "Backend not reachable",
                            lastLatencyMs,
                            approxFps
                    )
            );

            showToastOnUi("Lost connection to backend");
        } finally {
            requestInFlight = false;
        }
    }
}
