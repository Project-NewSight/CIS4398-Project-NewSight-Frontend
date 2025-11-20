package com.example.newsight.helpers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * LocationWebSocketHelper - Sends continuous location updates to /location/ws
 * This allows the backend to track user location in the background
 */
public class LocationWebSocketHelper {

    private static final String TAG = "LocationWebSocketHelper";
    private static final long RECONNECT_DELAY_MS = 5000;

    private final String serverUrl;
    private final String sessionId;
    private final OkHttpClient client;
    private final Handler mainHandler;
    private final Gson gson;

    private WebSocket webSocket;
    private boolean connected = false;
    private boolean tryingToReconnect = false;
    private ConnectionCallback callback;

    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }

    public LocationWebSocketHelper(String serverUrl, String sessionId) {
        this.serverUrl = serverUrl;
        this.sessionId = sessionId;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        this.callback = callback;
    }

    public void connect() {
        if (connected) {
            Log.w(TAG, "Already connected");
            return;
        }

        Log.i(TAG, "🔌 Connecting to " + serverUrl);
        Request request = new Request.Builder()
                .url(serverUrl)
                .build();

        webSocket = client.newWebSocket(request, new LocationWebSocketListener());
    }

    public void sendLocation(double latitude, double longitude) {
        if (!connected || webSocket == null) {
            Log.w(TAG, "Cannot send location - not connected");
            return;
        }

        try {
            JsonObject locationData = new JsonObject();
            locationData.addProperty("latitude", latitude);
            locationData.addProperty("longitude", longitude);
            locationData.addProperty("session_id", sessionId);
            locationData.addProperty("timestamp", System.currentTimeMillis() / 1000); // Unix timestamp in seconds

            String json = gson.toJson(locationData);
            boolean sent = webSocket.send(json);

            if (sent) {
                Log.d(TAG, String.format("📤 Sent location to WS [%s]: (%.6f, %.6f)", 
                    sessionId.substring(0, 8), latitude, longitude));
            } else {
                Log.w(TAG, "Failed to send location");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending location: " + e.getMessage(), e);
        }
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public void cleanup() {
        disconnect();
    }

    private class LocationWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
            connected = true;
            Log.i(TAG, "✅ Location WebSocket connected");

            if (callback != null) {
                mainHandler.post(() -> callback.onConnected());
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            Log.d(TAG, "📥 Received: " + text);
        }

        @Override
        public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, Response response) {
            connected = false;
            Log.e(TAG, "❌ Location WebSocket failed: " + t.getMessage());

            if (callback != null) {
                mainHandler.post(() -> {
                    callback.onDisconnected();
                    callback.onError("Connection failed: " + t.getMessage());
                });
            }

            if (!tryingToReconnect) {
                scheduleReconnect();
            }
        }

        @Override
        public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
            connected = false;
            Log.i(TAG, "🔌 Location WebSocket closing: " + reason);
            
            if (callback != null) {
                mainHandler.post(() -> callback.onDisconnected());
            }
        }

        @Override
        public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
            connected = false;
            Log.i(TAG, "🔌 Location WebSocket closed: " + reason);

            if (callback != null) {
                mainHandler.post(() -> callback.onDisconnected());
            }
        }

        private void scheduleReconnect() {
            tryingToReconnect = true;
            Log.d(TAG, "⏳ Scheduling reconnect in " + (RECONNECT_DELAY_MS / 1000) + "s");

            mainHandler.postDelayed(() -> {
                tryingToReconnect = false;
                if (!connected) {
                    Log.d(TAG, "🔄 Attempting to reconnect...");
                    connect();
                }
            }, RECONNECT_DELAY_MS);
        }
    }
}

