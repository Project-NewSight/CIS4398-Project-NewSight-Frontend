package com.example.newsight;

import android.Manifest;
import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class HapticTest {

    @Test
    public void testHapticPermissionHelper() {
        Application application = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApp = Shadows.shadowOf(application);

        // Initially no permission
        shadowApp.denyPermissions(Manifest.permission.VIBRATE);
        assertFalse(HapticPermissionHelper.hasVibrationPermission(application));

        // Grant permission
        shadowApp.grantPermissions(Manifest.permission.VIBRATE);
        assertTrue(HapticPermissionHelper.hasVibrationPermission(application));
        
        // Note: hasVibrator might return false in Robolectric by default unless shadowed service is configured.
        // We focus on permission check here which we controlled.
    }

    @Test
    public void testPatternGenerator() {
        PatternGenerator generator = new PatternGenerator();
        
        // Test Directional Patterns
        VibrationPattern rightMsg = generator.generateDirectionalPattern(PatternGenerator.Direction.RIGHT, 100);
        assertNotNull(rightMsg);
        
        // Test Warning Pattern
        VibrationPattern warning = generator.generateObstacleWarningPattern();
        assertNotNull(warning);
        
        // Test Proximity Logic
        VibrationPattern near = generator.generateProximityPattern(5.0f); // VERY NEAR
        assertNotNull(near);
        
        VibrationPattern far = generator.generateProximityPattern(40.0f); // NEAR
        assertNotNull(far);
        
        VibrationPattern veryFar = generator.generateProximityPattern(100.0f); // FAR
        assertNotNull(veryFar);
    }
}
