package com.example.newsight;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class PatternGenerator {
    private static final String TAG = "PatternGenerator";

    // Pattern types
    public enum PatternType {
        DIRECTIONAL_RIGHT,
        DIRECTIONAL_LEFT,
        DIRECTIONAL_FORWARD,
        OBSTACLE_WARNING,
        CROSSWALK_STOP,
        ARRIVAL_CELEBRATION,
        PROXIMITY_NEAR,
        PROXIMITY_VERY_NEAR
    }

    // Direction enum for directional patterns
    public enum Direction {
        LEFT,
        RIGHT,
        FORWARD
    }

    private Map<PatternType, VibrationPattern> patternLibrary;

    public PatternGenerator() {
        patternLibrary = new HashMap<>();
        initializePatternLibrary();
        Log.d(TAG, "PatternGenerator created with " + patternLibrary.size() + " patterns");
    }

    private void initializePatternLibrary() {
        // Right turn pattern - pulses on right side
        long[] rightTimings = {0, 200, 100, 200};
        int[] rightIntensities = {0, 180, 0, 180};
        patternLibrary.put(PatternType.DIRECTIONAL_RIGHT,
                new VibrationPattern(rightTimings, rightIntensities, -1));

        // Left turn pattern - pulses on left side
        long[] leftTimings = {200, 0, 200, 100};
        int[] leftIntensities = {180, 0, 180, 0};
        patternLibrary.put(PatternType.DIRECTIONAL_LEFT,
                new VibrationPattern(leftTimings, leftIntensities, -1));

        // Forward pattern - single pulse
        long[] forwardTimings = {0, 200};
        int[] forwardIntensities = {0, 150};
        patternLibrary.put(PatternType.DIRECTIONAL_FORWARD,
                new VibrationPattern(forwardTimings, forwardIntensities, -1));

        // Obstacle warning - rapid alternating pulses
        long[] warningTimings = {0, 100, 100, 100, 100, 100};
        int[] warningIntensities = {0, 255, 0, 255, 0, 255};
        patternLibrary.put(PatternType.OBSTACLE_WARNING,
                new VibrationPattern(warningTimings, warningIntensities, -1));

        // Crosswalk stop - three sharp pulses
        long[] stopTimings = {0, 150, 200, 150, 200, 150};
        int[] stopIntensities = {0, 200, 0, 200, 0, 200};
        patternLibrary.put(PatternType.CROSSWALK_STOP,
                new VibrationPattern(stopTimings, stopIntensities, -1));

        // Arrival celebration - 2 long + 3 short pulses
        long[] celebrationTimings = {0, 500, 200, 500, 200, 100, 100, 100, 100, 100};
        int[] celebrationIntensities = {0, 150, 0, 150, 0, 120, 0, 120, 0, 120};
        patternLibrary.put(PatternType.ARRIVAL_CELEBRATION,
                new VibrationPattern(celebrationTimings, celebrationIntensities, -1));

        // Proximity near - increasing intensity
        long[] proximityNearTimings = {0, 300};
        int[] proximityNearIntensities = {0, 180};
        patternLibrary.put(PatternType.PROXIMITY_NEAR,
                new VibrationPattern(proximityNearTimings, proximityNearIntensities, -1));

        // Proximity very near - high intensity
        long[] proximityVeryNearTimings = {0, 300};
        int[] proximityVeryNearIntensities = {0, 220};
        patternLibrary.put(PatternType.PROXIMITY_VERY_NEAR,
                new VibrationPattern(proximityVeryNearTimings, proximityVeryNearIntensities, -1));

        Log.d(TAG, "Pattern library initialized");
    }

    public VibrationPattern generateDirectionalPattern(Direction direction, int intensity) {
        intensity = Math.max(0, Math.min(100, intensity));

        int amplitude = (int) ((intensity / 100.0) * 255);

        long[] timings;
        int[] intensities;

        switch (direction) {
            case RIGHT:
                // Right turn - pulses on right side
                timings = new long[]{0, 200, 100, 200};
                intensities = new int[]{0, amplitude, 0, amplitude};
                Log.d(TAG, "Generated RIGHT directional pattern at " + intensity + "%");
                break;

            case LEFT:
                // Left turn - pulses on left side
                timings = new long[]{200, 0, 200, 100};
                intensities = new int[]{amplitude, 0, amplitude, 0};
                Log.d(TAG, "Generated LEFT directional pattern at " + intensity + "%");
                break;

            case FORWARD:
            default:
                // Forward - single pulse
                timings = new long[]{0, 200};
                intensities = new int[]{0, amplitude};
                Log.d(TAG, "Generated FORWARD directional pattern at " + intensity + "%");
                break;
        }

        return new VibrationPattern(timings, intensities, -1);
    }

    public VibrationPattern generateObstacleWarningPattern() {
        Log.d(TAG, "Generated OBSTACLE_WARNING pattern");
        return patternLibrary.get(PatternType.OBSTACLE_WARNING);
    }

    public VibrationPattern generateCrosswalkStopPattern() {
        Log.d(TAG, "Generated CROSSWALK_STOP pattern");
        return patternLibrary.get(PatternType.CROSSWALK_STOP);
    }

    public VibrationPattern generateArrivalCelebrationPattern() {
        Log.d(TAG, "Generated ARRIVAL_CELEBRATION pattern");
        return patternLibrary.get(PatternType.ARRIVAL_CELEBRATION);
    }

    public VibrationPattern generateProximityPattern(float distanceMeters) {
        if (distanceMeters < 10) {
            // Very close - high intensity
            Log.d(TAG, "Generated PROXIMITY_VERY_NEAR pattern (distance: " +
                    distanceMeters + "m)");
            return patternLibrary.get(PatternType.PROXIMITY_VERY_NEAR);
        } else if (distanceMeters < 50) {
            // Close - medium intensity
            Log.d(TAG, "Generated PROXIMITY_NEAR pattern (distance: " +
                    distanceMeters + "m)");
            return patternLibrary.get(PatternType.PROXIMITY_NEAR);
        } else {
            // Far - low intensity
            Log.d(TAG, "Generated low intensity proximity pattern (distance: " +
                    distanceMeters + "m)");
            long[] timings = {0, 200};
            int[] intensities = {0, 120};
            return new VibrationPattern(timings, intensities, -1);
        }
    }

    public VibrationPattern getPattern(PatternType type) {
        VibrationPattern pattern = patternLibrary.get(type);
        if (pattern == null) {
            Log.w(TAG, "Pattern type " + type + " not found in library");
        }
        return pattern;
    }

    public int getPatternCount() {
        return patternLibrary.size();
    }

    public boolean hasPattern(PatternType type) {
        return patternLibrary.containsKey(type);
    }
}