package com.example.newsight;

import android.util.Log;


public class VibrationPattern {
    private static final String TAG = "VibrationPattern";

    private long[] timings;

    private int[] intensities;

    private int repeat;

    public VibrationPattern(long[] timings, int[] intensities, int repeat) {
        this.timings = timings;
        this.intensities = intensities;
        this.repeat = repeat;

        Log.d(TAG, "VibrationPattern created - timings length: " +
                (timings != null ? timings.length : 0) +
                ", intensities length: " +
                (intensities != null ? intensities.length : 0) +
                ", repeat: " + repeat);
    }

    public VibrationPattern(long[] timings, int repeat) {
        this.timings = timings;
        this.repeat = repeat;

        if (timings != null) {
            this.intensities = new int[timings.length];
            for (int i = 0; i < timings.length; i++) {
                this.intensities[i] = 255;
            }
        }

        Log.d(TAG, "VibrationPattern created (default intensities) - timings length: " +
                (timings != null ? timings.length : 0) + ", repeat: " + repeat);
    }

    public long[] getTimings() {
        return timings;
    }

    public int[] getIntensities() {
        return intensities;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setTimings(long[] timings) {
        this.timings = timings;
    }

    public void setIntensities(int[] intensities) {
        this.intensities = intensities;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public long getDuration() {
        if (timings == null || timings.length == 0) {
            Log.w(TAG, "getDuration() called on empty pattern");
            return 0;
        }

        long totalDuration = 0;
        for (long timing : timings) {
            totalDuration += timing;
        }

        Log.d(TAG, "Pattern duration: " + totalDuration + "ms");
        return totalDuration;
    }

    public boolean validate() {
        // Check if timings array exists
        if (timings == null || timings.length == 0) {
            Log.e(TAG, "Validation failed: timings array is null or empty");
            return false;
        }

        // Check if intensities array exists
        if (intensities == null || intensities.length == 0) {
            Log.e(TAG, "Validation failed: intensities array is null or empty");
            return false;
        }

        // Check if arrays have matching lengths
        if (timings.length != intensities.length) {
            Log.e(TAG, "Validation failed: timings length (" + timings.length +
                    ") != intensities length (" + intensities.length + ")");
            return false;
        }

        // Check if repeat index is valid
        if (repeat < -1 || repeat >= timings.length) {
            Log.e(TAG, "Validation failed: repeat index (" + repeat +
                    ") out of bounds (valid range: -1 to " + (timings.length - 1) + ")");
            return false;
        }

        // Check if all timing values are non-negative
        for (int i = 0; i < timings.length; i++) {
            if (timings[i] < 0) {
                Log.e(TAG, "Validation failed: negative timing at index " + i +
                        " (" + timings[i] + ")");
                return false;
            }
        }

        // Check if all intensity values are in valid range (0-255)
        for (int i = 0; i < intensities.length; i++) {
            if (intensities[i] < 0 || intensities[i] > 255) {
                Log.e(TAG, "Validation failed: intensity at index " + i +
                        " (" + intensities[i] + ") out of range (0-255)");
                return false;
            }
        }

        Log.d(TAG, "Pattern validation: PASSED");
        return true;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VibrationPattern{");
        sb.append("duration=").append(getDuration()).append("ms, ");
        sb.append("repeat=").append(repeat).append(", ");

        if (timings != null) {
            sb.append("timings=[");
            for (int i = 0; i < timings.length; i++) {
                sb.append(timings[i]);
                if (i < timings.length - 1) sb.append(", ");
            }
            sb.append("], ");
        }

        if (intensities != null) {
            sb.append("intensities=[");
            for (int i = 0; i < intensities.length; i++) {
                sb.append(intensities[i]);
                if (i < intensities.length - 1) sb.append(", ");
            }
            sb.append("]");
        }

        sb.append("}");
        return sb.toString();
    }
}