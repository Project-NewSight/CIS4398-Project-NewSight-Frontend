package com.example.newsight;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class PatternGeneratorTest {

    private PatternGenerator generator;

    @Before
    public void setUp() {
        generator = new PatternGenerator();
    }

    @Test
    public void testPatternGeneratorCreation() {
        assertNotNull(generator);
        assertTrue(generator.getPatternCount() > 0);
    }

    @Test
    public void testPatternLibrarySize() {
        // Should have 8 pre-defined patterns
        assertEquals(8, generator.getPatternCount());
    }

    @Test
    public void testGenerateDirectionalPatternRight() {
        VibrationPattern pattern = generator.generateDirectionalPattern(
                PatternGenerator.Direction.RIGHT, 80);

        assertNotNull(pattern);
        assertTrue(pattern.validate());
        assertNotNull(pattern.getTimings());
        assertNotNull(pattern.getIntensities());
        assertTrue(pattern.getTimings().length > 0);
    }

    @Test
    public void testGenerateDirectionalPatternLeft() {
        VibrationPattern pattern = generator.generateDirectionalPattern(
                PatternGenerator.Direction.LEFT, 80);

        assertNotNull(pattern);
        assertTrue(pattern.validate());
    }

    @Test
    public void testGenerateDirectionalPatternForward() {
        VibrationPattern pattern = generator.generateDirectionalPattern(
                PatternGenerator.Direction.FORWARD, 80);

        assertNotNull(pattern);
        assertTrue(pattern.validate());
    }

    @Test
    public void testDirectionalPatternIntensityClamping() {
        // Test with out-of-range intensities
        VibrationPattern pattern1 = generator.generateDirectionalPattern(
                PatternGenerator.Direction.RIGHT, -50);  // Should clamp to 0
        assertNotNull(pattern1);
        assertTrue(pattern1.validate());

        VibrationPattern pattern2 = generator.generateDirectionalPattern(
                PatternGenerator.Direction.RIGHT, 150);  // Should clamp to 100
        assertNotNull(pattern2);
        assertTrue(pattern2.validate());
    }

    @Test
    public void testGenerateObstacleWarningPattern() {
        VibrationPattern pattern = generator.generateObstacleWarningPattern();

        assertNotNull(pattern);
        assertTrue(pattern.validate());
        assertTrue(pattern.getDuration() > 0);
    }

    @Test
    public void testGenerateCrosswalkStopPattern() {
        VibrationPattern pattern = generator.generateCrosswalkStopPattern();

        assertNotNull(pattern);
        assertTrue(pattern.validate());
        assertTrue(pattern.getDuration() > 0);
    }

    @Test
    public void testGenerateArrivalCelebrationPattern() {
        VibrationPattern pattern = generator.generateArrivalCelebrationPattern();

        assertNotNull(pattern);
        assertTrue(pattern.validate());
        assertTrue(pattern.getDuration() > 0);
    }

    @Test
    public void testGenerateProximityPatternVeryNear() {
        // Distance < 10m should return PROXIMITY_VERY_NEAR
        VibrationPattern pattern = generator.generateProximityPattern(5.0f);

        assertNotNull(pattern);
        assertTrue(pattern.validate());
    }

    @Test
    public void testGenerateProximityPatternNear() {
        // Distance 10-50m should return PROXIMITY_NEAR
        VibrationPattern pattern = generator.generateProximityPattern(25.0f);

        assertNotNull(pattern);
        assertTrue(pattern.validate());
    }

    @Test
    public void testGenerateProximityPatternFar() {
        // Distance > 50m should return low intensity pattern
        VibrationPattern pattern = generator.generateProximityPattern(100.0f);

        assertNotNull(pattern);
        assertTrue(pattern.validate());
    }

    @Test
    public void testGetPatternByType() {
        VibrationPattern pattern = generator.getPattern(
                PatternGenerator.PatternType.OBSTACLE_WARNING);

        assertNotNull(pattern);
        assertTrue(pattern.validate());
    }

    @Test
    public void testHasPattern() {
        assertTrue(generator.hasPattern(PatternGenerator.PatternType.OBSTACLE_WARNING));
        assertTrue(generator.hasPattern(PatternGenerator.PatternType.CROSSWALK_STOP));
        assertTrue(generator.hasPattern(PatternGenerator.PatternType.ARRIVAL_CELEBRATION));
    }

    @Test
    public void testAllPredefinedPatternsValid() {
        // Test that all pre-defined patterns in library are valid
        for (PatternGenerator.PatternType type : PatternGenerator.PatternType.values()) {
            VibrationPattern pattern = generator.getPattern(type);
            if (pattern != null) {
                assertTrue("Pattern " + type + " should be valid", pattern.validate());
            }
        }
    }

    @Test
    public void testDirectionalPatternsHaveDifferentTimings() {
        VibrationPattern right = generator.generateDirectionalPattern(
                PatternGenerator.Direction.RIGHT, 80);
        VibrationPattern left = generator.generateDirectionalPattern(
                PatternGenerator.Direction.LEFT, 80);
        VibrationPattern forward = generator.generateDirectionalPattern(
                PatternGenerator.Direction.FORWARD, 80);

        // Right and left should have same length but different patterns
        assertEquals(right.getTimings().length, left.getTimings().length);

        // Forward should be shorter (simpler pattern)
        assertTrue(forward.getTimings().length < right.getTimings().length);
    }

    @Test
    public void testPatternDurations() {
        // Obstacle warning should be relatively quick
        VibrationPattern warning = generator.generateObstacleWarningPattern();
        assertTrue(warning.getDuration() < 1000);

        // Celebration should be longer
        VibrationPattern celebration = generator.generateArrivalCelebrationPattern();
        assertTrue(celebration.getDuration() > 1000);
    }
}