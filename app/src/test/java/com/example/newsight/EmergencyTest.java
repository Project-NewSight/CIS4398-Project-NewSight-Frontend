package com.example.newsight;

import android.content.Intent;
import android.widget.FrameLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 33)
public class EmergencyTest {

    @Test
    public void testEmergencyNavigation() {
        EmergencySetupActivity activity = Robolectric.buildActivity(EmergencySetupActivity.class)
                .create().resume().get();

        // Test Set Contacts Navigation
        FrameLayout btnContacts = activity.findViewById(R.id.btnSetContacts);
        btnContacts.performClick();
        Intent expectedContacts = new Intent(activity, TrustedContactsActivity.class);
        Intent actualContacts = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(expectedContacts.getComponent(), actualContacts.getComponent());

        // Test GPS Setup Navigation
        FrameLayout btnGPS = activity.findViewById(R.id.btnConfigureGPS);
        btnGPS.performClick();
        Intent expectedGPS = new Intent(activity, GPSSetupActivity.class);
        Intent actualGPS = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(expectedGPS.getComponent(), actualGPS.getComponent());
        
        // Test Full Sim Navigation
        FrameLayout btnSim = activity.findViewById(R.id.btnFullTest);
        btnSim.performClick();
        Intent expectedSim = new Intent(activity, FullEmergencySimActivity.class);
        Intent actualSim = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(expectedSim.getComponent(), actualSim.getComponent());
    }
}
