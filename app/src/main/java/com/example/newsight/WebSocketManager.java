package com.example.newsight;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketManager {

    private static final String TAG = "WebSocketManager";
    private static final boolean ENABLE_RECONNECT = true;
    private static final long RECONNECT_DELAY_MS = 5000;

    private final OkHttpClient client;
    private final String serverUrl;
    private final WsListener listener;

    private WebSocket webSocket;
    private volatile boolean connected = false;
    private boolean tryingToReconnect = false;

    public interface WsListener {
        void onResultsReceived(String results);
        void onConnectionStatus(boolean isConnected);
    }

    public WebSocketManager(String url, WsListener listener) {
        this.serverUrl = url;
        this.listener = listener;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void connect() {
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

    public void sendFrame(byte[] frameBytes, @NonNull String feature) {
        if (connected && webSocket != null) {
            try {
                boolean sent = webSocket.send(okio.ByteString.of(frameBytes));
                if (!sent) Log.w(TAG, "Failed to send frame.");
                else Log.d(TAG, "Sent binary frame (" + frameBytes.length + " bytes)");
            } catch (Exception e) {
                Log.e(TAG, "Error sending binary frame: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "Skipping frame — not connected.");
        }
    }

    private class SocketListener extends WebSocketListener {
        @Override public void onOpen(@NonNull WebSocket ws, @NonNull Response r) {
            connected = true;
            Log.i(TAG, "WebSocket connected");
            if (listener != null) listener.onConnectionStatus(true);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            if (listener != null) listener.onResultsReceived(text);
        }

        @Override public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, Response r) {
            connected = false;
            Log.e(TAG, "WebSocket failed", t);
            if (listener != null) listener.onConnectionStatus(false);
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            connected = false;
            if (listener != null) listener.onConnectionStatus(false);
            Log.i(TAG, "WebSocket closing: " + reason);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            connected = false;
            if (listener != null) listener.onConnectionStatus(false);
            Log.i(TAG, "WebSocket closed: " + reason);
        }
    }
}
