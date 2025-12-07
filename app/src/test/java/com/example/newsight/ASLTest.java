package com.example.newsight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.FrameLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ASLTest {

    @Test
    public void testASLNavigation() {
        CommunicateActivity activity = Robolectric.buildActivity(CommunicateActivity.class)
                .create().resume().get();

        // Test ASL / Sign Translation Button
        FrameLayout btnSignTranslation = activity.findViewById(R.id.btnSignTranslation);
        assertNotNull("Sign Translation button should not be null", btnSignTranslation);
        
        btnSignTranslation.performClick();
        
        Intent expectedIntent = new Intent(activity, SignTranslationActivity.class);
        Intent actualIntent = shadowOf(activity).getNextStartedActivity();
        
        assertNotNull("Should create an intent", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}
