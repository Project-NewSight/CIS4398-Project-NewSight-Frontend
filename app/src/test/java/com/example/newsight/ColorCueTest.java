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
public class ColorCueTest {

    @Test
    public void testRefiningNavigation() {
        ColorCueActivity activity = Robolectric.buildActivity(ColorCueActivity.class)
                .create().resume().get();

        // Test Match Outfit Button
        FrameLayout btnMatchOutfit = activity.findViewById(R.id.btnMatchOutfit);
        assertNotNull("Match Outfit button should not be null", btnMatchOutfit);
        
        btnMatchOutfit.performClick();
        
        Intent expectedIntent = new Intent(activity, MatchOutfitActivity.class);
        Intent actualIntent = shadowOf(activity).getNextStartedActivity();
        
        assertNotNull("Should create an intent", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}
