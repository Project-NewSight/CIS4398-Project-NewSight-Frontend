package com.example.newsight;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketManager {

    private static final String TAG = "WebSocketManager";

    public interface WsListener {
        void onResultsReceived(String results);
        void onConnectionStatus(boolean isConnected);
    }

    private final String serverUrl;
    private final WsListener listener;

    private WebSocket webSocket;
    private boolean isConnected = false;

    public WebSocketManager(String serverUrl, WsListener listener) {
        this.serverUrl = serverUrl;
        this.listener = listener;
    }

    public void connect() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(serverUrl).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, okhttp3.Response response) {
                Log.d(TAG, "WebSocket opened");
                isConnected = true;
                if (listener != null) listener.onConnectionStatus(true);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                Log.d(TAG, "Received message: " + text);
                if (listener != null) listener.onResultsReceived(text);
            }

            @Override
            public void onMessage(WebSocket ws, ByteString bytes) {
                Log.d(TAG, "Received bytes: " + bytes.hex());
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + code + " " + reason);
                isConnected = false;
                if (listener != null) listener.onConnectionStatus(false);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, @Nullable okhttp3.Response response) {
                Log.e(TAG, "WebSocket failed", t);
                isConnected = false;
                if (listener != null) listener.onConnectionStatus(false);
            }
        });
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnected");
        }
    }

    /**
     * Send ASL frame data to backend.
     * Encodes frame to Base64 and sends as JSON: {"feature": "asl_detection", "frame": "<base64>"}
     *
     * @param frameData Raw grayscale byte array from camera (YUV Y-plane or similar)
     * @param feature Feature identifier (e.g., "asl_detection")
     */
    public void sendFrame(byte[] frameData, String feature) {
        if (!isConnected || webSocket == null) {
            Log.w(TAG, "WebSocket not connected, cannot send frame");
            return;
        }

        try {
            // Encode frame bytes to Base64
            String base64Frame = Base64.encodeToString(frameData, Base64.NO_WRAP);

            // Build JSON payload matching backend expectation
            String json = "{\"feature\":\"" + feature + "\",\"frame\":\"" + base64Frame + "\"}";
            webSocket.send(json);
            Log.d(TAG, "Sent ASL frame, size=" + frameData.length + " bytes, feature=" + feature);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send frame", e);
        }
    }

    /**
     * Alternative: Send raw grayscale frame without Base64 encoding (if backend supports raw bytes).
     * Currently not used; prefer sendFrame() which uses JSON.
     */
    public void sendRawGrayFrame(byte[] grayBytes, String feature) {
        if (!isConnected || webSocket == null) return;

        try {
            String frameB64 = Base64.encodeToString(grayBytes, Base64.NO_WRAP);
            String json = "{\"feature\":\"" + feature + "\",\"frame\":\"" + frameB64 + "\"}";
            webSocket.send(json);
            Log.d(TAG, "Sent raw gray frame, size=" + grayBytes.length + " bytes");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send raw gray frame", e);
        }
    }
}
