package com.example.newsight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class UserInteractionTest {

    @Test
    public void testHomeActivityInteractions() {
        ActivityController<HomeActivity> controller = Robolectric.buildActivity(HomeActivity.class)
                .create().start().resume().visible();
        HomeActivity activity = controller.get();

        // Test Navigation Button
        FrameLayout btnNavigate = activity.findViewById(R.id.btnNavigate);
        assertNotNull(btnNavigate);
        btnNavigate.performClick();
        Intent expectedIntent = new Intent(activity, NavigateActivity.class);
        Intent actualIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());

        // Test Settings Navigation
        LinearLayout navSettings = activity.findViewById(R.id.navSettings);
        assertNotNull(navSettings);
        navSettings.performClick();
        expectedIntent = new Intent(activity, SettingsActivity.class);
        actualIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        
        // Test Emergency Button
        FrameLayout btnEmergency = activity.findViewById(R.id.btnEmergency);
        assertNotNull(btnEmergency);
        btnEmergency.performClick();
        expectedIntent = new Intent(activity, EmergencyActivity.class);
        actualIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());

        controller.pause().stop().destroy();
    }

    @Test
    public void testSettingsActivityInteractions() {
        ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class)
                .create().start().resume().visible();
        SettingsActivity activity = controller.get();

        // Test User Profile Button
        FrameLayout btnUserProfile = activity.findViewById(R.id.btnUserProfile);
        assertNotNull(btnUserProfile);
        btnUserProfile.performClick();
        Intent expectedIntent = new Intent(activity, UserProfileActivity.class);
        Intent actualIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());

        // Test Back Button
        FrameLayout btnBack = activity.findViewById(R.id.btnBack);
        assertNotNull(btnBack);
        btnBack.performClick();
        // Should finish activity
        assert(activity.isFinishing());

        controller.pause().stop().destroy();
    }
}
