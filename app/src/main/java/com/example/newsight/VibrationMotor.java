package com.example.newsight;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public class VibrationMotor {
    private static final String TAG = "VibrationMotor";

    private Vibrator vibrator;
    private boolean isVibrating;
    private Context context;

    public VibrationMotor(Context context) {
        this.context = context;
        this.isVibrating = false;
        Log.d(TAG, "VibrationMotor created");
    }

    public void initialize() throws VibrationException {
        try {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            if (vibrator == null) {
                throw new VibrationException("Vibrator service not available");
            }

            if (!vibrator.hasVibrator()) {
                throw new VibrationException("Device does not have a vibrator");
            }

            Log.d(TAG, "VibrationMotor initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize vibrator: " + e.getMessage());
            throw new VibrationException("Failed to initialize: " + e.getMessage());
        }
    }

    public void triggerVibration(VibrationPattern pattern, int duration, int intensity) {
        if (vibrator == null) {
            Log.w(TAG, "Vibrator not initialized, cannot trigger vibration");
            return;
        }

        if (!pattern.validate()) {
            Log.e(TAG, "Invalid pattern, cannot trigger vibration");
            return;
        }

        intensity = Math.max(0, Math.min(100, intensity));

        int amplitude = (int) ((intensity / 100.0) * 255);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                triggerVibrationModern(pattern, amplitude);
            } else {
                triggerVibrationLegacy(pattern);
            }

            isVibrating = true;
            Log.d(TAG, "Vibration triggered - duration: " + pattern.getDuration() +
                    "ms, intensity: " + intensity + "%");

        } catch (Exception e) {
            Log.e(TAG, "Error triggering vibration: " + e.getMessage());
        }
    }

    private void triggerVibrationModern(VibrationPattern pattern, int amplitude) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] timings = pattern.getTimings();
            int[] intensities = pattern.getIntensities();
            int repeat = pattern.getRepeat();

            int[] scaledIntensities = new int[intensities.length];
            for (int i = 0; i < intensities.length; i++) {
                scaledIntensities[i] = (int) ((intensities[i] / 255.0) * amplitude);
                if (intensities[i] > 0 && scaledIntensities[i] == 0) {
                    scaledIntensities[i] = 1;
                }
            }

            VibrationEffect effect = VibrationEffect.createWaveform(
                    timings,
                    scaledIntensities,
                    repeat
            );

            vibrator.vibrate(effect);
            Log.d(TAG, "Modern vibration triggered with VibrationEffect");
        }
    }

    @SuppressWarnings("deprecation")
    private void triggerVibrationLegacy(VibrationPattern pattern) {
        long[] timings = pattern.getTimings();
        int repeat = pattern.getRepeat();

        vibrator.vibrate(timings, repeat);
        Log.d(TAG, "Legacy vibration triggered");
    }

    public void vibrateSimple(int duration, int intensity) {
        if (vibrator == null) {
            Log.w(TAG, "Vibrator not initialized");
            return;
        }

        intensity = Math.max(0, Math.min(100, intensity));
        int amplitude = (int) ((intensity / 100.0) * 255);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(duration, amplitude);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(duration);
            }

            isVibrating = true;
            Log.d(TAG, "Simple vibration triggered - " + duration + "ms at " + intensity + "%");

        } catch (Exception e) {
            Log.e(TAG, "Error in simple vibration: " + e.getMessage());
        }
    }

    public void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
            isVibrating = false;
            Log.d(TAG, "Vibration stopped");
        }
    }
    public boolean isVibrating() {
        return isVibrating;
    }


    public boolean isInitialized() {
        return vibrator != null;
    }

    public void close() {
        stopVibration();
        vibrator = null;
        Log.d(TAG, "VibrationMotor closed");
    }

    public static class VibrationException extends Exception {
        public VibrationException(String message) {
            super(message);
        }
    }
}


