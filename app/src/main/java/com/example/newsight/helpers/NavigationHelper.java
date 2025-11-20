package com.example.newsight.helpers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.newsight.models.NavigationUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * NavigationHelper - Manages WebSocket connection for real-time navigation updates
 * 
 * Connects to: ws://<backend>/navigation/ws
 * 
 * Sends: Location updates { "latitude": ..., "longitude": ..., "session_id": ... }
 * Receives: Navigation updates { "status": ..., "instruction": ..., "distance_to_next": ... }
 * 
 * Usage:
 *   NavigationHelper navHelper = new NavigationHelper("ws://...", sessionId);
 *   navHelper.setNavigationCallback(new NavigationHelper.NavigationCallback() {
 *       @Override
 *       public void onNavigationUpdate(NavigationUpdate update) { ... }
 *   });
 *   navHelper.connect();
 *   navHelper.sendLocation(lat, lng);
 */
public class NavigationHelper {

    private static final String TAG = "NavigationHelper";
    private static final long RECONNECT_DELAY_MS = 5000;

    private final String serverUrl;
    private final String sessionId;
    private final OkHttpClient client;
    private final Handler mainHandler;
    private final Gson gson;

    private WebSocket webSocket;
    private boolean connected = false;
    private boolean tryingToReconnect = false;
    private NavigationCallback callback;

    public interface NavigationCallback {
        void onNavigationUpdate(NavigationUpdate update);
        void onNavigationComplete();
        void onConnectionStatus(boolean isConnected);
        void onError(String error);
    }

    public NavigationHelper(String serverUrl, String sessionId) {
        this.serverUrl = serverUrl;
        this.sessionId = sessionId;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();

        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // Infinite for WebSocket
                .retryOnConnectionFailure(true)
                .build();
    }

    public void setNavigationCallback(NavigationCallback callback) {
        this.callback = callback;
    }

    /**
     * Connect to the navigation WebSocket
     */
    public void connect() {
        if (connected) {
            Log.w(TAG, "Already connected");
            return;
        }

        Log.i(TAG, "🔌 Connecting to " + serverUrl);
        Request request = new Request.Builder()
                .url(serverUrl)
                .build();

        webSocket = client.newWebSocket(request, new NavigationWebSocketListener());
    }

    /**
     * Send location update to backend
     */
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

            String json = gson.toJson(locationData);
            boolean sent = webSocket.send(json);

            if (sent) {
                Log.d(TAG, String.format("📤 Sent location: (%.6f, %.6f)", latitude, longitude));
            } else {
                Log.w(TAG, "Failed to send location");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending location: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnect from the navigation WebSocket
     */
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

    /**
     * Clean up resources
     */
    public void cleanup() {
        disconnect();
    }

    // ========== WebSocket Listener ==========

    private class NavigationWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
            connected = true;
            Log.i(TAG, "✅ Navigation WebSocket connected");

            if (callback != null) {
                mainHandler.post(() -> callback.onConnectionStatus(true));
            }
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            Log.d(TAG, "📥 Received: " + text);

            try {
                JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                
                // Parse navigation update
                NavigationUpdate update = gson.fromJson(json, NavigationUpdate.class);

                if (update != null && callback != null) {
                    mainHandler.post(() -> {
                        if ("arrived".equals(update.getStatus())) {
                            callback.onNavigationComplete();
                        } else {
                            callback.onNavigationUpdate(update);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing navigation update: " + e.getMessage(), e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Failed to parse update"));
                }
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, Response response) {
            connected = false;
            Log.e(TAG, "❌ Navigation WebSocket failed: " + t.getMessage());

            if (callback != null) {
                mainHandler.post(() -> {
                    callback.onConnectionStatus(false);
                    callback.onError("Connection failed: " + t.getMessage());
                });
            }

            // Auto-reconnect
            if (!tryingToReconnect) {
                scheduleReconnect();
            }
        }

        @Override
        public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
            connected = false;
            Log.i(TAG, "🔌 Navigation WebSocket closing: " + reason);
            
            if (callback != null) {
                mainHandler.post(() -> callback.onConnectionStatus(false));
            }
        }

        @Override
        public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
            connected = false;
            Log.i(TAG, "🔌 Navigation WebSocket closed: " + reason);

            if (callback != null) {
                mainHandler.post(() -> callback.onConnectionStatus(false));
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

