package com.example.newsight.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class FileUtils {

    @Nullable
    public static MultipartBody.Part uriToPart(Context context, Uri uri, String fieldName) {
        if (uri == null) return null;

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            File file = new File(context.getCacheDir(), System.currentTimeMillis() + ".jpg");
            FileOutputStream output = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            output.close();
            inputStream.close();

            RequestBody requestFile =
                    RequestBody.create(file, MediaType.parse("image/jpeg"));

            return MultipartBody.Part.createFormData(fieldName, file.getName(), requestFile);

        } catch (Exception e) {
            return null;
        }
    }

    public static RequestBody text(String value) {
        return RequestBody.create(value, MultipartBody.FORM);
    }
}
