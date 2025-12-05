package com.example.newsight;

import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 33)
public class FamiliarFaceTest {

    @Test
    public void testAddContactValidation() {
        AddContactActivity activity = Robolectric.buildActivity(AddContactActivity.class)
                .create().resume().get();

        Button btnSave = activity.findViewById(R.id.btnSaveContact);
        
        // Empty fields
        btnSave.performClick();
        assertTrue(ShadowToast.getTextOfLatestToast().contains("Please enter name"));
    }

    @Test
    public void testAddContactSuccess() {
        AddContactActivity activity = Robolectric.buildActivity(AddContactActivity.class)
                .create().resume().get();

        EditText inputName = activity.findViewById(R.id.inputName);
        EditText inputPhone = activity.findViewById(R.id.inputPhone);
        EditText inputRelationship = activity.findViewById(R.id.inputRelationship);
        Button btnSave = activity.findViewById(R.id.btnSaveContact);

        inputName.setText("John Doe");
        inputPhone.setText("1234567890");
        inputRelationship.setText("Friend");

        btnSave.performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertTrue(activity.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
        
        Intent resultIntent = shadowActivity.getResultIntent();
        assertEquals("John Doe", resultIntent.getStringExtra("name"));
        assertEquals("1234567890", resultIntent.getStringExtra("phone"));
    }
}
