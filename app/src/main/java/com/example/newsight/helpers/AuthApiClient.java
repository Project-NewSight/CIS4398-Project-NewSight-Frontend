package com.example.newsight.helpers;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.FormBody;

public class AuthApiClient {

    private static final String TAG = "AuthApiClient";

    private static final String BASE_URL = "http://10.0.2.2:8000";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public AuthApiClient() {
        client = new OkHttpClient();
    }

    // Register
    public void register(String email, String name, String password, SimpleCallback callback) {
        RegisterRequest bodyObj = new RegisterRequest(email,name, password);
        String json = gson.toJson(bodyObj);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/register")
                .post(body)
                .build();

        client.newCall(request).enqueue(makeCallback(callback));
    }


    // Login
    public void login(String email, String password, LoginCallback callback) {
        RequestBody body = new FormBody.Builder()
                .add("username", email)
                .add("password", password)
                .add("grant_type", "password")
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Login failed", e);
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + body);
                    return;
                }
                TokenResponse token = gson.fromJson(body, TokenResponse.class);
                callback.onSuccess(token.accessToken);
            }
        });
    }

    //     /auth/me
    public void getMe(String token, MeCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/me")
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "/auth/me failed", e);
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + body);
                    return;
                }
                UserResponse user = gson.fromJson(body, UserResponse.class);
                callback.onSuccess(user);
            }
        });
    }


    private static class RegisterRequest {
        String email;
        String name;
        String password;
        RegisterRequest(String email, String name, String password) {
            this.email = email;
            this.name = name;
            this.password = password;
        }
    }

    private static class TokenResponse {
        @SerializedName("access_token")
        String accessToken;
        @SerializedName("token_type")
        String tokenType;
    }

    public static class UserResponse {
        // match your Userout schema
        @SerializedName("user_id")
        public int userId;
        public String email;
        @Nullable
        public String name;
    }

    // Callbacks
    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface LoginCallback {
        void onSuccess(String accessToken);
        void onError(String error);
    }

    public interface MeCallback {
        void onSuccess(UserResponse user);
        void onError(String error);
    }

    private Callback makeCallback(SimpleCallback callback) {
        return new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed", e);
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code() + ": " + body);
                    return;
                }
                callback.onSuccess();
            }
        };
    }
}
