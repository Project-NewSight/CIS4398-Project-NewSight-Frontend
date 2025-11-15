package com.example.newsight;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public class HapticPermissionHelper {
    private static final String TAG = "HapticPermissions";

    public static boolean hasVibrationPermission(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return false;
        }

        // Check if permission is granted
        int result = context.checkCallingOrSelfPermission(
                android.Manifest.permission.VIBRATE
        );

        boolean hasPermission = (result == PackageManager.PERMISSION_GRANTED);

        Log.d(TAG, "Vibration permission status: " +
                (hasPermission ? "GRANTED" : "DENIED"));

        return hasPermission;
    }


    public static boolean hasVibrator(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return false;
        }

        android.os.Vibrator vibrator = (android.os.Vibrator)
                context.getSystemService(Context.VIBRATOR_SERVICE);

        boolean hasVibrator = vibrator != null && vibrator.hasVibrator();

        Log.d(TAG, "Device has vibrator: " + hasVibrator);

        return hasVibrator;
    }


    public static boolean canVibrate(Context context) {
        boolean permission = hasVibrationPermission(context);
        boolean hardware = hasVibrator(context);
        boolean canVibrate = permission && hardware;

        if (!canVibrate) {
            if (!permission) {
                Log.w(TAG, "Cannot vibrate: Permission not granted");
            }
            if (!hardware) {
                Log.w(TAG, "Cannot vibrate: No vibrator hardware");
            }
        } else {
            Log.d(TAG, "Device is ready to vibrate");
        }

        return canVibrate;
    }
}