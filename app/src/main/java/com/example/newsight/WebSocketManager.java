package com.example.newsight;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

// IMPORTANT: use Android's Base64 for API < 26
import android.util.Base64;

public class WebSocketManager {

    private static final String TAG = "WebSocketManager";
    private static final boolean ENABLE_RECONNECT = true;
    private static final long RECONNECT_DELAY_MS = 5000;
    private static final long MIN_FRAME_INTERVAL_MS = 250;

    private final OkHttpClient client;
    private final String serverUrl;
    private final WsListener listener;

    private WebSocket webSocket;
    private volatile boolean connected = false;
    private boolean tryingToReconnect = false;

    private volatile String currentFeature = null;
    private long lastSend = 0;
    private final java.util.concurrent.atomic.AtomicInteger framesSent = new java.util.concurrent.atomic.AtomicInteger(0);
    public int getFramesSent() { return framesSent.get(); }
    public interface WsListener {
        void onResultsReceived(String results);
        void onConnectionStatus(boolean isConnected);
    }

    public WebSocketManager(String url, WsListener listener) {
        this.serverUrl = url;
        this.listener = listener;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(0, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false)
                .build();


    }

    // ---------- Public API ----------

    public void connect() {
        Log.i(TAG, "Connecting to " + serverUrl);
        Request request = new Request.Builder().url(serverUrl).build();
        webSocket = client.newWebSocket(request, new SocketListener());
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setFeature(@NonNull String feature) {
        this.currentFeature = feature;
        Log.d(TAG, "setFeature -> " + feature);
        if (connected && webSocket != null) {
            sendText(buildHello(feature));
        }
    }

    /** Preferred: control JSON then binary JPEG */
    public void sendFrame(@NonNull byte[] frameBytes, @NonNull String feature) {
        if (!connected || webSocket == null) {
            Log.d(TAG, "Skipping frame — not connected.");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSend < MIN_FRAME_INTERVAL_MS) return;
        lastSend = now;

        try {
            if (currentFeature == null || !currentFeature.equals(feature)) {
                currentFeature = feature;
            }

            String ctl = buildFrameControl(feature, frameBytes.length);
            boolean okCtl = webSocket.send(ctl);
            if (!okCtl) {
                Log.w(TAG, "Failed to send frame control JSON");
                return;
            }

            boolean okBin = webSocket.send(ByteString.of(frameBytes));
            if (!okBin) {
                Log.w(TAG, "Failed to send binary frame.");
            } else {
                Log.d(TAG, "Sent frame: feature=" + feature + " (" + frameBytes.length + " bytes)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending frame: " + e.getMessage(), e);
        }
    }


    public void sendFrameAsJsonBase64(@NonNull byte[] frameBytes, @NonNull String feature) {
        if (!connected || webSocket == null) {
            Log.d(TAG, "Skipping frame (JSON mode) — not connected.");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSend < MIN_FRAME_INTERVAL_MS) return;
        lastSend = now;

        try {
            if (currentFeature == null || !currentFeature.equals(feature)) {
                currentFeature = feature;
            }
            // Android Base64
            String b64 = Base64.encodeToString(frameBytes, Base64.NO_WRAP);

            JSONObject obj = new JSONObject();
            obj.put("type", "frame");
            obj.put("feature", feature);
            obj.put("image_b64", b64);
            obj.put("len", frameBytes.length);

            boolean sent = webSocket.send(obj.toString());
            if (!sent) {
                Log.w(TAG, "Failed to send JSON base64 frame.");
            } else {
                Log.d(TAG, "Sent JSON base64 frame: feature=" + feature + " (" + frameBytes.length + " bytes)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending JSON base64 frame: " + e.getMessage(), e);
        }
    }

    // ---------- Internals ----------

    private void sendText(String payload) {
        if (!connected || webSocket == null) return;
        try {
            boolean ok = webSocket.send(payload);
            if (!ok) Log.w(TAG, "sendText failed");
            else Log.d(TAG, "sendText: " + payload);
        } catch (Exception e) {
            Log.e(TAG, "sendText error: " + e.getMessage(), e);
        }
    }

    private static String buildHello(String feature) {
        return "{\"type\":\"hello\",\"feature\":\"" + feature + "\"}";
    }

    private static String buildFrameControl(String feature, int len) {
        return "{\"type\":\"frame\",\"feature\":\"" + feature + "\",\"len\":" + len + "}";
    }

    // ---------- Listener ----------

    private class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket ws, @NonNull Response r) {
            connected = true;
            Log.i(TAG, "WebSocket connected");
            ws.send("ping");

            if (currentFeature != null) {
                ws.send(buildHello(currentFeature));
            }

            if (listener != null) listener.onConnectionStatus(true);
        }

        /*@Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            Log.d(TAG, "recv(text): " + text);
            if (listener != null) listener.onResultsReceived(text);
        }
        */


        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            try {
                org.json.JSONObject obj = new org.json.JSONObject(text);

                boolean match = obj.optBoolean("match", false);
                String name = obj.optString("contactName",
                        obj.optString("name", "Unknown"));


                Log.d(TAG, "rec(text) match: " + match + ", contactName: " + name);


                if (listener != null) listener.onResultsReceived(obj.toString());

            } catch (Exception e) {
                Log.d(TAG, "rec(text) (non-JSON)");
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, Response r) {
            connected = false;
            Log.e(TAG, "WebSocket failed: " + t +
                    (r != null ? (" | code=" + r.code() + " msg=" + r.message()) : " | no HTTP response"));

            // Guarded response-body logging to satisfy lint
            if (r != null) {
                try {
                    if (r.body() != null) {
                        Log.e(TAG, "resp body: " + r.body().string());
                    }
                } catch (Exception ignore) { }
            }

            if (listener != null) listener.onConnectionStatus(false);
            if (ENABLE_RECONNECT && !tryingToReconnect) scheduleReconnect();
        }

        private void scheduleReconnect() {
            tryingToReconnect = true;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                tryingToReconnect = false;
                connect();
            }, RECONNECT_DELAY_MS);
        }

        @Override
        public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
            connected = false;
            if (listener != null) listener.onConnectionStatus(false);
            Log.i(TAG, "WebSocket closing: " + reason);
        }

        @Override
        public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
            connected = false;
            if (listener != null) listener.onConnectionStatus(false);
            Log.i(TAG, "WebSocket closed: " + reason);
        }
    }
}
