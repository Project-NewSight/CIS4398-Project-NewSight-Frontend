package com.example.newsight;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for VibrationPattern class
 */
public class VibrationPatternTest {

    @Test
    public void testValidPatternCreation() {
        long[] timings = {0, 200, 100, 200};
        int[] intensities = {0, 150, 0, 150};
        int repeat = -1;

        VibrationPattern pattern = new VibrationPattern(timings, intensities, repeat);

        assertNotNull(pattern);
        assertArrayEquals(timings, pattern.getTimings());
        assertArrayEquals(intensities, pattern.getIntensities());
        assertEquals(repeat, pattern.getRepeat());
    }

    @Test
    public void testSimplifiedConstructor() {
        long[] timings = {0, 500};
        int repeat = -1;

        VibrationPattern pattern = new VibrationPattern(timings, repeat);

        assertNotNull(pattern);
        assertNotNull(pattern.getIntensities());
        assertEquals(timings.length, pattern.getIntensities().length);

        // Check default intensities are 255
        for (int intensity : pattern.getIntensities()) {
            assertEquals(255, intensity);
        }
    }

    @Test
    public void testGetDuration() {
        long[] timings = {0, 200, 100, 200};
        int[] intensities = {0, 150, 0, 150};

        VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

        long expectedDuration = 0 + 200 + 100 + 200;
        assertEquals(expectedDuration, pattern.getDuration());
    }

    @Test
    public void testValidateValidPattern() {
        long[] timings = {0, 200, 100, 200};
        int[] intensities = {0, 150, 0, 150};

        VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

        assertTrue(pattern.validate());
    }

    @Test
    public void testValidateMismatchedArrays() {
        long[] timings = {0, 200, 100};
        int[] intensities = {0, 150}; // Length mismatch

        VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

        assertFalse(pattern.validate());
    }

    @Test
    public void testValidateInvalidRepeat() {
        long[] timings = {0, 200, 100, 200};
        int[] intensities = {0, 150, 0, 150};

        VibrationPattern pattern = new VibrationPattern(timings, intensities, 10); // Out of bounds

        assertFalse(pattern.validate());
    }

    @Test
    public void testValidateNegativeTiming() {
        long[] timings = {0, -200, 100, 200}; // Negative value
        int[] intensities = {0, 150, 0, 150};

        VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

        assertFalse(pattern.validate());
    }

    @Test
    public void testValidateInvalidIntensity() {
        long[] timings = {0, 200, 100, 200};
        int[] intensities = {0, 300, 0, 150}; // Intensity > 255

        VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

        assertFalse(pattern.validate());
    }

    @Test
    public void testEmptyPatternDuration() {
        long[] timings = {};
        int[] intensities = {};

        VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

        assertEquals(0, pattern.getDuration());
    }

    @Test
    public void testToString() {
        long[] timings = {0, 200};
        int[] intensities = {0, 150};

        VibrationPattern pattern = new VibrationPattern(timings, intensities, -1);

        String result = pattern.toString();

        assertNotNull(result);
        assertTrue(result.contains("VibrationPattern"));
        assertTrue(result.contains("duration=200ms"));
    }
}